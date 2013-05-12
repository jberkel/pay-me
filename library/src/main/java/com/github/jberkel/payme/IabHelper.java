/* Copyright (c) 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.jberkel.payme;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import com.android.vending.billing.IInAppBillingService;
import com.github.jberkel.payme.listener.OnConsumeFinishedListener;
import com.github.jberkel.payme.listener.OnConsumeMultiFinishedListener;
import com.github.jberkel.payme.listener.OnIabPurchaseFinishedListener;
import com.github.jberkel.payme.listener.OnIabSetupFinishedListener;
import com.github.jberkel.payme.listener.QueryInventoryFinishedListener;
import com.github.jberkel.payme.model.Inventory;
import com.github.jberkel.payme.model.ItemType;
import com.github.jberkel.payme.model.Purchase;
import com.github.jberkel.payme.model.SkuDetails;
import com.github.jberkel.payme.security.DefaultSignatureValidator;
import com.github.jberkel.payme.security.SignatureValidator;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import static com.github.jberkel.payme.IabConsts.*;
import static com.github.jberkel.payme.model.ItemType.INAPP;
import static com.github.jberkel.payme.model.ItemType.SUBS;
import static com.github.jberkel.payme.Response.*;

/**
 * Provides convenience methods for in-app billing. You can create one instance of this
 * class for your application and use it to process in-app billing operations.
 * It provides synchronous (blocking) and asynchronous (non-blocking) methods for
 * many common in-app billing operations, as well as automatic signature
 * verification.
 *
 * After instantiating, you must perform setup in order to start using the object.
 * To perform setup, call the {@link #startSetup} method and provide a listener;
 * that listener will be notified when setup is complete, after which (and not before)
 * you may call other methods.
 *
 * After setup is complete, you will typically want to request an inventory of owned
 * items and subscriptions. See {@link #queryInventory}, {@link #queryInventoryAsync}
 * and related methods.
 *
 * When you are done with this object, don't forget to call {@link #dispose}
 * to ensure proper cleanup. This object holds a binding to the in-app billing
 * service, which will leak unless you dispose of it correctly. If you created
 * the object on an Activity's onCreate method, then the recommended
 * place to dispose of it is the Activity's onDestroy method.
 *
 * A note about threading: When using this object from a background thread, you may
 * call the blocking versions of methods; when using from a UI thread, call
 * only the asynchronous versions and handle the results via callbacks.
 * Also, notice that you can only call one asynchronous operation at a time;
 * attempting to start a second asynchronous operation while the first one
 * has not yet completed will result in an exception being thrown.
 *
 * @author Bruno Oliveira (Google)
 *
 */
public class IabHelper {
    protected static final Intent BIND_BILLING_SERVICE = new Intent("com.android.vending.billing.InAppBillingService.BIND");

    private boolean mDebugLog;
    private String mDebugTag = "IabHelper";

    private boolean mSetupDone;
    private boolean mDisposed;
    private boolean mSubscriptionsSupported;
    private boolean mInAppSupported;

    // Is an asynchronous operation in progress?
    // (only one at a time can be in progress)
    private boolean mAsyncInProgress;

    // (for logging/debugging)
    // if mAsyncInProgress == true, what asynchronous operation is in progress?
    private String mAsyncOperation = "";

    private Context mContext;

    // Connection to the service
    private IInAppBillingService mService;
    private ServiceConnection mServiceConn;

    // The request code used to launch purchase flow
    private int mRequestCode;

    // The item type of the current purchase flow
    private ItemType mPurchasingItemType;

    // The listener registered on launchPurchaseFlow, which we have to call back when the purchase finishes
    private @Nullable
    OnIabPurchaseFinishedListener mPurchaseListener;

    private SignatureValidator mSignatureValidator;

    /**
     * Like {@link #IabHelper(android.content.Context, String)}, but uses the string resource
     * <code>com_github_jberkel_payme_public_key</code> to resolve a public key.
     * @param ctx Your application or Activity context. Needed to bind to the in-app billing service.
     */
    public IabHelper(Context ctx) {
        this(ctx, ctx.getString(R.string.com_github_jberkel_payme_public_key));
    }

    /**
     * Creates an instance. After creation, it will not yet be ready to use. You must perform
     * setup by calling {@link #startSetup} and wait for setup to complete. This constructor does not
     * block and is safe to call from a UI thread.
     *
     * @param ctx Your application or Activity context. Needed to bind to the in-app billing service.
     * @param base64PublicKey Your application's public key, encoded in base64.
     *     This is used for verification of purchase signatures. You can find your app's base64-encoded
     *     public key in your application's page on Google Play Developer Console. Note that this
     *     is NOT your "developer public key".
     */
    public IabHelper(Context ctx, String base64PublicKey) {
        mContext = ctx.getApplicationContext();
        mSignatureValidator = new DefaultSignatureValidator(base64PublicKey);
        logDebug("IAB helper created.");
    }

    /**
     * Enables or disable debug logging through LogCat.
     */
    @SuppressWarnings("UnusedDeclaration")
    public void enableDebugLogging(boolean enable, String tag) {
        checkNotDisposed();
        enableDebugLogging(enable);
        mDebugTag = tag;
    }

    public void enableDebugLogging(boolean enable) {
        checkNotDisposed();
        mDebugLog = enable;
    }

    /**
     * Starts the setup process. This will start up the setup process asynchronously.
     * You will be notified through the listener when the setup process is complete.
     * This method is safe to call from a UI thread.
     *
     * @param listener The listener to notify when the setup process is complete.
     */
    public void startSetup(final @Nullable OnIabSetupFinishedListener listener) {
        // If already set up, can't do it again.
        checkNotDisposed();
        if (mSetupDone) throw new IllegalStateException("IAB helper is already set up.");

        // Connection to IAB service
        logDebug("Starting in-app billing setup.");
        mServiceConn = new ServiceConnection() {
            @Override
            public void onServiceDisconnected(ComponentName name) {
                logDebug("Billing service disconnected.");
                mService = null;
            }

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                if (mDisposed) return;
                logDebug("Billing service connected.");
                mService = getInAppBillingService(service);

                String packageName = mContext.getPackageName();
                try {
                    logDebug("Checking for in-app billing 3 support.");

                    // check for in-app billing v3 support
                    int response = mService.isBillingSupported(API_VERSION, packageName, INAPP.toString());
                    if (response != OK.code) {
                        if (listener != null) {
                            listener.onIabSetupFinished(new IabResult(response, null));
                        }
                    } else {
                        logDebug("In-app billing version 3 supported for " + packageName);
                        mInAppSupported = true;

                        // check for v3 subscriptions support
                        response = mService.isBillingSupported(API_VERSION, packageName, SUBS.toString());
                        if (response == OK.code) {
                            logDebug("Subscriptions AVAILABLE.");
                            mSubscriptionsSupported = true;
                        } else {
                            logDebug("Subscriptions NOT AVAILABLE. Response: "+response + " " + Response.getDescription(response));
                        }
                    }
                } catch (RemoteException e) {
                    if (listener != null) {
                        listener.onIabSetupFinished(new IabResult(IABHELPER_REMOTE_EXCEPTION));
                    }
                    Log.e(mDebugTag, "RemoteException while setting up in-app billing.", e);
                    return;
                } finally {
                    mSetupDone = true;
                }

                if (listener != null) {
                    listener.onIabSetupFinished(new IabResult(OK));
                }
            }
        };

        if (!mContext.getPackageManager().queryIntentServices(BIND_BILLING_SERVICE, 0).isEmpty()) {
            // service available to handle that Intent
            mContext.bindService(BIND_BILLING_SERVICE, mServiceConn, Context.BIND_AUTO_CREATE);
        } else {
            // no service available to handle that Intent
            mSetupDone = true;
            if (listener != null) {
                listener.onIabSetupFinished(new IabResult(BILLING_UNAVAILABLE));
            }
        }
    }

    // for testing
    protected IInAppBillingService getInAppBillingService(IBinder service) {
        return IInAppBillingService.Stub.asInterface(service);
    }

    /**
     * Dispose of object, releasing resources. It's very important to call this
     * method when you are done with this object. It will release any resources
     * used by it such as service connections. Naturally, once the object is
     * disposed of, it can't be used again.
     */
    public void dispose() {
        logDebug("Disposing.");
        mSetupDone = false;
        if (mServiceConn != null) {
            logDebug("Unbinding from service.");
            if (mContext != null) mContext.unbindService(mServiceConn);
        }
        mDisposed = true;
        mContext = null;
        mServiceConn = null;
        mService = null;
        mPurchaseListener = null;
    }

    private void checkNotDisposed() {
        if (mDisposed) throw new IllegalStateException("IabHelper was disposed of, so it cannot be used.");
    }

    /** Returns whether subscriptions are supported. */
    public boolean subscriptionsSupported() {
        checkNotDisposed();
        return mSubscriptionsSupported;
    }

    public void launchPurchaseFlow(Activity act, String sku, int requestCode, OnIabPurchaseFinishedListener listener) {
        launchPurchaseFlow(act, sku, requestCode, listener, "");
    }

    public void launchPurchaseFlow(Activity act, String sku, int requestCode,
            OnIabPurchaseFinishedListener listener, String extraData) {
        launchPurchaseFlow(act, sku, INAPP, requestCode, listener, extraData);
    }

    public void launchSubscriptionPurchaseFlow(Activity act, String sku, int requestCode,
            OnIabPurchaseFinishedListener listener) {
        launchSubscriptionPurchaseFlow(act, sku, requestCode, listener, "");
    }

    public void launchSubscriptionPurchaseFlow(Activity act, String sku, int requestCode,
            OnIabPurchaseFinishedListener listener, String extraData) {
        launchPurchaseFlow(act, sku, SUBS, requestCode, listener, extraData);
    }

    /**
     * Initiate the UI flow for an in-app purchase. Call this method to initiate an in-app purchase,
     * which will involve bringing up the Google Play screen. The calling activity will be paused while
     * the user interacts with Google Play, and the result will be delivered via the activity's
     * {@link android.app.Activity#onActivityResult} method, at which point you must call
     * this object's {@link #handleActivityResult} method to continue the purchase flow. This method
     * MUST be called from the UI thread of the Activity.
     *
     * @param act The calling activity.
     * @param sku The sku of the item to purchase.
     * @param itemType indicates if it's a product or a subscription (ITEM_TYPE_INAPP or ITEM_TYPE_SUBS)
     * @param requestCode A request code (to differentiate from other responses --
     *     as in {@link android.app.Activity#startActivityForResult}).
     * @param listener The listener to notify when the purchase process finishes
     * @param extraData Extra data (developer payload), which will be returned with the purchase data
     *     when the purchase completes. This extra data will be permanently bound to that purchase
     *     and will always be returned when the purchase is queried.
     */
    public void launchPurchaseFlow(Activity act, String sku, ItemType itemType, int requestCode,
                        @Nullable OnIabPurchaseFinishedListener listener, String extraData) {
        checkNotDisposed();
        checkSetupDone("launchPurchaseFlow");
        flagStartAsync("launchPurchaseFlow");
        IabResult result;

        if (itemType == SUBS  && !mSubscriptionsSupported ||
            itemType == INAPP && !mInAppSupported) {

            flagEndAsync();
            if (listener != null) {
                if (itemType == INAPP) {
                    result = new IabResult(BILLING_UNAVAILABLE);
                } else {
                    result = new IabResult(IABHELPER_SUBSCRIPTIONS_NOT_AVAILABLE);
                }
                listener.onIabPurchaseFinished(result, null);
            }
            return;
        }

        try {
            logDebug("Constructing buy intent for " + sku + ", item type: " + itemType);
            Bundle buyIntentBundle = mService.getBuyIntent(API_VERSION, mContext.getPackageName(), sku, itemType.toString(), extraData);
            int response = getResponseCodeFromBundle(buyIntentBundle);
            if (response != OK.code) {
                logError("Unable to buy item, Error response: " + getDescription(response));
                flagEndAsync();
                result = new IabResult(response, "Unable to buy item");
                if (listener != null) listener.onIabPurchaseFinished(result, null);
                return;
            }

            PendingIntent pendingIntent = buyIntentBundle.getParcelable(RESPONSE_BUY_INTENT);
            if (pendingIntent == null) {
                throw new SendIntentException("No pending intent");
            }

            logDebug("Launching buy intent for " + sku + ". Request code: " + requestCode);
            mRequestCode = requestCode;
            mPurchaseListener = listener;
            mPurchasingItemType = itemType;
            act.startIntentSenderForResult(pendingIntent.getIntentSender(),
                                           requestCode, new Intent(),
                                           0, 0, 0);
        } catch (SendIntentException e) {
            logError("SendIntentException while launching purchase flow for sku " + sku, e);
            flagEndAsync();

            result = new IabResult(IABHELPER_SEND_INTENT_FAILED);
            if (listener != null) listener.onIabPurchaseFinished(result, null);
        } catch (RemoteException e) {
            logError("RemoteException while launching purchase flow for sku " + sku, e);
            flagEndAsync();

            result = new IabResult(IABHELPER_REMOTE_EXCEPTION);
            if (listener != null) listener.onIabPurchaseFinished(result, null);
        }
    }

    /**
     * Handles an activity result that's part of the purchase flow in in-app billing. If you
     * are calling {@link #launchPurchaseFlow}, then you must call this method from your
     * Activity's {@link android.app.Activity@onActivityResult} method. This method
     * MUST be called from the UI thread of the Activity.
     *
     * @param requestCode The requestCode as you received it.
     * @param resultCode The resultCode as you received it.
     * @param data The data (Intent) as you received it.
     * @return Returns true if the result was related to a purchase flow and was handled;
     *     false if the result was not related to a purchase, in which case you should
     *     handle it normally.
     */
    public boolean handleActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        IabResult result;
        if (requestCode != mRequestCode) return false;

        checkNotDisposed();
        checkSetupDone("handleActivityResult");

        // end of async purchase operation that started on launchPurchaseFlow
        flagEndAsync();

        if (data == null) {
            logError("Null data in IAB activity result.");
            result = new IabResult(IABHELPER_BAD_RESPONSE, "Null data in IAB result");
            if (mPurchaseListener != null) mPurchaseListener.onIabPurchaseFinished(result, null);
            return true;
        }

        int responseCode = getResponseCodeFromBundle(data.getExtras());
        String purchaseData = data.getStringExtra(RESPONSE_INAPP_PURCHASE_DATA);
        String dataSignature = data.getStringExtra(RESPONSE_INAPP_SIGNATURE);

        if (resultCode == Activity.RESULT_OK && responseCode == OK.code) {
            logDebug("Successful resultcode from purchase activity.");
            logDebug("Purchase data: " + purchaseData);
            logDebug("Data signature: " + dataSignature);
            logDebug("Extras: " + data.getExtras());
            logDebug("Expected item type: " + mPurchasingItemType);

            if (purchaseData == null || dataSignature == null) {
                logError("BUG: either purchaseData or dataSignature is null.");
                logDebug("Extras: " + data.getExtras().toString());
                result = new IabResult(IABHELPER_UNKNOWN_ERROR, "IAB returned null purchaseData or dataSignature");
                if (mPurchaseListener != null) mPurchaseListener.onIabPurchaseFinished(result, null);
                return true;
            }

            Purchase purchase;
            try {
                purchase = new Purchase(mPurchasingItemType, purchaseData, dataSignature);
                String sku = purchase.getSku();

                // Verify signature
                if (!mSignatureValidator.validate(purchaseData, dataSignature)) {
                    logError("Purchase signature verification FAILED for sku " + sku);
                    result = new IabResult(IABHELPER_VERIFICATION_FAILED, "Signature verification failed for sku " + sku);
                    if (mPurchaseListener != null) mPurchaseListener.onIabPurchaseFinished(result, purchase);
                    return true;
                }
                logDebug("Purchase signature successfully verified.");
            }
            catch (JSONException e) {
                logError("Failed to parse purchase data.", e);
                e.printStackTrace();
                result = new IabResult(IABHELPER_BAD_RESPONSE, "Failed to parse purchase data.");
                if (mPurchaseListener != null) mPurchaseListener.onIabPurchaseFinished(result, null);
                return true;
            }

            if (mPurchaseListener != null) {
                mPurchaseListener.onIabPurchaseFinished(new IabResult(OK), purchase);
            }
        }
        else if (resultCode == Activity.RESULT_OK) {
            // result code was OK, but in-app billing response was not OK.
            logDebug("Result code was OK but in-app billing response was not OK: " + getDescription(responseCode));
            if (mPurchaseListener != null) {
                result = new IabResult(responseCode, "Problem purchashing item.");
                mPurchaseListener.onIabPurchaseFinished(result, null);
            }
        }
        else if (resultCode == Activity.RESULT_CANCELED) {
            logDebug("Purchase canceled - Response: " + getDescription(responseCode));
            result = new IabResult(IABHELPER_USER_CANCELLED);
            if (mPurchaseListener != null) mPurchaseListener.onIabPurchaseFinished(result, null);
        }
        else {
            logError("Purchase failed. Result code: " + Integer.toString(resultCode)
                    + ". Response: " + getDescription(responseCode));
            result = new IabResult(IABHELPER_UNKNOWN_PURCHASE_RESPONSE);
            if (mPurchaseListener != null) mPurchaseListener.onIabPurchaseFinished(result, null);
        }
        return true;
    }

    public Inventory queryInventory(boolean querySkuDetails, List<String> moreSkus) throws IabException {
        return queryInventory(querySkuDetails, moreSkus, null);
    }

    /**
     * Queries the inventory. This will query all owned items from the server, as well as
     * information on additional skus, if specified. This method may block or take long to execute.
     * Do not call from a UI thread. For that, use the non-blocking version {@link #queryInventoryAsync(boolean, List, com.github.jberkel.payme.listener.QueryInventoryFinishedListener)}.
     *
     * @param querySkuDetails if true, SKU details (price, description, etc) will be queried as well
     *     as purchase information.
     * @param moreItemSkus additional PRODUCT skus to query information on, regardless of ownership.
     *     Ignored if null or if querySkuDetails is false.
     * @param moreSubsSkus additional SUBSCRIPTIONS skus to query information on, regardless of ownership.
     *     Ignored if null or if querySkuDetails is false.
     * @throws IabException if a problem occurs while refreshing the inventory.
     */
    public Inventory queryInventory(boolean querySkuDetails,
                                    @Nullable List<String> moreItemSkus,
                                    @Nullable List<String> moreSubsSkus) throws IabException {
        checkNotDisposed();
        checkSetupDone("queryInventory");
        try {
            Inventory inv = new Inventory();
            int r = queryPurchases(inv, INAPP);
            if (r != OK.code) {
                throw new IabException(r, "Error refreshing inventory (querying owned items).");
            }

            if (querySkuDetails) {
                r = querySkuDetails(INAPP, inv, moreItemSkus);
                if (r != OK.code) {
                    throw new IabException(r, "Error refreshing inventory (querying prices of items).");
                }
            }

            // if subscriptions are supported, then also query for subscriptions
            if (mSubscriptionsSupported) {
                r = queryPurchases(inv, SUBS);
                if (r != OK.code) {
                    throw new IabException(r, "Error refreshing inventory (querying owned subscriptions).");
                }

                if (querySkuDetails) {
                    r = querySkuDetails(SUBS, inv, moreItemSkus);
                    if (r != OK.code) {
                        throw new IabException(r, "Error refreshing inventory (querying prices of subscriptions).");
                    }
                }
            }

            return inv;
        }
        catch (RemoteException e) {
            throw new IabException(IABHELPER_REMOTE_EXCEPTION, "Remote exception while refreshing inventory.", e);
        }
        catch (JSONException e) {
            throw new IabException(IABHELPER_BAD_RESPONSE, "Error parsing JSON response while refreshing inventory.", e);
        }
    }

    /**
     * Asynchronous wrapper for inventory query. This will perform an inventory
     * query as described in {@link #queryInventory}, but will do so asynchronously
     * and call back the specified listener upon completion. This method is safe to
     * call from a UI thread.
     *
     * @param querySkuDetails as in {@link #queryInventory}
     * @param moreSkus as in {@link #queryInventory}
     * @param listener The listener to notify when the refresh operation completes.
     */
    public void queryInventoryAsync(final boolean querySkuDetails,
                               final List<String> moreSkus,
                               final @Nullable QueryInventoryFinishedListener listener) {
        checkNotDisposed();
        checkSetupDone("queryInventory");
        new QueryInventoryTask(this, listener).execute(new QueryInventoryTask.QueryArgs(querySkuDetails, moreSkus));
    }

    public void queryInventoryAsync(QueryInventoryFinishedListener listener) {
        queryInventoryAsync(true, null, listener);
    }

    public void queryInventoryAsync(boolean querySkuDetails, QueryInventoryFinishedListener listener) {
        queryInventoryAsync(querySkuDetails, null, listener);
    }


    /**
     * Consumes a given in-app product. Consuming can only be done on an item
     * that's owned, and as a result of consumption, the user will no longer own it.
     * This method may block or take long to return. Do not call from the UI thread.
     * For that, see {@link #consumeAsync}.
     *
     * @param itemInfo The PurchaseInfo that represents the item to consume.
     * @throws IabException if there is a problem during consumption.
     */
    public void consume(Purchase itemInfo) throws IabException {
        checkNotDisposed();
        checkSetupDone("consume");

        if (itemInfo.getItemType() != INAPP) {
            throw new IabException(IABHELPER_INVALID_CONSUMPTION,
                    "Items of type '" + itemInfo.getItemType() + "' can't be consumed.");
        }
        try {
            String token = itemInfo.getToken();
            String sku = itemInfo.getSku();
            if (token == null || token.equals("")) {
               logError("Can't consume "+ sku + ". No token.");
               throw new IabException(IABHELPER_MISSING_TOKEN, "PurchaseInfo is missing token for sku: "
                   + sku + " " + itemInfo);
            }

            logDebug("Consuming sku: " + sku + ", token: " + token);
            int response = mService.consumePurchase(API_VERSION, mContext.getPackageName(), token);
            if (response == OK.code) {
               logDebug("Successfully consumed sku: " + sku);
            } else {
                logDebug("Error consuming consuming sku " + sku + ". " + getDescription(response));
               throw new IabException(response, "Error consuming sku " + sku);
            }
        }
        catch (RemoteException e) {
            throw new IabException(IABHELPER_REMOTE_EXCEPTION, "Remote exception while consuming. PurchaseInfo: " + itemInfo, e);
        }
    }

    /**
     * Asynchronous wrapper to item consumption. Works like {@link #consume}, but
     * performs the consumption in the background and notifies completion through
     * the provided listener. This method is safe to call from a UI thread.
     *
     * @param purchase The purchase to be consumed.
     * @param listener The listener to notify when the consumption operation finishes.
     */
    public void consumeAsync(Purchase purchase, OnConsumeFinishedListener listener) {
        checkNotDisposed();
        checkSetupDone("consume");
        List<Purchase> purchases = new ArrayList<Purchase>();
        purchases.add(purchase);
        consumeAsyncInternal(purchases, listener, null);
    }

    /**
     * Same as {@link #consumeAsync(Purchase, OnConsumeFinishedListener)}, but for multiple items at once.
     * @param purchases The list of PurchaseInfo objects representing the purchases to consume.
     * @param listener The listener to notify when the consumption operation finishes.
     */
    public void consumeAsync(List<Purchase> purchases, OnConsumeMultiFinishedListener listener) {
        checkNotDisposed();
        checkSetupDone("consume");
        consumeAsyncInternal(purchases, null, listener);
    }

    // Checks that setup was done; if not, throws an exception.
    private void checkSetupDone(String operation) {
        if (!mSetupDone) {
            logError("Illegal state for operation (" + operation + "): IAB helper is not set up.");
            throw new IllegalStateException("IAB helper is not set up. Can't perform operation: " + operation);
        }
    }

    protected int getResponseCodeFromBundle(Bundle bundle) {
        Object o;
        if (bundle == null || ((o = bundle.get(RESPONSE_CODE)) == null)) {
            logDebug("Bundle with null response code, assuming OK (known issue)");
            return OK.code;
        }
        else if (o instanceof Integer) return (Integer) o;
        // Workaround to bug where sometimes response codes come as Long instead of Integer
        else if (o instanceof Long) return (int)((Long)o).longValue();
        else {
            logError("Unexpected type for bundle response code.");
            logError(o.getClass().getName());
            throw new RuntimeException("Unexpected type for bundle response code: " + o.getClass().getName());
        }
    }

    protected void flagStartAsync(String operation) {
        if (mAsyncInProgress) throw new IllegalStateException("Can't start async operation (" +
                operation + ") because another async operation(" + mAsyncOperation + ") is in progress.");
        mAsyncOperation = operation;
        mAsyncInProgress = true;
        logDebug("Starting async operation: " + operation);
    }

    protected void flagEndAsync() {
        logDebug("Ending async operation: " + mAsyncOperation);
        mAsyncOperation = "";
        mAsyncInProgress = false;
    }

    protected boolean isDisposed() {
        return mDisposed;
    }


    private int queryPurchases(Inventory inv, ItemType itemType) throws JSONException, RemoteException {
        // Query purchases
        logDebug("Querying owned items, item type: " + itemType);
        logDebug("Package name: " + mContext.getPackageName());
        boolean verificationFailed = false;
        String continueToken = null;

        do {
            logDebug("Calling getPurchases with continuation token: " + continueToken);
            Bundle ownedItems = mService.getPurchases(API_VERSION, mContext.getPackageName(),
                    itemType.toString(), continueToken);

            int response = getResponseCodeFromBundle(ownedItems);
            logDebug("Owned items response: " + String.valueOf(response));
            if (response != OK.code) {
                logDebug("getPurchases() failed: " + getDescription(response));
                return response;
            }
            if (ownedItems == null
                    || !ownedItems.containsKey(RESPONSE_INAPP_ITEM_LIST)
                    || !ownedItems.containsKey(RESPONSE_INAPP_PURCHASE_DATA_LIST)
                    || !ownedItems.containsKey(RESPONSE_INAPP_SIGNATURE_LIST)) {
                logError("Bundle returned from getPurchases() doesn't contain required fields.");
                return IABHELPER_BAD_RESPONSE.code;
            }

            ArrayList<String> ownedSkus = ownedItems.getStringArrayList(
                        RESPONSE_INAPP_ITEM_LIST);
            ArrayList<String> purchaseDataList = ownedItems.getStringArrayList(
                        RESPONSE_INAPP_PURCHASE_DATA_LIST);
            ArrayList<String> signatureList = ownedItems.getStringArrayList(
                        RESPONSE_INAPP_SIGNATURE_LIST);

            for (int i = 0; i < purchaseDataList.size(); ++i) {
                String purchaseData = purchaseDataList.get(i);
                String signature = signatureList.get(i);
                String sku = ownedSkus.get(i);
                if (mSignatureValidator.validate(purchaseData, signature)) {
                    logDebug("Sku is owned: " + sku);
                    Purchase purchase = new Purchase(itemType, purchaseData, signature);

                    if (TextUtils.isEmpty(purchase.getToken())) {
                        logWarn("BUG: empty/null token!");
                        logDebug("Purchase data: " + purchaseData);
                    }

                    // Record ownership and token
                    inv.addPurchase(purchase);
                }
                else {
                    logWarn("Purchase signature verification **FAILED**. Not adding item.");
                    logDebug("   Purchase data: " + purchaseData);
                    logDebug("   Signature: " + signature);
                    verificationFailed = true;
                }
            }

            continueToken = ownedItems.getString(INAPP_CONTINUATION_TOKEN);
            logDebug("Continuation token: " + continueToken);
        } while (!TextUtils.isEmpty(continueToken));

        return verificationFailed ? IABHELPER_VERIFICATION_FAILED.code : OK.code;
    }

    private int querySkuDetails(ItemType itemType, Inventory inv, @Nullable List<String> moreSkus)
                                throws RemoteException, JSONException {
        logDebug("Querying SKU details.");
        ArrayList<String> skuList = new ArrayList<String>();
        skuList.addAll(inv.getAllOwnedSkus(itemType));
        if (moreSkus != null) {
            for (String sku : moreSkus) {
                if (!skuList.contains(sku)) {
                    skuList.add(sku);
                }
            }
        }

        if (skuList.size() == 0) {
            logDebug("queryPrices: nothing to do because there are no SKUs.");
            return OK.code;
        }

        Bundle querySkus = new Bundle();
        querySkus.putStringArrayList(GET_SKU_DETAILS_ITEM_LIST, skuList);
        Bundle skuDetails = mService.getSkuDetails(API_VERSION, mContext.getPackageName(),
                itemType.toString(), querySkus);

        if (!skuDetails.containsKey(RESPONSE_GET_SKU_DETAILS_LIST)) {
            int response = getResponseCodeFromBundle(skuDetails);
            if (response != OK.code) {
                logDebug("getSkuDetails() failed: " + getDescription(response));
                return response;
            } else {
                logError("getSkuDetails() returned a bundle with neither an error nor a detail list.");
                return IABHELPER_BAD_RESPONSE.code;
            }
        }

        ArrayList<String> responseList = skuDetails.getStringArrayList(
                RESPONSE_GET_SKU_DETAILS_LIST);

        for (String thisResponse : responseList) {
            SkuDetails d = new SkuDetails(itemType, thisResponse);
            logDebug("Got sku details: " + d);
            inv.addSkuDetails(d);
        }
        return OK.code;
    }


    private ConsumeTask consumeAsyncInternal(final List<Purchase> purchases,
                              final @Nullable OnConsumeFinishedListener singleListener,
                              final @Nullable OnConsumeMultiFinishedListener multiListener) {
        return (ConsumeTask) new ConsumeTask(this, singleListener, multiListener).execute(purchases.toArray(new Purchase[purchases.size()]));
    }

    private void logDebug(String msg) {
        if (mDebugLog) Log.d(mDebugTag, msg);
    }

    private void logError(String msg) {
        logError(msg, null);
    }

    private void logError(String msg, Throwable t) {
        Log.e(mDebugTag, "In-app billing error: " + msg, t);
    }

    private void logWarn(String msg) {
        Log.w(mDebugTag, "In-app billing warning: " + msg);
    }

    protected void setSignatureValidator(SignatureValidator validator) {
        if (validator == null) throw new IllegalArgumentException("need non-null validator");
        mSignatureValidator = validator;
    }
}

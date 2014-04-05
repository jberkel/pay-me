package com.github.jberkel.pay.me;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import com.android.vending.billing.IInAppBillingService;
import com.github.jberkel.pay.me.listener.OnConsumeFinishedListener;
import com.github.jberkel.pay.me.listener.OnConsumeMultiFinishedListener;
import com.github.jberkel.pay.me.listener.OnIabPurchaseFinishedListener;
import com.github.jberkel.pay.me.listener.OnIabSetupFinishedListener;
import com.github.jberkel.pay.me.listener.QueryInventoryFinishedListener;
import com.github.jberkel.pay.me.model.Inventory;
import com.github.jberkel.pay.me.model.Purchase;
import com.github.jberkel.pay.me.validator.SignatureValidator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.res.builder.RobolectricPackageManager;
import org.robolectric.shadows.ShadowLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.github.jberkel.pay.me.IabConsts.*;
import static com.github.jberkel.pay.me.Response.*;
import static com.github.jberkel.pay.me.model.ItemType.INAPP;
import static com.github.jberkel.pay.me.model.ItemType.SUBS;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class IabHelperTest {
    private static final int TEST_REQUEST_CODE = 42;

    @Mock private IInAppBillingService service;
    @Mock private OnIabSetupFinishedListener setupListener;
    @Mock private OnIabPurchaseFinishedListener purchaseFinishedListener;
    @Mock private SignatureValidator signatureValidator;


    private IabHelper helper;
    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        when(signatureValidator.validate(anyString(), anyString())).thenReturn(true);

        helper = new IabHelper(Robolectric.application, signatureValidator) {
            @Override
            protected IInAppBillingService getInAppBillingService(IBinder binder) {
                return service;
            }
        };
        helper.enableDebugLogging(true, getClass().getSimpleName());
        ShadowLog.stream = System.out;
    }
    @After public void after() { /* verify(service); */ }

    @Test public void shouldCreateHelper() throws Exception {
        IabHelper helper = new IabHelper(Robolectric.application, PUBLIC_KEY);
        assertThat(helper.isDisposed()).isFalse();
    }

    @Test public void shouldCreateHelperWithValidator() throws Exception {
        SignatureValidator validator = mock(SignatureValidator.class);
        new IabHelper(Robolectric.application, validator);
    }

    @Test(expected = IllegalArgumentException.class) public void shouldRequireValidator() throws Exception {
        new IabHelper(Robolectric.application, (SignatureValidator)null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldCheckPublicKeyEagerly() throws Exception {
        new IabHelper(Robolectric.application, "INVALIDKEY");
    }

    @Test public void shouldStartSetup_SuccessCase() throws Exception {
        registerServiceWithPackageManager();

        helper.startSetup(setupListener);
        verify(setupListener).onIabSetupFinished(new IabResult(OK));
    }

    @Test public void shouldBeInSetupStateWhenFinishedListenerIsCalled() throws Exception {
        registerServiceWithPackageManager();
        helper.startSetup(new OnIabSetupFinishedListener() {
            @Override
            public void onIabSetupFinished(IabResult result) {
                assertThat(result.getResponse()).isEqualTo(OK);
                assertThat(helper.isDisposed()).isFalse();
                helper.checkSetupDone("test");
            }
        });
    }

    @Test public void shouldBeInSetupStateWhenFinishedListenerIsCalledNoBillingAvailable() throws Exception {
        helper.startSetup(new OnIabSetupFinishedListener() {
            @Override
            public void onIabSetupFinished(IabResult result) {
                assertThat(result.getResponse()).isEqualTo(BILLING_UNAVAILABLE);
                assertThat(helper.isDisposed()).isFalse();
                helper.checkSetupDone("test");
            }
        });
    }

    @Test public void shouldStartSetup_BillingServiceDoesNotExist() throws Exception {
        helper.startSetup(setupListener);
        verify(setupListener).onIabSetupFinished(new IabResult(BILLING_UNAVAILABLE));
    }

    @Test public void shouldStartSetup_ServiceDoesNotSupportBilling() throws Exception {
        registerServiceWithPackageManager();
        when(service.isBillingSupported(eq(API_VERSION), anyString(), anyString())).thenReturn(BILLING_UNAVAILABLE.code);

        helper.startSetup(setupListener);
        verify(setupListener).onIabSetupFinished(new IabResult(BILLING_UNAVAILABLE));

        assertThat(helper.subscriptionsSupported()).isFalse();
    }

    @Test public void shouldStartSetup_CheckForSubscriptions_Unavailable() throws Exception {
        registerServiceWithPackageManager();
        when(service.isBillingSupported(eq(API_VERSION), anyString(), eq("inapp"))).thenReturn(OK.code);
        when(service.isBillingSupported(eq(API_VERSION), anyString(), eq("subs"))).thenReturn(BILLING_UNAVAILABLE.code);

        helper.startSetup(setupListener);

        verify(setupListener).onIabSetupFinished(new IabResult(OK));
        assertThat(helper.subscriptionsSupported()).isFalse();
    }

    @Test public void shouldStartSetup_CheckForSubscriptions_Success() throws Exception {
        registerServiceWithPackageManager();
        when(service.isBillingSupported(eq(API_VERSION), anyString(), eq("inapp"))).thenReturn(OK.code);
        when(service.isBillingSupported(eq(API_VERSION), anyString(), eq("subs"))).thenReturn(OK.code);

        helper.startSetup(setupListener);

        verify(setupListener).onIabSetupFinished(new IabResult(OK));
        assertThat(helper.subscriptionsSupported()).isTrue();
    }

    @Test public void shouldStartSetup_ServiceExistsButThrowsException() throws Exception {
        registerServiceWithPackageManager();
        when(service.isBillingSupported(eq(API_VERSION), anyString(), anyString())).thenThrow(new RemoteException());

        helper.startSetup(setupListener);
        verify(setupListener).onIabSetupFinished(new IabResult(IABHELPER_REMOTE_EXCEPTION));

        assertThat(helper.subscriptionsSupported()).isFalse();
    }

    @Test public void shouldStartSetup_ServiceExistsButDisposed() throws Exception {
        registerServiceWithPackageManager();
        when(service.isBillingSupported(eq(API_VERSION), anyString(), anyString())).thenReturn(OK.code);
        helper.startSetup(setupListener);
        verify(setupListener).onIabSetupFinished(new IabResult(OK));
        helper.dispose();
        assertThat(helper.subscriptionsSupported()).isFalse();
    }

    @Test public void shouldDisposeAfterStartupAndUnbindServiceConnection() throws Exception {
        shouldStartSetup_SuccessCase();
        assertThat(helper.isDisposed()).isFalse();
        helper.dispose();

        List<ServiceConnection> unboundServiceConnections =
                Robolectric.shadowOf(Robolectric.application).getUnboundServiceConnections();

        assertThat(unboundServiceConnections).hasSize(1);
        assertThat(helper.isDisposed()).isTrue();
    }

    @Test (expected = IllegalStateException.class)
    public void shouldRaiseExceptionIfStartingAndObjectIsDisposed() throws Exception {
        shouldStartSetup_SuccessCase();
        helper.dispose();
        helper.startSetup(setupListener);
    }

    // launchPurchaseFlow
    @Test(expected = IllegalArgumentException.class)
    public void shouldRaiseExceptionIfPurchaseFlowLaunchedWithEmptySky() throws Exception {
        shouldStartSetup_SuccessCase();
        helper.launchPurchaseFlow(mock(Activity.class), "", INAPP, 0, purchaseFinishedListener, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRaiseExceptionIfPurchaseFlowLaunchedWithNullType() throws Exception {
        shouldStartSetup_SuccessCase();
        helper.launchPurchaseFlow(mock(Activity.class), "sku", null, 0, purchaseFinishedListener, null);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldRaiseExceptionIfPurchaseFlowLaunchedWithoutSetup() throws Exception {
        helper.launchPurchaseFlow(mock(Activity.class), "sku", INAPP, 0, purchaseFinishedListener, "");
    }

    @Test
    public void shouldCallListenerWithDisposedReponseIfHelperIsDisposed() throws Exception {
        shouldStartSetup_SuccessCase();
        helper.dispose();
        helper.launchPurchaseFlow(mock(Activity.class), "sku", INAPP, 0, purchaseFinishedListener, "");
        verify(purchaseFinishedListener).onIabPurchaseFinished(new IabResult(Response.IABHELPER_DISPOSED), null);
    }

    @Test public void shouldFailPurchaseWhenEmptyResponseIsReturned() throws Exception {
        shouldStartSetup_SuccessCase();
        Bundle empty = new Bundle();
        when(service.getBuyIntent(API_VERSION, Robolectric.application.getPackageName(), "sku", "inapp", "")).thenReturn(empty);

        helper.launchPurchaseFlow(mock(Activity.class), "sku", INAPP, 0, purchaseFinishedListener, "");

        verify(purchaseFinishedListener).onIabPurchaseFinished(
                new IabResult(IABHELPER_SEND_INTENT_FAILED),
                null);
    }

    @Test public void shouldFailPurchaseWhenErrorIsReturned() throws Exception {
        shouldStartSetup_SuccessCase();
        Bundle errorResponse = new Bundle();
        errorResponse.putInt(RESPONSE_CODE, ERROR.code);
        when(service.getBuyIntent(API_VERSION, Robolectric.application.getPackageName(), "sku", "inapp", "")).thenReturn(errorResponse);

        helper.launchPurchaseFlow(mock(Activity.class), "sku", INAPP, TEST_REQUEST_CODE, purchaseFinishedListener, "");

        verify(purchaseFinishedListener).onIabPurchaseFinished(
                new IabResult(ERROR, "Unable to buy item"),
                null);
    }

    @Test public void shouldFailPurchaseWhenSendIntentExceptionIsThrown() throws Exception {
        shouldStartSetup_SuccessCase();
        Activity activity = new Activity() {
            @Override
            public void startIntentSenderForResult(IntentSender intent, int requestCode, Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags) throws IntentSender.SendIntentException {
                throw new IntentSender.SendIntentException("Failz");
            }
        };
        Bundle response = new Bundle();
        response.putParcelable(RESPONSE_BUY_INTENT, PendingIntent.getActivity(Robolectric.application, 0, new Intent(), 0));

        when(service.getBuyIntent(API_VERSION, Robolectric.application.getPackageName(), "sku", "inapp", "")).thenReturn(response);
        helper.launchPurchaseFlow(activity, "sku", INAPP, TEST_REQUEST_CODE, purchaseFinishedListener, "");

        verify(purchaseFinishedListener).onIabPurchaseFinished(
                new IabResult(IABHELPER_SEND_INTENT_FAILED),
                null);
    }

    @Test public void shouldFailPurchaseWhenRemoteExceptionIsThrown() throws Exception {
        shouldStartSetup_SuccessCase();

        when(service.getBuyIntent(API_VERSION, Robolectric.application.getPackageName(), "sku", "inapp", "")).thenThrow(new RemoteException());
        helper.launchPurchaseFlow(null, "sku", INAPP, TEST_REQUEST_CODE, purchaseFinishedListener, "");

        verify(purchaseFinishedListener).onIabPurchaseFinished(
                new IabResult(IABHELPER_REMOTE_EXCEPTION),
                null);
    }

    @Test public void shouldFailPurchaseWhenBillingUnsupported() throws Exception {
        shouldStartSetup_ServiceDoesNotSupportBilling();

        helper.launchPurchaseFlow(null, "sku", INAPP, TEST_REQUEST_CODE, purchaseFinishedListener, "");

        verify(purchaseFinishedListener).onIabPurchaseFinished(
                new IabResult(BILLING_UNAVAILABLE),
                null);
    }

    @Test public void shouldFailPurchaseWhenBillingUnsupported_NoService() throws Exception {
        shouldStartSetup_BillingServiceDoesNotExist();

        helper.launchPurchaseFlow(null, "sku", INAPP, TEST_REQUEST_CODE, purchaseFinishedListener, "");

        verify(purchaseFinishedListener).onIabPurchaseFinished(
                new IabResult(BILLING_UNAVAILABLE),
                null);
    }

    @Test public void shouldFailSubscriptionPurchaseWhenUnsupported() throws Exception {
        shouldStartSetup_CheckForSubscriptions_Unavailable();
        helper.launchPurchaseFlow(null, "sku", SUBS, TEST_REQUEST_CODE, purchaseFinishedListener, "");

        verify(purchaseFinishedListener).onIabPurchaseFinished(
                new IabResult(IABHELPER_SUBSCRIPTIONS_NOT_AVAILABLE),
                null);
    }

    @Test public void shouldStartIntentAfterSuccessfulLaunchPurchase() throws Exception {
        shouldStartSetup_SuccessCase();

        Bundle response = new Bundle();
        response.putParcelable(RESPONSE_BUY_INTENT, PendingIntent.getActivity(Robolectric.application, 0, new Intent(), 0));

        when(service.getBuyIntent(API_VERSION, Robolectric.application.getPackageName(), "sku", "inapp", "")).thenReturn(response);

        Activity activity = mock(Activity.class);
        helper.launchPurchaseFlow(activity, "sku", INAPP, TEST_REQUEST_CODE, purchaseFinishedListener, "");
        verify(activity).startIntentSenderForResult(any(IntentSender.class), eq(TEST_REQUEST_CODE), any(Intent.class), eq(0), eq(0), eq(0));
    }

    @Test public void shouldStartIntentAfterSuccessfulLaunchPurchaseForSubscription() throws Exception {
        shouldStartSetup_SuccessCase();

        Bundle response = new Bundle();
        response.putParcelable(RESPONSE_BUY_INTENT, PendingIntent.getActivity(Robolectric.application, 0, new Intent(), 0));

        when(service.getBuyIntent(API_VERSION, Robolectric.application.getPackageName(), "sku", "subs", "")).thenReturn(response);

        Activity activity = mock(Activity.class);
        helper.launchPurchaseFlow(activity, "sku", SUBS, TEST_REQUEST_CODE, purchaseFinishedListener, "");
        verify(activity).startIntentSenderForResult(any(IntentSender.class), eq(TEST_REQUEST_CODE), any(Intent.class), eq(0), eq(0), eq(0));
    }

    @Test public void shouldLaunchSubscriptionPurchaseFlowWithoutExtraData() throws Exception {
        shouldStartSetup_SuccessCase();

        Bundle response = new Bundle();
        response.putParcelable(RESPONSE_BUY_INTENT, PendingIntent.getActivity(Robolectric.application, 0, new Intent(), 0));

        when(service.getBuyIntent(API_VERSION, Robolectric.application.getPackageName(), "sku", "subs", "")).thenReturn(response);
        Activity activity = mock(Activity.class);
        helper.launchPurchaseFlow(activity, "sku", SUBS, TEST_REQUEST_CODE, purchaseFinishedListener, "");
        verify(activity).startIntentSenderForResult(any(IntentSender.class), eq(TEST_REQUEST_CODE), any(Intent.class), eq(0), eq(0), eq(0));
    }

    // handleActivityResult

    @Test public void handleActivityResultRequestCodeMismatch() throws Exception {
        assertThat(helper.handleActivityResult(TEST_REQUEST_CODE, 0, null)).isFalse();
    }

    @Test public void shouldLaunchPurchaseAndStartIntentAndThenHandleActivityResultNullData() throws Exception {
        shouldStartIntentAfterSuccessfulLaunchPurchase();
        assertThat(helper.handleActivityResult(TEST_REQUEST_CODE, 0, null)).isTrue();
    }

    @Test public void shouldLaunchPurchaseAndStartIntentAndThenHandleActivityResultWithData() throws Exception {
        shouldStartIntentAfterSuccessfulLaunchPurchase();
        Intent data = new Intent();
        data.putExtra(RESPONSE_CODE, OK.code);
        data.putExtra(RESPONSE_INAPP_PURCHASE_DATA, "{ \"productId\": \"foo\" }");
        data.putExtra(RESPONSE_INAPP_SIGNATURE, "");

        assertThat(helper.handleActivityResult(TEST_REQUEST_CODE, Activity.RESULT_OK, data)).isTrue();
        verify(purchaseFinishedListener).onIabPurchaseFinished(eq(new IabResult(OK)), any(Purchase.class));
    }

    @Test public void shouldLaunchPurchaseAndStartIntentAndThenHandleActivityResultWithoutData() throws Exception {
        shouldStartIntentAfterSuccessfulLaunchPurchase();
        Intent data = new Intent();
        data.putExtra(RESPONSE_CODE, OK.code);

        assertThat(helper.handleActivityResult(TEST_REQUEST_CODE, Activity.RESULT_OK, data)).isTrue();
        verify(purchaseFinishedListener).onIabPurchaseFinished(eq(new IabResult(IABHELPER_UNKNOWN_ERROR, "IAB returned null purchaseData or dataSignature")), any(Purchase.class));
    }

    @Test public void shouldLaunchPurchaseAndStartIntentAndThenHandleActivityResultWithInvalidData() throws Exception {
        shouldStartIntentAfterSuccessfulLaunchPurchase();
        Intent data = new Intent();
        data.putExtra(RESPONSE_CODE, OK.code);
        data.putExtra(RESPONSE_INAPP_PURCHASE_DATA, "this is not json");
        data.putExtra(RESPONSE_INAPP_SIGNATURE, "");

        assertThat(helper.handleActivityResult(TEST_REQUEST_CODE, Activity.RESULT_OK, data)).isTrue();
        verify(purchaseFinishedListener).onIabPurchaseFinished(new IabResult(IABHELPER_BAD_RESPONSE, "Failed to parse purchase data."), null);
    }

    @Test public void shouldLaunchPurchaseAndStartIntentAndThenHandleActivityResultWithErrorResponseCode() throws Exception {
        shouldStartIntentAfterSuccessfulLaunchPurchase();
        Intent data = new Intent();
        data.putExtra(RESPONSE_CODE, ERROR.code);

        assertThat(helper.handleActivityResult(TEST_REQUEST_CODE, Activity.RESULT_OK, data)).isTrue();

        verify(purchaseFinishedListener).onIabPurchaseFinished(new IabResult(ERROR, "Problem purchashing item."), null);
    }

    @Test public void shouldLaunchPurchaseAndStartIntentAndThenHandleActivityResultWithCanceledResultCode() throws Exception {
        shouldStartIntentAfterSuccessfulLaunchPurchase();
        Intent data = new Intent();
        data.putExtra(RESPONSE_CODE, ITEM_UNAVAILABLE.code);

        assertThat(helper.handleActivityResult(TEST_REQUEST_CODE, Activity.RESULT_CANCELED, data)).isTrue();
        verify(purchaseFinishedListener).onIabPurchaseFinished(new IabResult(ITEM_UNAVAILABLE), null);
    }

    @Test public void shouldLaunchPurchaseAndStartIntentAndThenHandleActivityResultWithUnknownResultCode() throws Exception {
        shouldStartIntentAfterSuccessfulLaunchPurchase();
        Intent data = new Intent();
        data.putExtra(RESPONSE_CODE, OK.code);

        assertThat(helper.handleActivityResult(TEST_REQUEST_CODE, 23, data)).isFalse();
        verify(purchaseFinishedListener).onIabPurchaseFinished(new IabResult(IABHELPER_UNKNOWN_PURCHASE_RESPONSE), null);
    }

    @Test public void shouldLaunchPurchaseAndStartIntentAndThenHandleActivityResultWhenDisposed() throws Exception {
        shouldStartIntentAfterSuccessfulLaunchPurchase();
        Intent data = new Intent();
        data.putExtra(RESPONSE_CODE, OK.code);
        data.putExtra(RESPONSE_INAPP_PURCHASE_DATA, "{ \"productId\": \"foo\" }");
        data.putExtra(RESPONSE_INAPP_SIGNATURE, "");
        helper.dispose();
        assertThat(helper.handleActivityResult(TEST_REQUEST_CODE, Activity.RESULT_OK, data)).isFalse();
    }

    @Test public void shouldHandleInvalidSignature() throws Exception {
        shouldStartIntentAfterSuccessfulLaunchPurchase();

        Intent data = new Intent();
        data.putExtra(RESPONSE_CODE, OK.code);
        String purchaseData = "{ \"productId\": \"foo\" }";
        data.putExtra(RESPONSE_INAPP_PURCHASE_DATA, purchaseData);
        data.putExtra(RESPONSE_INAPP_SIGNATURE, "some signature");

        when(signatureValidator.validate(eq(purchaseData), eq("some signature"))).thenReturn(false);

        assertThat(helper.handleActivityResult(TEST_REQUEST_CODE, Activity.RESULT_OK, data)).isTrue();
        verify(purchaseFinishedListener).onIabPurchaseFinished(
                eq(new IabResult(IABHELPER_VERIFICATION_FAILED)), any(Purchase.class));
    }

    // inventory

    @Test public void shouldQueryInventoryWithoutSubscriptions() throws Exception {
        shouldStartSetup_CheckForSubscriptions_Unavailable();

        Bundle response = createInventoryResponseBundle("foo");

        when(service.getPurchases(API_VERSION, Robolectric.application.getPackageName(), "inapp", null))
                        .thenReturn(response);

        Inventory inventory = helper.queryInventory(false, null ,null);
        assertThat(inventory.getAllPurchases()).hasSize(1);
        assertThat(inventory.getAllOwnedSkus()).hasSize(1);
        assertThat(inventory.getSkuDetails()).isEmpty();
    }

    @Test(expected = IabException.class)
    public void shouldQueryInventoryWhenDisposedThrowIabException() throws Exception {
        shouldStartSetup_CheckForSubscriptions_Unavailable();
        helper.dispose();
        Inventory inventory = helper.queryInventory(false, null ,null);
    }

    @Test public void shouldQueryInventoryWithoutSubscriptionsButSkuDetails() throws Exception {
        shouldStartSetup_CheckForSubscriptions_Unavailable();

        Bundle response = createInventoryResponseBundle("foo");
        Bundle skuDetails = createSkuDetailsResponseBundle("foo");

        when(service.getPurchases(API_VERSION, Robolectric.application.getPackageName(), "inapp", null))
                .thenReturn(response);


        when(service.getSkuDetails(eq(API_VERSION),
                eq(Robolectric.application.getPackageName()),
                eq("inapp"),
                TestHelper.BundleStringArrayListMatcher.bundleWithStringValues(GET_SKU_DETAILS_ITEM_LIST, "foo")))
           .thenReturn(skuDetails);

        Inventory inventory = helper.queryInventory(true, null ,null);

        assertThat(inventory.getAllPurchases()).hasSize(1);
        assertThat(inventory.getAllOwnedSkus()).hasSize(1);
        assertThat(inventory.getSkuDetails()).hasSize(1);
    }

    @Test public void shouldQueryInventoryWithoutSubscriptionsButSkuDetailsAndMoreSkus() throws Exception {
        shouldStartSetup_CheckForSubscriptions_Unavailable();

        Bundle response = createInventoryResponseBundle("foo");
        Bundle skuDetails = createSkuDetailsResponseBundle("foo");

        when(service.getPurchases(API_VERSION, Robolectric.application.getPackageName(), "inapp", null))
                .thenReturn(response);


        when(service.getSkuDetails(eq(API_VERSION),
                eq(Robolectric.application.getPackageName()),
                eq("inapp"),
                TestHelper.BundleStringArrayListMatcher.bundleWithStringValues(GET_SKU_DETAILS_ITEM_LIST, "foo", "anotherSku")))
                .thenReturn(skuDetails);

        Inventory inventory = helper.queryInventory(true, asArrayList("anotherSku"), null);

        assertThat(inventory.getAllPurchases()).hasSize(1);
        assertThat(inventory.getAllOwnedSkus()).hasSize(1);
        assertThat(inventory.getSkuDetails()).hasSize(1);
    }

    @Test(expected = IabException.class) public void shouldQueryInventoryWithoutSubscriptionsButSkuDetailsAndSkuError() throws Exception {
        shouldStartSetup_CheckForSubscriptions_Unavailable();

        Bundle response =  createInventoryResponseBundle("foo");

        Bundle skuDetails = new Bundle();
        skuDetails.putInt(RESPONSE_CODE, ERROR.code);

        when(service.getPurchases(API_VERSION, Robolectric.application.getPackageName(), "inapp", null))
                .thenReturn(response);

        when(service.getSkuDetails(eq(API_VERSION),
                eq(Robolectric.application.getPackageName()),
                eq("inapp"),
                any(Bundle.class)))
                .thenReturn(skuDetails);

        helper.queryInventory(true, null, null);
    }


    @Test(expected = IabException.class) public void shouldQueryInventoryButServiceReturnsInconsistentData() throws Exception {
        shouldStartSetup_CheckForSubscriptions_Unavailable();

        Bundle response = new Bundle();
        response.putStringArrayList(RESPONSE_INAPP_ITEM_LIST, asArrayList()); // NB empty list here
        response.putStringArrayList(RESPONSE_INAPP_PURCHASE_DATA_LIST, asArrayList("{}"));
        response.putStringArrayList(RESPONSE_INAPP_SIGNATURE_LIST, asArrayList(""));
        response.putString(INAPP_CONTINUATION_TOKEN, "");

        Bundle skuDetails = new Bundle();
        skuDetails.putInt(RESPONSE_CODE, OK.code);

        when(service.getPurchases(API_VERSION, Robolectric.application.getPackageName(), "inapp", null))
                .thenReturn(response);

        when(service.getSkuDetails(eq(API_VERSION),
                eq(Robolectric.application.getPackageName()),
                eq("inapp"),
                any(Bundle.class)))
                .thenReturn(skuDetails);

        helper.queryInventory(true, null, null);
    }


    @Test public void shouldQueryInventoryWithEmptySkuDetails() throws Exception {
        shouldStartSetup_CheckForSubscriptions_Unavailable();

        Bundle response = createInventoryResponseBundle();
        Bundle skuDetails = new Bundle();

        when(service.getPurchases(API_VERSION, Robolectric.application.getPackageName(), "inapp", null))
                .thenReturn(response);

        when(service.getSkuDetails(eq(API_VERSION),
                eq(Robolectric.application.getPackageName()),
                eq("inapp"),
                any(Bundle.class)))
                .thenReturn(skuDetails);

        Inventory inv = helper.queryInventory(true, null, null);
        assertThat(inv.getAllOwnedSkus().isEmpty());
    }

    @Test public void shouldQueryInventoryEmptySkuDetails() throws Exception {
        shouldStartSetup_CheckForSubscriptions_Unavailable();

        Bundle response = createInventoryResponseBundle();
        Bundle skuDetails = new Bundle();

        when(service.getPurchases(API_VERSION, Robolectric.application.getPackageName(), "inapp", null))
                .thenReturn(response);

        when(service.getSkuDetails(eq(API_VERSION),
                eq(Robolectric.application.getPackageName()),
                eq("inapp"),
                any(Bundle.class)))
                .thenReturn(skuDetails);

        Inventory inv = helper.queryInventory(true, null, null);
        assertThat(inv.getAllOwnedSkus().isEmpty());
    }


    @Test public void shouldQueryInventoryWithSubscriptions() throws Exception {
        shouldStartSetup_SuccessCase();

        Bundle response = createInventoryResponseBundle("foo");

        when(service.getPurchases(API_VERSION, Robolectric.application.getPackageName(), "inapp", null))
                .thenReturn(response);
        when(service.getPurchases(API_VERSION, Robolectric.application.getPackageName(), "subs", null))
                .thenReturn(response);

        Inventory inventory = helper.queryInventory(false, null ,null);
        assertThat(inventory.getAllPurchases()).hasSize(1);
        assertThat(inventory.getAllOwnedSkus()).hasSize(1);
        assertThat(inventory.getSkuDetails()).isEmpty();
    }

    @Test public void shouldQueryInventoryWithSubscriptionsAndSkus() throws Exception {
        shouldStartSetup_SuccessCase();
        when(service.getPurchases(API_VERSION, Robolectric.application.getPackageName(), "inapp", null))
                .thenReturn(createInventoryResponseBundle("foo"));

        when(service.getPurchases(API_VERSION, Robolectric.application.getPackageName(), "subs", null))
                .thenReturn(createInventoryResponseBundle("bar"));

        when(service.getSkuDetails(eq(API_VERSION),
                eq(Robolectric.application.getPackageName()),
                eq("inapp"),
                any(Bundle.class)))
                .thenReturn(createSkuDetailsResponseBundle("foo"));

        when(service.getSkuDetails(eq(API_VERSION),
                eq(Robolectric.application.getPackageName()),
                eq("subs"),
                any(Bundle.class)))
                .thenReturn(createSkuDetailsResponseBundle("bar"));

        Inventory inventory = helper.queryInventory(true, null ,null);

        assertThat(inventory.getAllPurchases()).hasSize(2);
        assertThat(inventory.getAllOwnedSkus()).hasSize(2);
        assertThat(inventory.getSkuDetails()).hasSize(2);
    }

    @Test public void shouldQueryInventoryWithSubscriptionsAndSkusAndMoreSkus() throws Exception {
        shouldStartSetup_SuccessCase();
        when(service.getPurchases(API_VERSION, Robolectric.application.getPackageName(), "inapp", null))
                .thenReturn(createInventoryResponseBundle("inappSku"));

        when(service.getPurchases(API_VERSION, Robolectric.application.getPackageName(), "subs", null))
                .thenReturn(createInventoryResponseBundle("subSku"));

        when(service.getSkuDetails(eq(API_VERSION),
                eq(Robolectric.application.getPackageName()),
                eq("inapp"),
                TestHelper.BundleStringArrayListMatcher.bundleWithStringValues(GET_SKU_DETAILS_ITEM_LIST, "inappSku", "anotherInAppSku")))
                .thenReturn(createSkuDetailsResponseBundle("inappSku", "anotherInappSku"));

        when(service.getSkuDetails(eq(API_VERSION),
                eq(Robolectric.application.getPackageName()),
                eq("subs"),
                TestHelper.BundleStringArrayListMatcher.bundleWithStringValues(GET_SKU_DETAILS_ITEM_LIST, "subSku", "anotherSubSku")))
                .thenReturn(createSkuDetailsResponseBundle("subSku", "anotherSubSku"));

        Inventory inventory = helper.queryInventory(true,
                asArrayList("anotherInAppSku"),
                asArrayList("anotherSubSku"));

        assertThat(inventory.getAllOwnedSkus(INAPP)).hasSize(1);
        assertThat(inventory.getAllOwnedSkus(SUBS)).hasSize(1);

        assertThat(inventory.getAllPurchases()).hasSize(2); // inappSku + subSku
        assertThat(inventory.getAllOwnedSkus()).hasSize(2);

        assertThat(inventory.getSkuDetails()).hasSize(4);
    }


    @Test(expected = IabException.class) public void shouldQueryInventoryRemoteException() throws Exception {
        shouldStartSetup_SuccessCase();

        when(service.getPurchases(API_VERSION, Robolectric.application.getPackageName(), "inapp", null))
                .thenThrow(new RemoteException());

        helper.queryInventory(false, null, null);
    }

    @Test(expected = IabException.class) public void shouldQueryInventoryErrorCode() throws Exception {
        shouldStartSetup_SuccessCase();

        Bundle response = new Bundle();
        response.putInt(RESPONSE_CODE, DEVELOPER_ERROR.code);
        when(service.getPurchases(API_VERSION, Robolectric.application.getPackageName(), "inapp", null))
                .thenReturn(response);

        helper.queryInventory(false, null, null);
    }

    @Test(expected = IabException.class) public void shouldQueryInventoryJSONException() throws Exception {
        shouldStartSetup_CheckForSubscriptions_Unavailable();

        Bundle response = new Bundle();

        response.putStringArrayList(RESPONSE_INAPP_ITEM_LIST, asArrayList("foo"));
        response.putStringArrayList(RESPONSE_INAPP_PURCHASE_DATA_LIST, asArrayList("not json"));
        response.putStringArrayList(RESPONSE_INAPP_SIGNATURE_LIST, asArrayList(""));

        when(service.getPurchases(API_VERSION, Robolectric.application.getPackageName(), "inapp", null))
                .thenReturn(response);

        helper.queryInventory(false, null ,null);
    }

    @Test(expected = IabException.class) public void shouldQueryInventoryWithoutRequiredFields() throws Exception {
        shouldStartSetup_CheckForSubscriptions_Unavailable();

        Bundle response = new Bundle();

        when(service.getPurchases(API_VERSION, Robolectric.application.getPackageName(), "inapp", null))
                .thenReturn(response);

        helper.queryInventory(false, null ,null);
    }

    @Test public void shouldQueryInventoryInvalidSignature() throws Exception {
        shouldStartSetup_CheckForSubscriptions_Unavailable();
        when(signatureValidator.validate(eq("{}"), eq("INVALID"))).thenReturn(false);

        Bundle response = new Bundle();

        response.putStringArrayList(RESPONSE_INAPP_ITEM_LIST, asArrayList("foo"));
        response.putStringArrayList(RESPONSE_INAPP_PURCHASE_DATA_LIST, asArrayList("{}"));
        response.putStringArrayList(RESPONSE_INAPP_SIGNATURE_LIST, asArrayList("INVALID"));

        response.putString(INAPP_CONTINUATION_TOKEN, "");

        when(service.getPurchases(API_VERSION, Robolectric.application.getPackageName(), "inapp", null))
                .thenReturn(response);

        try {
            helper.queryInventory(false, null ,null);
            fail("expected exception");
        } catch (IabException e) {
            assertThat(e.getResult()).isEqualTo(new IabResult(IABHELPER_VERIFICATION_FAILED,
                    "Error refreshing inventory (querying owned items)."));
        }
    }

    @Test public void queryInventoryAsync() throws Exception {
        shouldStartSetup_CheckForSubscriptions_Unavailable();

        QueryInventoryFinishedListener listener = mock(QueryInventoryFinishedListener.class);

        Bundle response = createInventoryResponseBundle("foo");

        when(service.getPurchases(API_VERSION, Robolectric.application.getPackageName(), "inapp", null))
                .thenReturn(response);

        helper.queryInventoryAsync(false, null, null, listener);
        verify(listener).onQueryInventoryFinished(eq(new IabResult(OK)), any(Inventory.class));
    }

    @Test public void queryInventoryAsyncWhenDisposed() throws Exception {
        shouldStartSetup_CheckForSubscriptions_Unavailable();
        QueryInventoryFinishedListener listener = mock(QueryInventoryFinishedListener.class);

        helper.dispose();
        helper.queryInventoryAsync(false, null, null, listener);

        verify(listener).onQueryInventoryFinished(eq(new IabResult(IABHELPER_DISPOSED)), isNull(Inventory.class));
    }

    // consume
    @Test(expected = IabException.class) public void shouldNotConsumeSubscription() throws Exception {
        shouldStartSetup_SuccessCase();
        Purchase purchase = mock(Purchase.class);
        when(purchase.getToken()).thenReturn("foo");
        when(purchase.getItemType()).thenReturn(SUBS);
        helper.consume(purchase);
    }

    @Test(expected = IabException.class) public void shouldNotConsumePurchaseWithoutType() throws Exception {
        shouldStartSetup_SuccessCase();
        Purchase purchase = mock(Purchase.class);
        when(purchase.getToken()).thenReturn("foo");
        helper.consume(purchase);
    }

    @Test(expected = IabException.class) public void shouldNotConsumeInAppItemWithoutToken() throws Exception {
        shouldStartSetup_SuccessCase();
        Purchase purchase = mock(Purchase.class);
        when(purchase.getItemType()).thenReturn(INAPP);
        helper.consume(purchase);
    }

    @Test(expected = IabException.class) public void shouldNotConsumeWhenDisposed() throws Exception {
        shouldStartSetup_SuccessCase();
        helper.dispose();
        Purchase purchase = mock(Purchase.class);
        when(purchase.getItemType()).thenReturn(INAPP);
        helper.consume(purchase);
    }

    @Test public void shouldConsumeInAppItem() throws Exception {
        shouldStartSetup_SuccessCase();
        Purchase purchase = mock(Purchase.class);
        when(purchase.getToken()).thenReturn("foo");
        when(purchase.getItemType()).thenReturn(INAPP);

        when(service.consumePurchase(API_VERSION, Robolectric.application.getPackageName(), "foo")).thenReturn(OK.code);
        helper.consume(purchase);
    }

    @Test(expected = IabException.class) public void shouldConsumeInAppItemErrorResponse() throws Exception {
        shouldStartSetup_SuccessCase();
        Purchase purchase = mock(Purchase.class);
        when(purchase.getToken()).thenReturn("foo");
        when(purchase.getItemType()).thenReturn(INAPP);
        when(service.consumePurchase(API_VERSION, Robolectric.application.getPackageName(), "foo")).thenReturn(ERROR.code);
        helper.consume(purchase);
    }

    @Test(expected = IabException.class) public void shouldConsumeInAppItemRemoteException() throws Exception {
        shouldStartSetup_SuccessCase();
        Purchase purchase = mock(Purchase.class);
        when(service.consumePurchase(API_VERSION, Robolectric.application.getPackageName(), "foo")).thenThrow(new RemoteException());
        helper.consume(purchase);
    }

    @Test public void shouldConsumeAsyncSinglePurchase() throws Exception {
        shouldStartSetup_SuccessCase();
        Purchase purchase = mock(Purchase.class);
        when(purchase.getToken()).thenReturn("foo");
        when(purchase.getItemType()).thenReturn(INAPP);

        OnConsumeFinishedListener listener = mock(OnConsumeFinishedListener.class);

        when(service.consumePurchase(API_VERSION, Robolectric.application.getPackageName(), "foo")).thenReturn(OK.code);
        helper.consumeAsync(purchase, listener);
        verify(listener).onConsumeFinished(purchase, new IabResult(OK));
    }

    @Test public void shouldConsumeAsyncMultiplePurchase() throws Exception {
        shouldStartSetup_SuccessCase();
        Purchase p1 = mock(Purchase.class);
        when(p1.getToken()).thenReturn("foo");
        when(p1.getItemType()).thenReturn(INAPP);

        Purchase p2 = mock(Purchase.class);
        when(p2.getToken()).thenReturn("bar");
        when(p2.getItemType()).thenReturn(INAPP);

        List<Purchase> purchases = new ArrayList<Purchase>();
        purchases.add(p1);
        purchases.add(p2);

        OnConsumeMultiFinishedListener listener = mock(OnConsumeMultiFinishedListener.class);

        when(service.consumePurchase(API_VERSION, Robolectric.application.getPackageName(), "foo")).thenReturn(OK.code);
        when(service.consumePurchase(API_VERSION, Robolectric.application.getPackageName(), "bar")).thenReturn(ERROR.code);
        helper.consumeAsync(purchases, listener);

        List<IabResult> results = new ArrayList<IabResult>();
        results.add(new IabResult(OK));
        results.add(new IabResult(ERROR));

        verify(listener).onConsumeMultiFinished(purchases, results);
    }

    // getResponseCodeFromBundle
    @Test public void shouldGetResponseCodeFromBundleEmpty() throws Exception {
        Bundle b = new Bundle();
        assertThat(helper.getResponseCodeFromBundle(b)).isEqualTo(OK.code);
    }

    @Test public void shouldGetResponseCodeFromBundleInt() throws Exception {
        Bundle b = new Bundle();
        b.putInt(RESPONSE_CODE, 20);
        assertThat(helper.getResponseCodeFromBundle(b)).isEqualTo(20);
    }

    @Test public void shouldGetResponseCodeFromBundleLong() throws Exception {
        Bundle b = new Bundle();
        b.putLong(RESPONSE_CODE, 60L);
        assertThat(helper.getResponseCodeFromBundle(b)).isEqualTo(60);
    }

    @Test(expected = RuntimeException.class) public void shouldGetResponseCodeFromBundleUnknown() throws Exception {
        Bundle b = new Bundle();
        b.putString(RESPONSE_CODE, "invalid");
        helper.getResponseCodeFromBundle(b);
    }

    // other methods
    @Test(expected = IllegalStateException.class) public void testFlagAsync() throws Exception {
        helper.flagStartAsync("something");
        helper.flagEndAsync();
        helper.flagStartAsync("something");
        helper.flagStartAsync("something else");
    }

    private Context registerServiceWithPackageManager() {
        Context context = Robolectric.application;
        RobolectricPackageManager pm = (RobolectricPackageManager) context.getPackageManager();
        pm.addResolveInfoForIntent(IabHelper.BIND_BILLING_SERVICE, new ResolveInfo());
        return context;
    }

    private static ArrayList<String> asArrayList(String... elements) {
        ArrayList<String> list = new ArrayList<String>();
        Collections.addAll(list, elements);
        return list;
    }


    private static Bundle createInventoryResponseBundle(String... skus) {
        Bundle response = new Bundle();
        ArrayList<String> itemList = new ArrayList<String>(skus.length);
        ArrayList<String> signatures = new ArrayList<String>(skus.length);
        ArrayList<String> purchaseData = new ArrayList<String>(skus.length);
        for (String sku : skus) {
            itemList.add(sku);
            signatures.add("");
            purchaseData.add("{ \"productId\": \""+sku+"\" }");
        }
        response.putInt(RESPONSE_CODE, OK.code);
        response.putStringArrayList(RESPONSE_INAPP_ITEM_LIST, itemList);
        response.putStringArrayList(RESPONSE_INAPP_PURCHASE_DATA_LIST, purchaseData);
        response.putStringArrayList(RESPONSE_INAPP_SIGNATURE_LIST, signatures);
        response.putString(INAPP_CONTINUATION_TOKEN, "");
        return response;
    }

    private static Bundle createSkuDetailsResponseBundle(String... skus) {
        Bundle response = new Bundle();
        ArrayList<String> itemList = new ArrayList<String>(skus.length);
        for (String sku : skus) {
            itemList.add("{ \"productId\": \""+sku+"\" }");
        }
        response.putInt(RESPONSE_CODE, OK.code);
        response.putStringArrayList(RESPONSE_GET_SKU_DETAILS_LIST, itemList);
        return response;
    }

    final static String PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAzoFJ+dq/PQo2u71ndt2k\n" +
            "t0XK3oGFvUPagg0QogBrp2IyBKTodFtmcb0riKtDGjZ9JKB45GIBC3RR2fuC9lOR\n" +
            "15rRjA2Tfxoig0K/VYy7K5+fkLt2yGVDd3oqBFEDSGcwYYP1LfmgI8B2WJjACu3V\n" +
            "ehEQeO/cYrr8tav6VthmqdrL9C+BL9McTMjf3FzeJOTkiGeOFCu58T/sYvSc0ESG\n" +
            "YLh4lXAIG309WvEJ0GofxM4hWnD9aHcuu+hwYblrLJ5jk9hJQJmF7isripkDOQeO\n" +
            "9UbH0kNa9o1pq05beHmGW9a1pt3vWmgBQXZQIOKZzxvmh52d0BJWBgp7NMh68MSx\n" +
            "qwIDAQAB";
}

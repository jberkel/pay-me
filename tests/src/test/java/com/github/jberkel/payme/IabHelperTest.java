package com.github.jberkel.payme;

import android.content.Context;
import android.content.pm.ResolveInfo;
import android.os.IBinder;
import android.os.RemoteException;
import com.android.vending.billing.IInAppBillingService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.res.builder.RobolectricPackageManager;

import static com.github.jberkel.payme.IabHelper.*;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class IabHelperTest {
    @Mock private IInAppBillingService service;
    @Mock private IabHelper.OnIabSetupFinishedListener setupListener;


    private IabHelper helper;
    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        helper = new IabHelper(Robolectric.application, "key") {
            @Override
            protected IInAppBillingService getInAppBillingService(IBinder binder) {
                return service;
            }
        };
    }
    @After public void after() { /* verify(service); */ }

    @Test public void shouldCreateHelper() throws Exception {
        IabHelper helper = new IabHelper(Robolectric.application, "key");
    }

    @Test public void shouldStartSetup_SuccessCase() throws Exception {
        registerServiceWithPackageManager();

        helper.startSetup(setupListener);
        verify(setupListener).onIabSetupFinished(new IabResult(0, "Setup successful."));
        helper.checkSetupDone("testing");
    }

    @Test public void shouldStartSetup_ServiceDoesNotExist() throws Exception {
        helper.startSetup(setupListener);
        verify(setupListener).onIabSetupFinished(new IabResult(
                BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE,
                "Billing service unavailable on device."));
    }

    @Test public void shouldStartSetup_ServiceExistsButDoesNotSupportBilling() throws Exception {
        registerServiceWithPackageManager();
        when(service.isBillingSupported(eq(API_VERSION), anyString(), anyString())).thenReturn(BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE);

        helper.startSetup(setupListener);
        verify(setupListener).onIabSetupFinished(new IabResult(
                BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE,
                "Error checking for billing v3 support."));

        assertThat(helper.subscriptionsSupported()).isFalse();
    }

    @Test public void shouldStartSetup_CheckForSubscriptions_Failure() throws Exception {
        registerServiceWithPackageManager();
        when(service.isBillingSupported(eq(API_VERSION), anyString(), eq(ITEM_TYPE_INAPP))).thenReturn(BILLING_RESPONSE_RESULT_OK);
        when(service.isBillingSupported(eq(API_VERSION), anyString(), eq(ITEM_TYPE_SUBS))).thenReturn(BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE);

        helper.startSetup(setupListener);

        verify(setupListener).onIabSetupFinished(new IabResult(0, "Setup successful."));
        assertThat(helper.subscriptionsSupported()).isFalse();
    }

    @Test public void shouldStartSetup_CheckForSubscriptions_Success() throws Exception {
        registerServiceWithPackageManager();
        when(service.isBillingSupported(eq(API_VERSION), anyString(), eq(ITEM_TYPE_INAPP))).thenReturn(BILLING_RESPONSE_RESULT_OK);
        when(service.isBillingSupported(eq(API_VERSION), anyString(), eq(ITEM_TYPE_SUBS))).thenReturn(BILLING_RESPONSE_RESULT_OK);

        helper.startSetup(setupListener);

        verify(setupListener).onIabSetupFinished(new IabResult(0, "Setup successful."));
        assertThat(helper.subscriptionsSupported()).isTrue();
    }

    @Test public void shouldStartSetup_ServiceExistsButThrowsException() throws Exception {
        registerServiceWithPackageManager();
        when(service.isBillingSupported(eq(API_VERSION), anyString(), anyString())).thenThrow(new RemoteException());

        helper.startSetup(setupListener);
        verify(setupListener).onIabSetupFinished(new IabResult(
                IABHELPER_REMOTE_EXCEPTION,
                "RemoteException while setting up in-app billing."));

        assertThat(helper.subscriptionsSupported()).isFalse();
    }

    private Context registerServiceWithPackageManager() {
        Context context = Robolectric.application;
        RobolectricPackageManager pm = (RobolectricPackageManager) context.getPackageManager();
        pm.addResolveInfoForIntent(IabHelper.BIND_BILLING_SERVICE, new ResolveInfo());
        return context;
    }
}

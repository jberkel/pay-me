package com.github.jberkel.payme;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class IabHelperTest {

    @Test
    public void shouldCreateHelper() throws Exception {
        IabHelper helper = new IabHelper(Robolectric.application, "key");
    }

    @Test
    public void shouldStartSetup() throws Exception {
        IabHelper helper = new IabHelper(Robolectric.application, "key");

        helper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            @Override
            public void onIabSetupFinished(IabResult result) {
            }
        });
    }


}

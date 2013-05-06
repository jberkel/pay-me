package com.github.jberkel.payme;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class)
public class SecurityTest {
    @Test public void shouldVerifyPurchaseNullInput() throws Exception {
        assertThat(Security.verifyPurchase("", null, "")).isFalse();
    }

    @Test public void shouldVerifyPurchaseEmptyInput() throws Exception {
        assertThat(Security.verifyPurchase("", "", "")).isTrue();
    }

    @Test public void shouldVerifyPurchaseNonEmptyInputEmptySignature() throws Exception {
        assertThat(Security.verifyPurchase("", "{}", "")).isTrue();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldVerifyPurchaseWithInvalidSignatureShouldThrow() throws Exception {
        Security.verifyPurchase("", "", "signature");
    }
}

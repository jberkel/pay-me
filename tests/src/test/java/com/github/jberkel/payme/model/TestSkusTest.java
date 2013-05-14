package com.github.jberkel.payme.model;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.github.jberkel.payme.model.TestSkus.*;
import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class)
public class TestSkusTest {

    @Test
    public void shouldHaveRefundedSku() throws Exception {
        assertThat(REFUNDED.getSku()).isEqualTo("android.test.refunded");
        assertThat(REFUNDED.isTestSku()).isTrue();
    }

    @Test
    public void shouldHaveCanceledSku() throws Exception {
        assertThat(CANCELED.getSku()).isEqualTo("android.test.canceled");
        assertThat(CANCELED.isTestSku()).isTrue();
    }

    @Test
    public void shouldHavePurchasedSku() throws Exception {
        assertThat(PURCHASED.getSku()).isEqualTo("android.test.purchased");
        assertThat(PURCHASED.isTestSku()).isTrue();
    }

    @Test
    public void shouldHaveUnavailableSku() throws Exception {
        assertThat(UNAVAILABLE.getSku()).isEqualTo("android.test.item_unavailable");
        assertThat(UNAVAILABLE.isTestSku()).isTrue();
    }
}

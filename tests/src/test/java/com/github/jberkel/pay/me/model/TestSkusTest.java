package com.github.jberkel.pay.me.model;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.github.jberkel.pay.me.model.TestSkus.*;
import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class)
public class TestSkusTest {

    @Test
    public void shouldHaveRefundedSku() throws Exception {
        assertThat(REFUNDED.getSku()).isEqualTo("android.test.refunded");
        assertThat(REFUNDED.isTestSku()).isTrue();
        assertThat(REFUNDED.getType()).isEqualTo(ItemType.INAPP);
    }

    @Test
    public void shouldHaveCanceledSku() throws Exception {
        assertThat(CANCELED.getSku()).isEqualTo("android.test.canceled");
        assertThat(CANCELED.isTestSku()).isTrue();
        assertThat(CANCELED.getType()).isEqualTo(ItemType.INAPP);
    }

    @Test
    public void shouldHavePurchasedSku() throws Exception {
        assertThat(PURCHASED.getSku()).isEqualTo("android.test.purchased");
        assertThat(PURCHASED.isTestSku()).isTrue();
        assertThat(PURCHASED.getType()).isEqualTo(ItemType.INAPP);
    }

    @Test
    public void shouldHaveUnavailableSku() throws Exception {
        assertThat(UNAVAILABLE.getSku()).isEqualTo("android.test.item_unavailable");
        assertThat(UNAVAILABLE.isTestSku()).isTrue();
        assertThat(UNAVAILABLE.getType()).isEqualTo(ItemType.INAPP);
    }
}

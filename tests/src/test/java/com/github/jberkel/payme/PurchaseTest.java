package com.github.jberkel.payme;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class)
public class PurchaseTest {

    @Test
    public void shouldParsePurchase() throws Exception {
        Purchase p = new Purchase(" { \"orderId\": \"123\" } ", "");
        assertThat(p.getOrderId()).isEqualTo("123");
    }
}

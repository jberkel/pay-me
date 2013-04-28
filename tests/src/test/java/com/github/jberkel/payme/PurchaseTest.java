package com.github.jberkel.payme;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.github.jberkel.payme.TestHelper.resourceAsString;
import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class)
public class PurchaseTest {

    @Test
    public void shouldParsePurchase() throws Exception {
        String purchase = resourceAsString("purchase.json");
        Purchase p = new Purchase(purchase, "");
        assertThat(p.getOrderId()).isEqualTo("someOrderId");
    }
}

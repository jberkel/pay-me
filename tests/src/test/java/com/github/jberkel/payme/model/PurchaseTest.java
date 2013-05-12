package com.github.jberkel.payme.model;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.github.jberkel.payme.TestHelper.resourceAsString;
import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class)
public class PurchaseTest {

    @Test public void shouldParsePurchase() throws Exception {
        String purchase = resourceAsString("purchase.json");
        Purchase p = new Purchase(purchase, "signature");

        assertThat(p.getOrderId()).isEqualTo("someOrderId");
        assertThat(p.getPackageName()).isEqualTo("com.example.test");
        assertThat(p.getPurchaseTime()).isEqualTo(12345L);
        assertThat(p.getPurchaseState()).isEqualTo(1);
        assertThat(p.getDeveloperPayload()).isEqualTo("custom");
        assertThat(p.getSignature()).isEqualTo("signature");
        assertThat(p.getOriginalJson()).isEqualTo(purchase);
        assertThat(p.getItemType()).isEqualTo(ItemType.INAPP);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAcceptNullItemType() throws Exception {
        new Purchase(null, "{}", "");
    }

    @Test
    public void shouldHaveToString() throws Exception {
        String purchase = resourceAsString("purchase.json");
        Purchase p = new Purchase(purchase, "signature");
        assertThat(p.toString()).startsWith("Purchase(type:inapp)");
    }
}

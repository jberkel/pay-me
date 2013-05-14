package com.github.jberkel.pay.me.model;


import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.github.jberkel.pay.me.TestHelper.resourceAsString;
import static com.github.jberkel.pay.me.model.ItemType.INAPP;
import static com.github.jberkel.pay.me.model.Purchase.State;
import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class)
public class PurchaseTest {

    @Test public void shouldParsePurchase() throws Exception {
        String purchase = resourceAsString("purchase.json");
        Purchase p = new Purchase(INAPP, purchase, "signature");

        assertThat(p.getOrderId()).isEqualTo("someOrderId");
        assertThat(p.getSku()).isEqualTo("someSKU");
        assertThat(p.getPackageName()).isEqualTo("com.example.test");
        assertThat(p.getPurchaseTime()).isEqualTo(12345L);
        assertThat(p.getRawState()).isEqualTo(1);
        assertThat(p.getDeveloperPayload()).isEqualTo("custom");
        assertThat(p.getSignature()).isEqualTo("signature");
        assertThat(p.getOriginalJson()).isEqualTo(purchase);
        assertThat(p.getItemType()).isEqualTo(INAPP);
        assertThat(p.getState()).isEqualTo(State.CANCELED);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAcceptNullItemType() throws Exception {
        new Purchase(null, "{}", "");
    }

    @Test
    public void shouldHaveToString() throws Exception {
        String purchase = resourceAsString("purchase.json");
        Purchase p = new Purchase(INAPP, purchase, "signature");
        assertThat(p.toString()).startsWith("Purchase(type:inapp)");
    }

    @Test(expected = JSONException.class) public void shouldRequireAnSKU() throws Exception {
        String purchase = resourceAsString("purchase_without_sku.json");
        new Purchase(INAPP, purchase, "");
    }

    @Test
    public void shouldMapPurchaseStateToType() throws Exception {
        assertThat(State.fromCode(0)).isEqualTo(State.PURCHASED);
        assertThat(State.fromCode(1)).isEqualTo(State.CANCELED);
        assertThat(State.fromCode(2)).isEqualTo(State.REFUNDED);
        assertThat(State.fromCode(999)).isEqualTo(State.UNKNOWN);
    }
}

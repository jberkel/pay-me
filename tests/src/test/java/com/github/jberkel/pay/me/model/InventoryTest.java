package com.github.jberkel.pay.me.model;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class InventoryTest {

    @Test
    public void shouldInitialiseEmptyRepository() throws Exception {
        Inventory inventory = new Inventory();
        assertThat(inventory.getAllPurchases()).isEmpty();
        assertThat(inventory.getSkuDetails()).isEmpty();
        assertThat(inventory.getAllOwnedSkus()).isEmpty();
    }

    @Test public void testAddPurchase() throws Exception {
        Inventory inventory = new Inventory();
        inventory.addPurchase(mock(Purchase.class));
        assertThat(inventory.getAllPurchases()).hasSize(1);
    }

    @Test public void testGetPurchase() throws Exception {
        Inventory inventory = new Inventory();
        Purchase p = mock(Purchase.class);
        when(p.getSku()).thenReturn("sku1");
        inventory.addPurchase(p);
        assertThat(inventory.getPurchase("sku1")).isSameAs(p);
    }

    @Test public void testHasPurchase() throws Exception {
        Inventory inventory = new Inventory();
        Purchase p = mock(Purchase.class);
        when(p.getSku()).thenReturn("sku1");
        assertThat(inventory.hasPurchase("sku1")).isFalse();
        inventory.addPurchase(p);
        assertThat(inventory.hasPurchase("sku1")).isTrue();
    }

    @Test public void testAddSkuDetails() throws Exception {
        Inventory inventory = new Inventory();
        SkuDetails details = mock(SkuDetails.class);
        when(details.getSku()).thenReturn("sku1");
        inventory.addSkuDetails(details);
        assertThat(inventory.getSkuDetails()).hasSize(1);
    }

    @Test public void testGetSkuDetails() throws Exception {
        Inventory inventory = new Inventory();
        SkuDetails details = mock(SkuDetails.class);
        when(details.getSku()).thenReturn("sku1");
        assertThat(inventory.hasDetails("sku1")).isFalse();
        inventory.addSkuDetails(details);
        assertThat(inventory.getSkuDetails("sku1")).isSameAs(details);
    }

    @Test public void testHasSkuDetails() throws Exception {
        Inventory inventory = new Inventory();
        SkuDetails details = mock(SkuDetails.class);
        when(details.getSku()).thenReturn("sku1");
        assertThat(inventory.hasDetails("sku1")).isFalse();
        inventory.addSkuDetails(details);
        assertThat(inventory.hasDetails("sku1")).isTrue();
    }

    @Test public void testGetAllOwnedSkus() throws Exception {
        Inventory inventory = new Inventory();
        Purchase p1 = mock(Purchase.class);
        when(p1.getSku()).thenReturn("sku1");

        Purchase p2 = mock(Purchase.class);
        when(p2.getSku()).thenReturn("sku2");

        inventory.addPurchase(p1);
        inventory.addPurchase(p2);

        assertThat(inventory.getAllOwnedSkus()).containsExactly("sku1", "sku2");
    }

    @Test public void testGetAllOwnedSkusForItemType() throws Exception {
        Inventory inventory = new Inventory();
        Purchase p1 = mock(Purchase.class);
        when(p1.getItemType()).thenReturn(ItemType.INAPP);
        when(p1.getSku()).thenReturn("sku_inapp");

        Purchase p2 = mock(Purchase.class);
        when(p2.getItemType()).thenReturn(ItemType.SUBS);
        when(p2.getSku()).thenReturn("sku_sub");

        inventory.addPurchase(p1);
        inventory.addPurchase(p2);

        assertThat(inventory.getAllOwnedSkus(ItemType.SUBS)).containsExactly("sku_sub");
    }

    @Test public void testErasePurchase() throws Exception {
        Inventory inventory = new Inventory();
        Purchase p1 = mock(Purchase.class);
        when(p1.getSku()).thenReturn("sku1");

        Purchase p2 = mock(Purchase.class);
        when(p2.getSku()).thenReturn("sku2");

        inventory.addPurchase(p1);
        inventory.addPurchase(p2);
        assertThat(inventory.getAllOwnedSkus()).containsExactly("sku1", "sku2");
        inventory.erasePurchase("sku1");
        assertThat(inventory.getAllOwnedSkus()).containsExactly("sku2");
    }
}

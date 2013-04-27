package com.github.jberkel.payme;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class)
public class InventoryTest {

    @Test
    public void shouldAddPurchase() throws Exception {

        Inventory inventory = new Inventory();
        inventory.addPurchase(new Purchase(" { } ", ""));

        assertThat(inventory.getAllPurchases()).hasSize(1);
    }
}

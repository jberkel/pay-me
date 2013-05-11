package com.github.jberkel.payme.listener;

import com.github.jberkel.payme.IabResult;
import com.github.jberkel.payme.model.Inventory;

/**
 * Listener that notifies when an inventory query operation completes.
 */
public interface QueryInventoryFinishedListener {
    /**
     * Called to notify that an inventory query operation completed.
     *
     * @param result The result of the operation.
     * @param inv The inventory.
     */
    public void onQueryInventoryFinished(IabResult result, Inventory inv);
}

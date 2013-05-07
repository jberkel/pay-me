package com.github.jberkel.payme;

/**
 * Callback that notifies when a consumption operation finishes.
 */
public interface OnConsumeFinishedListener {
    /**
     * Called to notify that a consumption has finished.
     *
     * @param purchase The purchase that was (or was to be) consumed.
     * @param result The result of the consumption operation.
     */
    public void onConsumeFinished(Purchase purchase, IabResult result);
}

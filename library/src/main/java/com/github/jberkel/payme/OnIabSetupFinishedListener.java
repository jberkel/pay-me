package com.github.jberkel.payme;

/**
 * Callback for setup process. This listener's {@link #onIabSetupFinished} method is called
 * when the setup process is complete.
 */
public interface OnIabSetupFinishedListener {
    /**
     * Called to notify that setup is complete.
     *
     * @param result The result of the setup process.
     */
    public void onIabSetupFinished(IabResult result);
}

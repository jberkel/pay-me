package com.github.jberkel.payme;

import android.os.AsyncTask;
import com.github.jberkel.payme.listener.QueryInventoryFinishedListener;
import com.github.jberkel.payme.model.Inventory;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.github.jberkel.payme.Response.OK;

public class QueryInventoryTask extends AsyncTask<QueryInventoryTask.QueryArgs, Void, Inventory> {
    private final IabHelper mIabHelper;
    private final QueryInventoryFinishedListener mListener;
    private IabResult mResult = new IabResult(OK);

    public QueryInventoryTask(IabHelper iabHelper, @Nullable QueryInventoryFinishedListener listener) {
        mIabHelper = iabHelper;
        mListener = listener;
    }

    @Override
    protected void onPreExecute() {
        mIabHelper.flagStartAsync("refresh inventory");
    }

    @Override
    protected Inventory doInBackground(QueryArgs... args) {
        if (args == null || args.length == 0 || args[0] == null) throw new IllegalArgumentException("need args");
        try {
            return mIabHelper.queryInventory(args[0].queryDetails, args[0].skus);
        } catch (IabException ex) {
            mResult = ex.getResult();
            return null;
        }
    }

    @Override
    protected void onPostExecute(Inventory inventory) {
        mIabHelper.flagEndAsync();
        if (mListener != null && !mIabHelper.isDisposed() && !isCancelled()) {
            mListener.onQueryInventoryFinished(mResult, inventory);
        }
    }

    static class QueryArgs {
        final boolean queryDetails;
        final List<String> skus;
        public QueryArgs(boolean querySkuDetails, List<String> moreSkus) {
            queryDetails = querySkuDetails;
            skus = moreSkus;
        }
    }
}


/* Copyright (c) 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.jberkel.payme.model;

import android.text.TextUtils;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents an in-app billing purchase.
 */
public class Purchase {
    private final ItemType mItemType;
    private final String mOrderId;
    private final String mPackageName;
    private final String mSku;
    private final long mPurchaseTime;
    private final int mPurchaseState;
    private final String mDeveloperPayload;
    private final String mToken;
    private final String mSignature;
    private String mOriginalJson;

    public Purchase(String jsonPurchaseInfo, String signature) throws JSONException {
        this(ItemType.INAPP, jsonPurchaseInfo, signature);
    }

    public Purchase(ItemType itemType, String jsonPurchaseInfo, String signature) throws JSONException {
        if (itemType == null) throw new IllegalArgumentException("itemType cannot be null");

        mItemType = itemType;
        mOriginalJson = jsonPurchaseInfo;
        JSONObject o = new JSONObject(mOriginalJson);
        mOrderId = o.optString("orderId");
        mPackageName = o.optString("packageName");
        mSku = o.optString("productId");
        mPurchaseTime = o.optLong("purchaseTime");
        mPurchaseState = o.optInt("purchaseState");
        mDeveloperPayload = o.optString("developerPayload");
        mToken = o.optString("token", o.optString("purchaseToken"));
        mSignature = signature;

        if (TextUtils.isEmpty(mSku)) {
            throw new JSONException("SKU is empty");
        }
    }

    public ItemType getItemType() { return mItemType; }
    public String getOrderId() { return mOrderId; }
    public String getPackageName() { return mPackageName; }
    public String getSku() { return mSku; }
    public long getPurchaseTime() { return mPurchaseTime; }
    public int getPurchaseState() { return mPurchaseState; }
    public String getDeveloperPayload() { return mDeveloperPayload; }
    public String getToken() { return mToken; }
    public String getOriginalJson() { return mOriginalJson; }
    public String getSignature() { return mSignature; }

    @Override
    public String toString() {
        return "Purchase(type:" + mItemType + "):" + mOriginalJson;
    }
}

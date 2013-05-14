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
 * Represents an in-app product's listing details.
 */
public class SkuDetails {
    private final String mSku;
    private final String mType;
    private final String mPrice;
    private final String mTitle;
    private final String mDescription;

    private String mJson;
    private final ItemType mItemType;

    public SkuDetails(String jsonSkuDetails) throws JSONException {
        mJson = jsonSkuDetails;
        JSONObject json = new JSONObject(mJson);
        mSku = json.optString("productId");
        mType = json.optString("type");
        mPrice = json.optString("price");
        mTitle = json.optString("title");
        mDescription = json.optString("description");

        mItemType = ItemType.fromString(mType);

        if (TextUtils.isEmpty(mSku)) {
            throw new JSONException("SKU cannot be empty");
        }
    }

    // package constructor for TestSkus
    SkuDetails(ItemType itemType,
               String sku,
               String price,
               String title,
               String description) {
        if (itemType == null) throw new IllegalArgumentException("itemType cannot be null");
        if (TextUtils.isEmpty(sku)) {
            throw new IllegalArgumentException("SKU cannot be empty");
        }
        mItemType = itemType;
        mType = itemType.toString();
        mSku = sku;
        mPrice = price;
        mTitle = title;
        mDescription = description;
    }

    /**
     * @return The product ID for the product.
     */
    public String getSku() {
        return mSku;
    }

    /**
     * @return Formatted price of the item, including its currency sign. The price does not include tax.
     */
    public String getPrice() {
        return mPrice;
    }

    /**
     * @return Title of the product.
     */
    public String getTitle() {
        return mTitle;
    }

    /**
     * @return Description of the product.
     */
    public String getDescription() {
        return mDescription;
    }

    /**
     * @return Value must be “inapp” for an in-app product or "subs" for subscriptions.
     */
    public String getRawType() {
        return mType;
    }

    /**
     * @return parsed representation of {@link #getRawType()}.
     */
    public ItemType getType() {
        return mItemType;
    }

    @Override
    public String toString() {
        return "SkuDetails{" +
                "mItemType='" + mItemType + '\'' +
                ", mSku='" + mSku + '\'' +
                ", mType='" + mType + '\'' +
                ", mPrice='" + mPrice + '\'' +
                ", mTitle='" + mTitle + '\'' +
                ", mDescription='" + mDescription + '\'' +
                ", mJson='" + mJson + '\'' +
                '}';
    }

    public boolean isTestSku() {
        return mSku.startsWith(TestSkus.TEST_PREFIX);
    }
}

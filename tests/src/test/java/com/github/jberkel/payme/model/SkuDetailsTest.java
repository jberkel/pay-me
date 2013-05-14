package com.github.jberkel.payme.model;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.github.jberkel.payme.TestHelper.resourceAsString;
import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class)
public class SkuDetailsTest {

    @Test
    public void shouldParseSkuDetails() throws Exception {
        String sku = resourceAsString("sku.json");
        SkuDetails details = new SkuDetails(sku);
        assertThat(details.getSku()).isEqualTo("123");
        assertThat(details.getDescription()).isEqualTo("A great ACME flamethrower");
        assertThat(details.getTitle()).isEqualTo("ACME");
        assertThat(details.getPrice()).isEqualTo("1.99");
        assertThat(details.getRawType()).isEqualTo("inapp");
        assertThat(details.getType()).isEqualTo(ItemType.INAPP);
    }

    @Test
    public void shouldConstructWithoutJson() throws Exception {
        SkuDetails details = new SkuDetails(ItemType.INAPP, "123", "1.99", "ACME", "A great ACME flamethrower");
        assertThat(details.getSku()).isEqualTo("123");
        assertThat(details.getDescription()).isEqualTo("A great ACME flamethrower");
        assertThat(details.getTitle()).isEqualTo("ACME");
        assertThat(details.getPrice()).isEqualTo("1.99");
        assertThat(details.getRawType()).isEqualTo("inapp");
        assertThat(details.getType()).isEqualTo(ItemType.INAPP);
    }

    @Test(expected = JSONException.class)
    public void shouldThrowErrorOnInvalidJson() throws Exception {
        new SkuDetails("");
    }

    @Test(expected = JSONException.class)
    public void shouldNotAcceptNullItemType() throws Exception {
        new SkuDetails("{}");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAcceptNullItemTypeForArgumentConstructor() throws Exception {
        new SkuDetails(null, "123", "1.99", "ACME", "A great ACME flamethrower");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAcceptEmptySKUForArgumentConstructor() throws Exception {
        new SkuDetails(null, "", "1.99", "ACME", "A great ACME flamethrower");
    }

    @Test public void shouldCheckIfTestSku() throws Exception {
        SkuDetails sku = new SkuDetails(ItemType.INAPP, "123", "1.99", "ACME", "A great ACME flamethrower");
        assertThat(sku.isTestSku()).isFalse();

        SkuDetails test = new SkuDetails(ItemType.INAPP, "android.test.foo", "1.99", "ACME", "A great ACME flamethrower");
        assertThat(test.isTestSku()).isTrue();
    }
}

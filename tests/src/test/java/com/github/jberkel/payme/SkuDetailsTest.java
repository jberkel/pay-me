package com.github.jberkel.payme;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class)
public class SkuDetailsTest {

    @Test
    public void shouldParseSkuDetails() throws Exception {
        SkuDetails details = new SkuDetails("{}");
        assertThat(details.getSku()).isEqualTo("");
    }

    @Test(expected = JSONException.class)
    public void shouldThrowErrorOnInvalidJson() throws Exception {
        new SkuDetails("");
    }
}

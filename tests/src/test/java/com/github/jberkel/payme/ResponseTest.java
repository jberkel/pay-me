package com.github.jberkel.payme;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class)
public class ResponseTest {

    @Test
    public void shouldReturnReadableErrorDescriptionForCode() throws Exception {
        assertThat(Response.getDescription(0)).isEqualTo("OK");
        assertThat(Response.getDescription(99999)).isEqualTo("Unknown error");
    }


    @Test
    public void shouldMapCodeToConstant() throws Exception {
        assertThat(Response.fromCode(0)).isEqualTo(Response.BILLING_RESPONSE_RESULT_OK);
        assertThat(Response.fromCode(99999)).isEqualTo(Response.IABHELPER_UNKNOWN_ERROR);
    }
}

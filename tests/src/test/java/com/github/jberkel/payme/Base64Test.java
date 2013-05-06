package com.github.jberkel.payme;

import org.junit.Test;

import static org.fest.assertions.api.Assertions.assertThat;

public class Base64Test {

    @Test public void shouldDecodeBase64() throws Exception {
        assertThat(Base64.decode("dGhpcyBpcyBhIHRlc3Q=\n")).isEqualTo("this is a test".getBytes());
    }

    @Test public void shouldEncodeToBase64() throws Exception {
        assertThat(Base64.encode("this is something else".getBytes())).isEqualTo("dGhpcyBpcyBzb21ldGhpbmcgZWxzZQ==");
    }
}

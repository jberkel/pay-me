package com.github.jberkel.payme;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.security.PublicKey;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class)
public class SecurityTest {
    @Test public void shouldVerifyPurchaseNullInput() throws Exception {
        assertThat(Security.verifyPurchase("", null, "")).isFalse();
    }

    @Test public void shouldVerifyPurchaseEmptyInput() throws Exception {
        assertThat(Security.verifyPurchase("", "", "")).isTrue();
    }

    @Test public void shouldVerifyPurchaseNonEmptyInputEmptySignature() throws Exception {
        assertThat(Security.verifyPurchase("", "{}", "")).isTrue();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldVerifyPurchaseWithInvalidKeyAndSignatureShouldThrow() throws Exception {
        Security.verifyPurchase("", "", "signature");
    }

    public void shouldVerifyPurchaseWithValidKeyAndInvalidSignatureShouldThrow() throws Exception {
        assertThat(Security.verifyPurchase(ENCODED_KEY, "{}", "signature")).isFalse();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotGeneratePublicKeyWithInvalidKey() throws Exception {
        Security.generatePublicKey("invalid!");
    }

    @Test public void shouldGeneratePublicKey() throws Exception {
        PublicKey key = Security.generatePublicKey(ENCODED_KEY);
        assertThat(key.getAlgorithm()).isEqualTo("RSA");
    }

    static final String ENCODED_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAzoFJ+dq/PQo2u71ndt2k\n" +
            "t0XK3oGFvUPagg0QogBrp2IyBKTodFtmcb0riKtDGjZ9JKB45GIBC3RR2fuC9lOR\n" +
            "15rRjA2Tfxoig0K/VYy7K5+fkLt2yGVDd3oqBFEDSGcwYYP1LfmgI8B2WJjACu3V\n" +
            "ehEQeO/cYrr8tav6VthmqdrL9C+BL9McTMjf3FzeJOTkiGeOFCu58T/sYvSc0ESG\n" +
            "YLh4lXAIG309WvEJ0GofxM4hWnD9aHcuu+hwYblrLJ5jk9hJQJmF7isripkDOQeO\n" +
            "9UbH0kNa9o1pq05beHmGW9a1pt3vWmgBQXZQIOKZzxvmh52d0BJWBgp7NMh68MSx\n" +
            "qwIDAQAB";
}

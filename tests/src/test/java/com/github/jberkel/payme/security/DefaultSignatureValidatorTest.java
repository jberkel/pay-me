package com.github.jberkel.payme.security;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class)
public class DefaultSignatureValidatorTest {
    SignatureValidator validator;

    @Before public void before() throws Exception {
        validator = new DefaultSignatureValidator(ENCODED_KEY);
    }

    @Test public void shouldVerifyPurchaseNullInput() throws Exception {
        assertThat(validator.validate(null, "")).isFalse();
    }

    @Test public void shouldVerifyPurchaseEmptyInput() throws Exception {
        assertThat(validator.validate("", "")).isTrue();
    }

    @Test public void shouldVerifyPurchaseNonEmptyInputEmptySignature() throws Exception {
        assertThat(validator.validate("{}", "")).isTrue();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldVerifyPurchaseWithInvalidKeyAndSignatureShouldThrow() throws Exception {
        new DefaultSignatureValidator("").validate("", "signature");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldVerifyPurchaseWithInvalidKeyShouldThrow() throws Exception {
        new DefaultSignatureValidator("INVALIDKEY").validate("", "");
    }

    @Test
    public void shouldVerifyPurchaseWithValidKeyAndInvalidSignatureShouldThrow() throws Exception {
        assertThat(validator.validate("{}", "signature")).isFalse();
    }

    private static final String ENCODED_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAzoFJ+dq/PQo2u71ndt2k\n" +
            "t0XK3oGFvUPagg0QogBrp2IyBKTodFtmcb0riKtDGjZ9JKB45GIBC3RR2fuC9lOR\n" +
            "15rRjA2Tfxoig0K/VYy7K5+fkLt2yGVDd3oqBFEDSGcwYYP1LfmgI8B2WJjACu3V\n" +
            "ehEQeO/cYrr8tav6VthmqdrL9C+BL9McTMjf3FzeJOTkiGeOFCu58T/sYvSc0ESG\n" +
            "YLh4lXAIG309WvEJ0GofxM4hWnD9aHcuu+hwYblrLJ5jk9hJQJmF7isripkDOQeO\n" +
            "9UbH0kNa9o1pq05beHmGW9a1pt3vWmgBQXZQIOKZzxvmh52d0BJWBgp7NMh68MSx\n" +
            "qwIDAQAB";
}

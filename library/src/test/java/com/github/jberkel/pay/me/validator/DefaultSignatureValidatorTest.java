package com.github.jberkel.pay.me.validator;

import android.util.Base64;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

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
        assertThat(validator.validate("", "")).isFalse();
    }

    @Test public void shouldVerifyPurchaseNonEmptyInputEmptySignature() throws Exception {
        assertThat(validator.validate("{}", "")).isFalse();
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

    @Test public void shouldValidateTheSHA1WithRSASignature() throws Exception {
        // create a public/private key pair
        KeyFactory keyFactory   = KeyFactory.getInstance("RSA");
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        KeyPair pair = keyGen.generateKeyPair();
        EncodedKeySpec encoded = new X509EncodedKeySpec(pair.getPublic().getEncoded());
        PublicKey encodedPublic = keyFactory.generatePublic(encoded);
        String encodePublicBase64 = Base64.encodeToString(encodedPublic.getEncoded(), Base64.DEFAULT);

        // and sign some data with it
        Signature sig = Signature.getInstance("SHA1WithRSA");
        sig.initSign(pair.getPrivate());
        String data = "some sample data";
        sig.update(data.getBytes());

        String signature = Base64.encodeToString(sig.sign(), Base64.DEFAULT);

        SignatureValidator validator = new DefaultSignatureValidator(encodePublicBase64);
        assertThat(validator.validate(data, signature)).isTrue();
        assertThat(validator.validate(data+"extraData", signature)).isFalse();
    }

    private static final String ENCODED_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAzoFJ+dq/PQo2u71ndt2k\n" +
            "t0XK3oGFvUPagg0QogBrp2IyBKTodFtmcb0riKtDGjZ9JKB45GIBC3RR2fuC9lOR\n" +
            "15rRjA2Tfxoig0K/VYy7K5+fkLt2yGVDd3oqBFEDSGcwYYP1LfmgI8B2WJjACu3V\n" +
            "ehEQeO/cYrr8tav6VthmqdrL9C+BL9McTMjf3FzeJOTkiGeOFCu58T/sYvSc0ESG\n" +
            "YLh4lXAIG309WvEJ0GofxM4hWnD9aHcuu+hwYblrLJ5jk9hJQJmF7isripkDOQeO\n" +
            "9UbH0kNa9o1pq05beHmGW9a1pt3vWmgBQXZQIOKZzxvmh52d0BJWBgp7NMh68MSx\n" +
            "qwIDAQAB";
}

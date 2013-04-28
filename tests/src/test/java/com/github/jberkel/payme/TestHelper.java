package com.github.jberkel.payme;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import static org.fest.assertions.api.Assertions.assertThat;

public class TestHelper {
    private TestHelper() {
    }

    public static String resourceAsString(String name) throws IOException {
        InputStream is = TestHelper.class.getResourceAsStream("/" + name);
        assertThat(is).isNotNull();
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int n;
            while ((n = is.read(buffer)) != -1) {
                bos.write(buffer, 0, n);
            }
            return new String(bos.toByteArray(), Charset.forName("UTF-8"));
        } finally {
            is.close();
        }
    }
}

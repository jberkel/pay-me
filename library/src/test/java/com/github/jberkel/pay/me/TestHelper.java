package com.github.jberkel.pay.me;

import android.os.Bundle;
import org.hamcrest.Description;
import org.mockito.ArgumentMatcher;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.argThat;

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

    public static class BundleStringArrayListMatcher extends ArgumentMatcher<Bundle> {
        private String key;
        private String[] values;

        public BundleStringArrayListMatcher(String key, String... values) {
            this.key = key;
            this.values = values;
        }

        @Override
        public boolean matches(Object argument) {
            Bundle b = (Bundle) argument;
            List<String> strings = b.getStringArrayList(key);
            return strings != null && strings.equals(Arrays.asList(values));
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(" wanted:"+ Arrays.asList(values));
        }

        public static Bundle bundleWithStringValues(String key, String...values) {
            return argThat(new BundleStringArrayListMatcher(key, values));
        }
    }
}

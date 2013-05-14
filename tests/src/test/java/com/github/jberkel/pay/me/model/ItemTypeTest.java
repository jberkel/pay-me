package com.github.jberkel.pay.me.model;

import org.junit.Test;

import static com.github.jberkel.pay.me.model.ItemType.*;
import static org.fest.assertions.api.Assertions.assertThat;

public class ItemTypeTest {

    @Test public void toStringShouldReturnTheRightConstant() throws Exception {
        assertThat(INAPP.toString()).isEqualTo("inapp");
        assertThat(SUBS.toString()).isEqualTo("subs");
    }

    @Test
    public void shouldMapFromString() throws Exception {
        assertThat(fromString("inapp")).isEqualTo(INAPP);
        assertThat(fromString("subs")).isEqualTo(SUBS);
        assertThat(fromString("foo")).isEqualTo(UNKNOWN);
        assertThat(fromString(null)).isEqualTo(UNKNOWN);
    }
}

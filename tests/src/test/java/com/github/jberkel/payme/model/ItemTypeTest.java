package com.github.jberkel.payme.model;

import org.junit.Test;

import static org.fest.assertions.api.Assertions.assertThat;

public class ItemTypeTest {

    @Test public void toStringShouldReturnTheRightConstant() throws Exception {
        assertThat(ItemType.INAPP.toString()).isEqualTo("inapp");
        assertThat(ItemType.SUBS.toString()).isEqualTo("subs");
    }
}

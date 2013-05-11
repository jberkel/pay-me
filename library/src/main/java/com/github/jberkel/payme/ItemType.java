package com.github.jberkel.payme;

import java.util.Locale;

public enum ItemType {
    INAPP,
    SUBS,
    UNKNOWN;

    public String toString() {
        return name().toLowerCase(Locale.ENGLISH);
    }
}

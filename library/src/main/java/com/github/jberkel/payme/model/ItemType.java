package com.github.jberkel.payme.model;

import java.util.Locale;

public enum ItemType {
    INAPP,
    SUBS,
    UNKNOWN;

    public String toString() {
        return name().toLowerCase(Locale.ENGLISH);
    }
}

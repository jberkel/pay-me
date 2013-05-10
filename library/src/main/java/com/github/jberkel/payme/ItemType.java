package com.github.jberkel.payme;

public enum ItemType {
    INAPP,
    SUBS,
    UNKNOWN;

    public static ItemType fromString(String s) {
        if (INAPP.name().equalsIgnoreCase(s)) {
            return INAPP;
        } else if (SUBS.name().equalsIgnoreCase(s)) {
            return SUBS;
        } else {
            return UNKNOWN;
        }
    }

    public String toString() {
        return name().toLowerCase();
    }
}

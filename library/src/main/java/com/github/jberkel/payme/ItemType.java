package com.github.jberkel.payme;

public enum ItemType {
    INAPP,
    SUBS,
    UNKNOWN;

    public String toString() {
        return name().toLowerCase();
    }
}

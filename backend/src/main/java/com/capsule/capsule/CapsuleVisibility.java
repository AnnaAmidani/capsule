package com.capsule.capsule;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum CapsuleVisibility {
    public_("public"),
    private_("private");

    private final String value;

    CapsuleVisibility(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static CapsuleVisibility fromValue(String value) {
        for (CapsuleVisibility v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown visibility: " + value);
    }
}

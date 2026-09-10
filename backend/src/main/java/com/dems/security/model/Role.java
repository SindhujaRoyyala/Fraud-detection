package com.dems.security.model;

public enum Role {

    ADMIN,
    INVESTIGATOR,
    LEGAL_OFFICER,
    VIEWER;

    public static Role fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Role cannot be null");
        }
        try {
            return Role.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + value + ". Valid roles are: ADMIN, INVESTIGATOR, LEGAL_OFFICER, VIEWER");
        }
    }
}

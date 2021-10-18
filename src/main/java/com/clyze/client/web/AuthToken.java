package com.clyze.client.web;

public class AuthToken {
    /** The user name. */
    private final String userName;
    /** The authentication token (password or API key, depending on server configuration). */
    private final String value;

    public AuthToken(String userName, String value) {
        this.userName = userName;
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public String getUserName() {
        return userName;
    }

    @Override
    public String toString() {
        return "{ user: " + userName + ", auth-value: " + value + " }";
    }
}

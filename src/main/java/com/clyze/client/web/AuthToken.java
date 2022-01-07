package com.clyze.client.web;

public class AuthToken {
    /** The user name. */
    private final String username;
    /** The authentication token (password or API key, depending on server configuration). */
    private final String value;

    public AuthToken(String username, String value) {
        this.username = username;
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public String toString() {
        return "{ user: " + username + ", auth-value: " + value + " }";
    }
}

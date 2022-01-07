package com.clyze.client.web;

/**
 * An authentication token used to perform privileged operations in
 * the server (such as posting code snapshots or running analyses).
 */
public class AuthToken {
    /** The user name. */
    private final String username;
    /** The authentication token (password or API key, depending on server configuration). */
    private final String value;

    /**
     * Create an authentication token.
     * @param username    the name of the authenticated user
     * @param value       the token to use (examples: API key, password)
     */
    public AuthToken(String username, String value) {
        this.username = username;
        this.value = value;
    }

    /**
     * Returns the value of the authentication token (such as
     * a password or an API key).
     * @return the token value
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns the name of the authenticated user.
     * @return the user name
     */
    public String getUsername() {
        return username;
    }

    /**
     * Return a string representation of the token, used for debugging.
     * @return a string describing this token
     */
    @Override
    public String toString() {
        return "{ user: " + username + ", auth-value: " + value + " }";
    }
}

package com.clyze.client.web;

import java.util.List;

/**
 * The options that drive the interaction with the server.
 */
public class PostOptions {
    /** The server (such as www.service.com:8080/abc). */
    public String host;
    /** The authentication token. */
    public AuthToken authToken;
    /** The user that owns the project. */
    public String owner;
    /** The project name to use. */
    public String project;
    /** The technology stacks to use. */
    public List<String> stacks;
    /** If true, skip last step (actual posting to the server). */
    public boolean dry;
    /** If true, Android should be supported by the server. */
    public boolean android;
    /** If true, automated repackaging should be supported by the server. */
    public boolean autoRepackaging;
}

package com.clyze.client.web;

import java.util.List;

/**
 * The options that drive the interaction with the server.
 */
public class PostOptions {
    public String host;
    public int port;
    public String username;
    public String password;
    public String project;
    public List<String> stacks;
    public boolean dry;
    /** true if Android should be supported by the server */
    public boolean android;
    /** true if automated repackaging should be supported by the server */
    public boolean autoRepackaging;
}

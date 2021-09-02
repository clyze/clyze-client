package com.clyze.client.web;

import java.util.List;

/**
 * The options that drive the interaction with the server.
 */
public class PostOptions {
    public String host;
    public int port;
    public String basePath;
    public String username;
    public String password;
    public String project;
    public List<String> stacks;
    public boolean dry;
    /** true if Android should be supported by the server */
    public boolean android;
    /** true if automated repackaging should be supported by the server */
    public boolean autoRepackaging;

    /**
     * Return the host prefix of the server.
     * @return  the host prefix (e.g. www.service.com:8080/abc)
     */
    public String getHostPrefix() {
        return host + ":" + port + basePath;
    }
}

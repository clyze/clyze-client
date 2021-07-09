package com.clyze.client;

/**
 * A printer of messages from the core library, so that they can be fed to
 * a logger or output to the console.
 */
public interface Printer {
    void error(String message);
    void warn(String message);
    void debug(String message);
    void info(String message);
    void always(String message);
}

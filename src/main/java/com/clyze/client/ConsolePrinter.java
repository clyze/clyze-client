package com.clyze.client;

/**
 * A simple console-based implementation of the Printer interface.
 */
public class ConsolePrinter implements Printer {
    private final boolean debug;

    public ConsolePrinter(boolean debug) {
        this.debug = debug;
    }

    @Override
    public void error(String message) {
        System.err.println(message);
    }

    @Override
    public void warn(String message) {
        System.err.println(message);
    }

    @Override
    public void debug(String message) {
        if (debug)
            System.err.println(message);
    }

    @Override
    public void info(String message) {
        System.out.println(message);
    }

    @Override
    public void always(String message) {
        System.out.println(message);
    }
}

package net.immute.ccs;

public abstract class CcsLogger {
    public static void warn(String msg) {
        System.err.println("WARN: " + msg);
    }

    public static void error(String msg) {
        System.err.println("ERROR: " + msg);
    }
}

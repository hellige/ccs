package net.immute.ccs;

public abstract class CCSLogger {
    public static void debug(String msg) {
        System.err.println("DEBUG: " + msg);
    }

    public static void info(String msg) {
        System.err.println("INFO: " + msg);
    }

    public static void warn(String msg) {
        System.err.println("WARN: " + msg);
    }

    public static void error(String msg) {
        System.err.println("ERROR: " + msg);
    }

    public static void internalError(String msg) {
        System.err.println("INTERNAL: " + msg);
    }

    /*
     * static { checkConfiguration(); } private static void checkConfiguration() {
     * if (!Category.getRoot().getAllAppenders().hasMoreElements()) { // we
     * initialize logging only if our category has no appenders.
     * BasicConfigurator.resetConfiguration(); BasicConfigurator.configure();
     * _category.info("Initializing ICE logger to standard output."); } }
     */
}

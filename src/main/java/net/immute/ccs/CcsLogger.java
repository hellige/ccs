package net.immute.ccs;

public interface CcsLogger {
    void info(String s);
    void warn(String msg);
    void error(String msg);
    void error(String msg, Throwable e);

    public static class StderrCcsLogger implements CcsLogger {
        @Override
        public void warn(String msg) {
            System.err.println("WARN: " + msg);
        }

        @Override
        public void info(String msg) {
            System.err.println("INFO: " + msg);
        }

        @Override public void error(String msg) {
            System.err.println("ERROR: " + msg);
        }

        @Override
        public void error(String msg, Throwable e) {
            System.err.println("ERROR: " + msg);
            e.printStackTrace();
        }
    }
}

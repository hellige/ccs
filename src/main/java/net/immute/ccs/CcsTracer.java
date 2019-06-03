package net.immute.ccs;

import java.util.Collection;
import java.util.SortedSet;

public interface CcsTracer {
    void onPropertyFound(CcsContext ccsContext, String propertyName, CcsProperty prop);
    void onPropertyNotFound(CcsContext ccsContext, String propertyName);
    void onConflict(CcsContext ccsContext, String propertyName, SortedSet<CcsProperty> prop);
    void onParseError(String msg);
    void onParseError(String msg, Throwable t);

    class Logging implements CcsTracer {
        private final CcsLogger log;
        private final boolean logAccesses;

        public Logging(CcsLogger log, boolean logAccesses) {
            this.log = log;
            this.logAccesses = logAccesses;
        }

        @Override
        public void onPropertyFound(CcsContext ccsContext, String propertyName, CcsProperty prop) {
            if (logAccesses) {
                StringBuilder msg = new StringBuilder();
                msg.append("Found property: " + propertyName
                                   + " = " + prop.getValue() + "\n");
                msg.append("    at " + prop.getOrigin() + " in context: [" + ccsContext.toString() + "]");
                log.info(msg.toString());
            }
        }

        @Override
        public void onPropertyNotFound(CcsContext ccsContext, String propertyName) {
            if (logAccesses) {
                StringBuilder msg = new StringBuilder();
                msg.append("Property not found: " + propertyName + "\n");
                msg.append("    in context: [" + ccsContext.toString() + "]");
                log.info(msg.toString());
            }
        }

        private String origins(Collection<CcsProperty> values) {
            StringBuilder b = new StringBuilder();
            boolean first = true;
            for (CcsProperty v : values) {
                if (!first) b.append(", ");
                b.append(v.getOrigin());
                first = false;
            }
            return b.toString();
        }

        @Override
        public void onConflict(CcsContext ccsContext, String propertyName, SortedSet<CcsProperty> values) {
            String msg = ("Conflict detected for property '" + propertyName
                    + "' in context [" + ccsContext.toString() + "]. "
                    + "(Conflicting settings at: [" + origins(values)
                    + "].) Using most recent value.");
            log.warn(msg);
        }

        @Override
        public void onParseError(String msg) {
            log.error(msg);
        }

        @Override
        public void onParseError(String msg, Throwable t) {
            log.error(msg, t);
        }
    }
}

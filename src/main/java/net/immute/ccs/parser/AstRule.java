package net.immute.ccs.parser;

import net.immute.ccs.CcsLogger;
import net.immute.ccs.CcsProperty;
import net.immute.ccs.Origin;
import net.immute.ccs.dag.Node;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public interface AstRule {
    void addTo(Node node);
    boolean resolveImports(List<AstRule> results, ImportResolver importResolver, Loader loader);

    public static class Import implements AstRule {
        private final String location;

        public Import(String location) {
            this.location = location;
        }

        @Override public void addTo(Node node) {}

        @Override
        public boolean resolveImports(List<AstRule> results, ImportResolver importResolver, Loader loader) {
            try {
                // TODO add context...
                if (!loader.parseCcsStream(results, importResolver.resolve(location), location, importResolver))
                    return false;
            } catch (IOException e) {
                CcsLogger.error(String.format("Error loading imported document '%s': %s", location, e.toString()));
                // TODO maybe a stack trace or something too?
                return false;
            }
            return true;
        }
    }

    public static class PropDef implements AstRule {
        private final String name;
        private final Value<?> value;
        private final Origin origin;

        public PropDef(String name, Value<?> value, Origin origin) {
            this.name = name;
            this.value = value;
            this.origin = origin;
        }

        @Override
        public void addTo(Node node) {
            node.addProperty(name, new CcsProperty(value.toString(), origin, 0), true); // TODO property number
        }

        @Override
        public boolean resolveImports(List<AstRule> results, ImportResolver importResolver, Loader loader) {
            return true;
        }
    }

    public static class Nested implements AstRule {
        private final List<AstRule> rules = new ArrayList<AstRule>();
        private Selector selector;

        public Nested(Selector selector) {
            this.selector = selector;
        }

        // these return boolean for ease of use with parboiled...
        boolean setSelector(Selector selector) {
            this.selector = selector;
            return true;
        }

        boolean append(AstRule rule) {
            rules.add(rule);
            return true;
        }

        @Override
        public void addTo(Node node) {
            Node next = selector == null ? node : selector.traverse(node);
            for (AstRule rule : rules) rule.addTo(next);
        }

        @Override
        public boolean resolveImports(List<AstRule> results, ImportResolver importResolver, Loader loader) {
            // TODO add context...
            for (AstRule rule : rules)
                if (!rule.resolveImports(results, importResolver, loader)) return false;
            return true;
        }
    }
}

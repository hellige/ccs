package net.immute.ccs.parser;

import net.immute.ccs.CcsProperty;
import net.immute.ccs.Origin;
import net.immute.ccs.dag.Node;

import java.util.List;

public interface AstRule {
    void addTo(Node root);

    public static class Import implements AstRule {
        private final String location;

        public Import(String location) {
            this.location = location;
        }

        @Override
        public void addTo(Node node) {
            // TODO...
            System.out.println("Adding imported rules: " + location);
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
            node.addProperty(name, new CcsProperty(value.toString(), origin, 0), true);
        }
    }

    public static class Nested implements AstRule {
        private final Selector selector;
        private final List<AstRule> rules;

        public Nested(Selector selector, List<AstRule> rules) {
            this.selector = selector;
            this.rules = rules;
        }

        @Override
        public void addTo(Node node) {
            Node next = selector.traverse(node);
            for (AstRule rule : rules) rule.addTo(next);
        }
    }
}

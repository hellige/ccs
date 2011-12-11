package net.immute.ccs.parser;

import java.util.List;

public interface AstRule {
    public static class Import implements AstRule {
        private final String location;

        public Import(String location) {
            this.location = location;
        }
    }

    public static class PropDef implements AstRule {
        private final String name;
        private final Value<?> value;

        public PropDef(String name, Value<?> value) {
            this.name = name;
            this.value = value;
        }
    }

    public static class Nested implements AstRule {
        private final Selector selector;
        private final List<AstRule> rules;

        public Nested(Selector selector, List<AstRule> rules) {
            this.selector = selector;
            this.rules = rules;
        }
    }
}

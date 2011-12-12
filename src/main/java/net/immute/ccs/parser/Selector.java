package net.immute.ccs.parser;

import net.immute.ccs.tree.Key;

public abstract class Selector {
    Selector conjoin(Selector next) {
        return new Conjunction(this, next); // TODO override to optimize...
    }
    Selector disjoin(Selector next) {
        return new Disjunction(this, next); // TODO override to optimize...
    }

    public static class Conjunction extends Selector {
        private final Selector first;
        private final Selector second;

        public Conjunction(Selector first, Selector second) {
            this.first = first;
            this.second = second;
        }
    }

    public static class Disjunction extends Selector {
        private final Selector first;
        private final Selector second;

        public Disjunction(Selector first, Selector second) {
            this.first = first;
            this.second = second;
        }
    }
    
    public static class Child extends Selector {
        private final Selector parent;
        private final Selector child;

        public Child(Selector parent, Selector child) {
            this.parent = parent;
            this.child = child;
        }
    }
    
    public static class Descendant extends Selector {
        private final Selector parent;
        private final Selector desc;

        public Descendant(Selector parent, Selector desc) {
            this.parent = parent;
            this.desc = desc;
        }
    }
    
    public static class Step extends Selector {
        private final Key key;

        public Step(Key key) {
            this.key = key;
        }
    }
}

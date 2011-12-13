package net.immute.ccs.parser;

import net.immute.ccs.dag.Node;
import net.immute.ccs.tree.Key;

public abstract class Selector {
    public abstract Node traverse(Node node);
    public abstract Selector asDirectChild();

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

        @Override public Node traverse(Node node) {
            throw new UnsupportedOperationException(); // TODO
        }

        @Override public Selector asDirectChild() {
            return new Conjunction(first.asDirectChild(), second.asDirectChild());
        }
    }

    public static class Disjunction extends Selector {
        private final Selector first;
        private final Selector second;

        public Disjunction(Selector first, Selector second) {
            this.first = first;
            this.second = second;
        }

        @Override public Node traverse(Node node) {
            throw new UnsupportedOperationException(); // TODO
        }

        @Override public Selector asDirectChild() {
            return new Disjunction(first.asDirectChild(), second.asDirectChild());
        }
    }

    public static class Descendant extends Selector {
        private final Selector parent;
        private final Selector desc;

        public Descendant(Selector parent, Selector desc) {
            this.parent = parent;
            this.desc = desc;
        }

        @Override public Node traverse(Node node) {
            return desc.traverse(parent.traverse(node));
        }

        @Override public Selector asDirectChild() {
            return new Descendant(parent, desc.asDirectChild());
        }
    }

    public static class Step extends Selector {
        private final Key key;

        public Step(Key key) {
            this.key = key;
        }

        @Override public Node traverse(Node node) {
            Node tmpNode = node.getChild(key);
            if (tmpNode == null) {
                tmpNode = new Node();
                node.addChild(key, tmpNode);
            }
            return tmpNode;
        }

        @Override public Selector asDirectChild() {
            // TODO sure would be nicer if Key was immutable, or at least easily copied...
            key.setDirectChild(true);
            return this;
        }
    }
}

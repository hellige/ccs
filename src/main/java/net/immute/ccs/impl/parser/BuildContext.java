package net.immute.ccs.impl.parser;

import net.immute.ccs.CcsProperty;
import net.immute.ccs.Origin;
import net.immute.ccs.impl.dag.*;

public abstract class BuildContext {
    protected final DagBuilder dag;

    abstract public Node traverse(SelectorLeaf selector);
    abstract public Node getNode();

    public BuildContext(DagBuilder dag) {
        this.dag = dag;
    }

    void addProperty(String name, Value value, Origin origin, boolean override) {
        getNode().addProperty(name, new CcsProperty(value.toString(), origin, dag.nextProperty(), override));
    }

    public void addConstraint(Key key) {
        getNode().addConstraint(key);
    }

    // this one's actually a separate class since it's the root. we need to start someplace after all!
    public static class Descendant extends BuildContext {
        private final Dag dag;

        public Descendant(DagBuilder builder, Dag dag) {
            super(builder);
            this.dag = dag;
        }

        @Override public Node traverse(SelectorLeaf selector) {
            return selector.traverse(this);
        }

        @Override public Node getNode() {
            return dag.getRootSettings();
        }
    }

    private static abstract class TallyBuildContext extends BuildContext {
        private final Node firstNode;
        private final Class<?> tallyClass;

        abstract protected Tally newTally(Node firstNode, Node secondNode);

        public TallyBuildContext(DagBuilder builder, Node node, Class<?> tallyClass) {
            super(builder);
            this.firstNode = node;
            this.tallyClass = tallyClass;
        }

        @Override
        public Node traverse(SelectorLeaf selector) {
            Node secondNode = selector.traverse(dag.getBuildContext());

            for (Tally tally : firstNode.getTallies())
                if (tallyClass.isInstance(tally))
                    for (int i = 0; i < tally.getSize(); i++)
                        if (tally.getLeg(i) == secondNode)
                            return tally.getNode();

            // doesn't exist yet... we need to create it
            Tally tally = newTally(firstNode, secondNode);
            firstNode.addTally(tally);
            secondNode.addTally(tally);
            return tally.getNode();
        }

        @Override
        public Node getNode() {
            return firstNode;
        }
    }

    public BuildContext disjunction(final Node node) {
        return new TallyBuildContext(dag, node, Tally.OrTally.class) {
            @Override
            protected Tally.OrTally newTally(Node firstNode, Node secondNode) {
                return new Tally.OrTally(new Node(), new Node[] {firstNode, secondNode});
            }
        };
    }

    public BuildContext conjunction(final Node node) {
        return new TallyBuildContext(dag, node, Tally.AndTally.class) {
            @Override
            protected Tally.AndTally newTally(Node firstNode, Node secondNode) {
                return new Tally.AndTally(new Node(), new Node[] {firstNode, secondNode});
            }
        };
    }
}

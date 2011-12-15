package net.immute.ccs.parser;

import net.immute.ccs.CcsProperty;
import net.immute.ccs.Origin;
import net.immute.ccs.dag.Dag;
import net.immute.ccs.dag.Node;
import net.immute.ccs.dag.Tally;

import java.util.HashSet;
import java.util.Set;

public abstract class BuildContext {
    protected final Dag dag;

    abstract public Node traverse(SelectorLeaf selector);
    abstract public Node getNode();

    public BuildContext(Dag dag) {
        this.dag = dag;
    }

    void addProperty(String name, Value value, Origin origin, boolean local) {
        getNode().addProperty(name, new CcsProperty(value.toString(), origin, dag.nextProperty()), local);
    }

    public BuildContext descendant(Node node) {
        return new Descendant(dag, node);
    }

    // this one's actually a separate class since it's the root. we need to start someplace after all!
    public static class Descendant extends BuildContext {
        private final Node node;

        public Descendant(Dag dag, Node node) {
            super(dag);
            this.node = node;
        }

        @Override public Node traverse(SelectorLeaf selector) {
            return selector.traverse(this, this);  // TODO i think this is wrong
        }

        @Override public Node getNode() {
            return node;
        }
    }

    public BuildContext disjunction(final Node node, final BuildContext baseContext) {
        return new BuildContext(dag) {
            @Override public Node traverse(SelectorLeaf selector) {
                // TODO kill this duplication...
                Node firstNode = node;
                Node secondNode = selector.traverse(baseContext, baseContext);  // TODO i think this is wrong
                Set<Tally> tallies = new HashSet<Tally>();
                tallies.addAll(firstNode.getTallies());
                tallies.retainAll(secondNode.getTallies());
                assert tallies.isEmpty() || tallies.size() == 1;
                if (tallies.isEmpty()) {
                    Tally tally = new Tally.OrTally(new Node(), new Node[] {firstNode, secondNode});
                    firstNode.addTally(tally);
                    secondNode.addTally(tally);
                    return tally.getNode();
                } else {
                    return tallies.iterator().next().getNode();
                }
            }

            @Override public Node getNode() {
                return node;
            }
        };
    }

    public BuildContext conjunction(final Node node, final BuildContext baseContext) {
        return new BuildContext(dag) {
            @Override public Node traverse(SelectorLeaf selector) {
                Node firstNode = node;
                Node secondNode = selector.traverse(baseContext, baseContext);  // TODO i think this is wrong
                Set<Tally> tallies = new HashSet<Tally>();
                tallies.addAll(firstNode.getTallies());
                tallies.retainAll(secondNode.getTallies());
                assert tallies.isEmpty() || tallies.size() == 1;
                if (tallies.isEmpty()) {
                    Tally tally = new Tally.AndTally(new Node(), new Node[] {firstNode, secondNode});
                    firstNode.addTally(tally);
                    secondNode.addTally(tally);
                    return tally.getNode();
                } else {
                    return tallies.iterator().next().getNode();
                }
            }

            @Override public Node getNode() {
                return node;
            }
        };
    }
}

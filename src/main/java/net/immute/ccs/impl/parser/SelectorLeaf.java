package net.immute.ccs.impl.parser;

import net.immute.ccs.impl.dag.DagBuilder;
import net.immute.ccs.impl.dag.Key;
import net.immute.ccs.impl.dag.Node;

import java.util.ArrayList;
import java.util.List;

abstract class SelectorLeaf {
    abstract Node traverse(DagBuilder dag);
    abstract SelectorLeaf descendant(SelectorLeaf right);
    abstract SelectorLeaf conjunction(SelectorLeaf right);
    abstract SelectorLeaf disjunction(SelectorLeaf right);

    static SelectorLeaf step(final Key key) {
        return new SelectorLeaf() {
            @Override public Node traverse(DagBuilder dag) {
                return dag.findOrCreateNode(key);
            }

            @Override public SelectorLeaf descendant(SelectorLeaf right) {
                throw new UnsupportedOperationException(); // TODO
//                return SelectorLeaf.wrap(SelectorBranch.descendant(this), right);
            }

            @Override public SelectorLeaf conjunction(SelectorLeaf right) {
                return SelectorLeaf.wrap(SelectorBranch.conjunction(this), right);
            }

            @Override public SelectorLeaf disjunction(SelectorLeaf right) {
                return SelectorLeaf.wrap(SelectorBranch.disjunction(this), right);
            }
        };
    }

    private static SelectorLeaf wrap(final SelectorBranch selector, final SelectorLeaf leaf) {
        return new SelectorLeaf() {
            private final List<SelectorBranch> branches = new ArrayList<SelectorBranch>() {{ add(selector); }};
            private SelectorLeaf right = leaf;

            @Override public Node traverse(DagBuilder dag) {
                BuildContext tmp = dag.getBuildContext();
                for (SelectorBranch branch : branches) tmp = branch.traverse(tmp);
                return tmp.traverse(right);
            }

            private SelectorLeaf push(SelectorBranch newBranch, SelectorLeaf newRight) {
                branches.add(newBranch);
                this.right = newRight;
                return this;
            }

            @Override public SelectorLeaf descendant(SelectorLeaf right) {
                throw new UnsupportedOperationException(); // TODO
//                return push(SelectorBranch.descendant(this.right), right);
            }

            @Override public SelectorLeaf conjunction(SelectorLeaf right) {
                return push(SelectorBranch.conjunction(this.right), right);
            }

            @Override public SelectorLeaf disjunction(SelectorLeaf right) {
                return push(SelectorBranch.disjunction(this.right), right);
            }
        };
    }
}

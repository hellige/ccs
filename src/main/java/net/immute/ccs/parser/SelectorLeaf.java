package net.immute.ccs.parser;

import net.immute.ccs.dag.Key;
import net.immute.ccs.dag.Node;

import java.util.ArrayList;
import java.util.List;

public abstract class SelectorLeaf {
    abstract Node traverse(BuildContext context);
    abstract SelectorLeaf descendant(SelectorLeaf pop);
    abstract SelectorLeaf conjunction(SelectorLeaf pop);
    abstract SelectorLeaf disjunction(SelectorLeaf pop);

    public static SelectorLeaf step(final Key key) {
        return new SelectorLeaf() {
            @Override public Node traverse(BuildContext context) {
                Node node = context.getNode();
                Node tmpNode = node.getChild(key);
                if (tmpNode == null) {
                    tmpNode = new Node();
                    node.addChild(key, tmpNode);
                }
                return tmpNode;
            }

            @Override public SelectorLeaf descendant(SelectorLeaf right) {
                return SelectorLeaf.wrap(SelectorBranch.descendant(this), right);
            }

            @Override public SelectorLeaf conjunction(SelectorLeaf right) {
                return SelectorLeaf.wrap(SelectorBranch.conjunction(this), right);
            }

            @Override public SelectorLeaf disjunction(SelectorLeaf right) {
                return SelectorLeaf.wrap(SelectorBranch.disjunction(this), right);
            }
        };
    }

    public static SelectorLeaf wrap(final SelectorBranch selector, final SelectorLeaf leaf) {
        return new SelectorLeaf() {
            private final List<SelectorBranch> branches = new ArrayList<SelectorBranch>() {{ add(selector); }};
            private SelectorLeaf right = leaf;

            @Override public Node traverse(BuildContext context) {
                BuildContext tmp = context;
                for (SelectorBranch branch : branches) tmp = branch.traverse(tmp, context);
                return tmp.traverse(right);
            }

            private SelectorLeaf push(SelectorBranch newBranch, SelectorLeaf newRight) {
                branches.add(newBranch);
                this.right = newRight;
                return this;
            }

            @Override public SelectorLeaf descendant(SelectorLeaf right) {
                return push(SelectorBranch.descendant(this.right), right);
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

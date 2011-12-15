package net.immute.ccs.parser;

import net.immute.ccs.dag.Key;
import net.immute.ccs.dag.Node;

import java.util.ArrayList;
import java.util.List;

public interface SelectorLeaf {
    Node traverse(BuildContext context, BuildContext baseContext);
    SelectorLeaf asDirectChild();
    SelectorLeaf descendant(SelectorLeaf pop);
    SelectorLeaf conjunction(SelectorLeaf pop);
    SelectorLeaf disjunction(SelectorLeaf pop);

    public static class Step implements SelectorLeaf {
        private final Key key;

        public Step(Key key) {
            this.key = key;
        }

        @Override public Node traverse(BuildContext context, BuildContext _) {
            Node node = context.getNode();
            Node tmpNode = node.getChild(key);
            if (tmpNode == null) {
                tmpNode = new Node();
                node.addChild(key, tmpNode);
            }
            return tmpNode;
        }

        @Override public SelectorLeaf asDirectChild() {
            // TODO sure would be nicer if Key was immutable, or at least easily copied...
            key.setDirectChild(true);
            return this;
        }

        @Override public SelectorLeaf descendant(SelectorLeaf right) {
            return new SelectorLeaf.Wrapped(new SelectorBranch.Descendant(this), right);
        }

        @Override public SelectorLeaf conjunction(SelectorLeaf right) {
            return new SelectorLeaf.Wrapped(new SelectorBranch.Conjunction(this), right);
        }

        @Override public SelectorLeaf disjunction(SelectorLeaf right) {
            return new SelectorLeaf.Wrapped(new SelectorBranch.Disjunction(this), right);
        }
    }

    public static class Wrapped implements SelectorLeaf {
        private final List<SelectorBranch> branches = new ArrayList<SelectorBranch>();
        private SelectorLeaf right;

        public Wrapped(SelectorBranch selector, SelectorLeaf right) {
            branches.add(selector);
            this.right = right;
        }

        @Override public Node traverse(BuildContext context, BuildContext baseContext) {
            for (SelectorBranch branch : branches) context = branch.traverse(context, baseContext);
            return right.traverse(context, context); // NB: this is where baseContext changes...
        }

        @Override public SelectorLeaf asDirectChild() {
            return new Wrapped(branches.get(0).asDirectChild(), right);
        }

        @Override public SelectorLeaf descendant(SelectorLeaf right) {
            branches.add(new SelectorBranch.Descendant(this.right));
            this.right = right;
            return this;
        }

        @Override public SelectorLeaf conjunction(SelectorLeaf right) {
            branches.add(new SelectorBranch.Descendant(this.right));
            this.right = right;
            return this;
        }

        @Override public SelectorLeaf disjunction(SelectorLeaf right) {
            branches.add(new SelectorBranch.Descendant(this.right));
            this.right = right;
            return this;
        }
    }
}

package net.immute.ccs.parser;

public abstract class SelectorBranch {
    public abstract BuildContext traverse(BuildContext context, BuildContext baseContext);
    public abstract SelectorBranch asDirectChild();

    public static class Conjunction extends SelectorBranch {
        private final SelectorLeaf first;

        public Conjunction(SelectorLeaf first) {
            this.first = first;
        }

        @Override public BuildContext traverse(BuildContext context, BuildContext baseContext) {
            return context.conjunction(context.traverse(first), baseContext);
        }

        @Override public SelectorBranch asDirectChild() {
            return new Conjunction(first.asDirectChild());
        }
    }

    public static class Disjunction extends SelectorBranch {
        private final SelectorLeaf first;

        public Disjunction(SelectorLeaf first) {
            this.first = first;
        }

        @Override public BuildContext traverse(BuildContext context, BuildContext baseContext) {
            return context.disjunction(context.traverse(first), baseContext);
        }

        @Override public SelectorBranch asDirectChild() {
            return new Disjunction(first.asDirectChild());
        }
    }

    public static class Descendant extends SelectorBranch {
        private final SelectorLeaf parent;

        public Descendant(SelectorLeaf parent) {
            this.parent = parent;
        }

        @Override public BuildContext traverse(BuildContext context, BuildContext baseContext) {
            return context.descendant(context.traverse(parent));
        }

        @Override public SelectorBranch asDirectChild() {
            return new Descendant(parent.asDirectChild());
        }
    }
}

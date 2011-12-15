package net.immute.ccs.parser;

public abstract class SelectorBranch {
    public abstract BuildContext traverse(BuildContext context, BuildContext baseContext);

    public static SelectorBranch conjunction(final SelectorLeaf first) {
        return new SelectorBranch() {
            @Override public BuildContext traverse(BuildContext context, BuildContext baseContext) {
                return context.conjunction(context.traverse(first), baseContext);
            }
        };
    }

    public static SelectorBranch disjunction(final SelectorLeaf first) {
        return new SelectorBranch() {
            @Override public BuildContext traverse(BuildContext context, BuildContext baseContext) {
                return context.disjunction(context.traverse(first), baseContext);
            }
        };
    }

    public static SelectorBranch descendant(final SelectorLeaf first) {
        return new SelectorBranch() {
            @Override public BuildContext traverse(BuildContext context, BuildContext baseContext) {
                return context.descendant(context.traverse(first));
            }
        };
    }
}

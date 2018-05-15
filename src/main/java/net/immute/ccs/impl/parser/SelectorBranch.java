package net.immute.ccs.impl.parser;

import net.immute.ccs.impl.dag.Dag;

public abstract class SelectorBranch {
    public abstract BuildContext traverse(BuildContext context);

    public static SelectorBranch conjunction(final SelectorLeaf first) {
        return new SelectorBranch() {
            @Override public BuildContext traverse(BuildContext context) {
                return context.conjunction(context.traverse(first));
            }
        };
    }

    public static SelectorBranch disjunction(final SelectorLeaf first) {
        return new SelectorBranch() {
            @Override public BuildContext traverse(BuildContext context) {
                return context.disjunction(context.traverse(first));
            }
        };
    }
}

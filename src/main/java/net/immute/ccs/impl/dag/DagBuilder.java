package net.immute.ccs.impl.dag;

import net.immute.ccs.impl.parser.BuildContext;

public class DagBuilder {
    private final Dag root = new Dag();
    private final BuildContext buildContext = new BuildContext.Descendant(this, root);

    private int nextProperty = 0;

    public int nextProperty() {
        return nextProperty++;
    }

    public Dag getRoot() {
        return root;
    }

    public BuildContext getBuildContext() {
        return buildContext;
    }
}

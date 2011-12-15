package net.immute.ccs.dag;

import net.immute.ccs.parser.BuildContext;

public class DagBuilder {
    private final Node root = new Node();
    private final BuildContext buildContext = new BuildContext.Descendant(this, root);

    private int nextProperty = 0;

    public int nextProperty() {
        return nextProperty++;
    }

    public Node getRoot() {
        return root;
    }

    public BuildContext getBuildContext() {
        return buildContext;
    }
}

package net.immute.ccs.impl.dag;

import net.immute.ccs.impl.parser.BuildContext;

import java.util.HashMap;

public class DagBuilder extends Dag {
    private final BuildContext buildContext = new BuildContext.Descendant(this);

    private int nextProperty = 0;

    public int nextProperty() {
        return nextProperty++;
    }

    public BuildContext getBuildContext() {
        return buildContext;
    }

    public Node getRootSettings() {
        return rootSettings;
    }

    public Node findOrCreateNode(Key key) {
        return children
                .computeIfAbsent(key.getName(), k -> new HashMap<>())
                .computeIfAbsent(key.getValue(), v -> new Node());
    }

    public Dag build() {
        return new Dag(children, rootSettings);
    }
}

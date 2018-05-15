package net.immute.ccs.impl.dag;

import net.immute.ccs.impl.SearchState;

import java.util.HashMap;
import java.util.Map;

public class Dag {
    private final Map<String, Map<String ,Node>> children = new HashMap<>();
    private final Node rootSettings = new Node();

    public void traverse(Key key, SearchState.Builder builder) {
        Map<String, Node> nodes = children.get(key.getName());
        for (Map.Entry<String, Node> entry : nodes.entrySet()) {
            if (entry.getKey() == null || entry.getKey().equals(key.getValue())) {
                entry.getValue().activate(key.getSpecificity(), builder);
            } else {
                // TODO poison nodes which don't match value?
            }
        }
    }

    public void activateRoot(SearchState.Builder builder) {
        rootSettings.activate(new Specificity(), builder);
    }

    public Node getRootSettings() {
        return rootSettings;
    }
}

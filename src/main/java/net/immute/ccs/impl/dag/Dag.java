package net.immute.ccs.impl.dag;

import net.immute.ccs.impl.SearchState;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class Dag {
    // protected to provide mutable builder interface during loading...
    protected final Map<String, Map<String ,Node>> children; // note: second-level map must allow null keys!
    protected final Node rootSettings;

    Dag() {
        children = new HashMap<>();
        rootSettings = new Node();
    }

    Dag(Map<String, Map<String ,Node>> children, Node rootSettings) {
        this.children = children;
        this.rootSettings = rootSettings;
    }

    public void traverse(Key key, SearchState.Builder builder) {
        Map<String, Node> nodes = children.get(key.getName());
        if (nodes == null) return;
        for (Map.Entry<String, Node> entry : nodes.entrySet()) {
            if (entry.getKey() == null || entry.getKey().equals(key.getValue())) {
                entry.getValue().activate(key.getSpecificity(), builder); // TODO should this be key.specificity? or like, the rule specificity? (no val if entry.getKey() is null)
            } else {
                // TODO poison nodes which don't match value?
            }
        }
    }

    public void activateRoot(SearchState.Builder builder) {
        rootSettings.activate(new Specificity(), builder);
    }

    public void forEachRule(String name, SearchState searchState, Consumer<String> consumer) {
        Map<String, Node> nodes = children.get(name);
        if (nodes == null) return;
        for (Map.Entry<String, Node> entry : nodes.entrySet()) {
            if (entry.getKey() == null) continue;
            if (searchState.changesProperties(new Key(name, entry.getKey())))
                consumer.accept(entry.getKey());
        }
    }
}

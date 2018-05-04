package net.immute.ccs.impl.dag;

import net.immute.ccs.CcsProperty;
import net.immute.ccs.impl.SearchState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Node {
    private final HashMap<Key, Node> children = new HashMap<>();
    private final Set<Tally> tallies = new HashSet<>();
    private final HashMap<String, List<CcsProperty>> props = new HashMap<>();
    private final Key constraints = new Key();

    public Set<Tally> getTallies() {
        return tallies;
    }

    public void addTally(Tally tally) {
        tallies.add(tally);
    }

    public Node getChild(Key key) {
        return children.get(key);
    }

    public void addChild(Key key, Node child) {
        children.put(key, child);
    }

    public void getChildren(Key key, Specificity spec, SearchState searchState) {
        for (Map.Entry<Key, Node> entry : children.entrySet())
            if (entry.getKey().matches(key))
                entry.getValue().activate(spec.add(entry.getKey().getSpecificity()), searchState);
    }

    public void addProperty(String name, CcsProperty value) {
        List<CcsProperty> values = props.computeIfAbsent(name, k -> new ArrayList<>());
        values.add(value);
    }

    public void addConstraint(Key key) {
        constraints.addAll(key);
    }

    public void activate(Specificity spec, SearchState searchState) {
        searchState.constrain(constraints);
        if (searchState.add(spec, this)) {
            for (Map.Entry<String, List<CcsProperty>> prop : props.entrySet())
                for (CcsProperty v : prop.getValue())
                    searchState.cacheProperty(prop.getKey(), spec, v);
            for (Tally tally : this.tallies)
                tally.activate(this, spec, searchState);
        }
    }
}

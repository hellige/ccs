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
    private final Set<Tally> tallies = new HashSet<>();
    private final HashMap<String, List<CcsProperty>> props = new HashMap<>();
    private final Set<Key> constraints = new HashSet<>();

    public Set<Tally> getTallies() {
        return tallies;
    }

    public void addTally(Tally tally) {
        tallies.add(tally);
    }

    public void addProperty(String name, CcsProperty value) {
        List<CcsProperty> values = props.computeIfAbsent(name, k -> new ArrayList<>());
        values.add(value);
    }

    public void addConstraint(Key key) {
        constraints.add(key);
    }

    public void activate(Specificity spec, SearchState.Builder searchState) {
        for (Key constraint : constraints)
            searchState.constrain(constraint);
        for (Map.Entry<String, List<CcsProperty>> prop : props.entrySet())
            for (CcsProperty v : prop.getValue())
                searchState.addProperty(prop.getKey(), spec, v);
        for (Tally tally : this.tallies)
            tally.activate(this, spec, searchState);
    }

    public boolean trialActivate(Specificity specificity, SearchState searchState) {
        return false;
    }
}
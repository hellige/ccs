package net.immute.ccs.dag;

import net.immute.ccs.*;

import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Collections.frequency;

public class Node {
    private final HashMap<Key, Node> children = new HashMap<Key, Node>();
    private final Set<Tally> tallies = new HashSet<Tally>();
    private final HashMap<String, List<CcsProperty>> props = new HashMap<String, List<CcsProperty>>();
    private final HashMap<String, List<CcsProperty>> localProps = new HashMap<String, List<CcsProperty>>();

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

    public void getChildren(Key key, SearchContext sc, boolean includeDirectChildren,
                            SortedMap<Specificity, List<Node>> results) {
        for (Key pattern : children.keySet()) {
            if (pattern.matches(key, sc, includeDirectChildren)) {
                List<Node> nodes = results.get(pattern.getSpecificity());
                if (nodes == null) {
                    nodes = new ArrayList<Node>();
                    results.put(pattern.getSpecificity(), nodes);
                }
                nodes.add(getChild(pattern));
            }
        }
    }

    public void getChildren(Key key, Specificity spec, SearchContext sc,
                            boolean includeDirectChildren, SearchState searchState) {
        for (Map.Entry<Key, Node> entry : children.entrySet())
            if (entry.getKey().matches(key, sc, includeDirectChildren))
                entry.getValue().activate(spec.add(entry.getKey().getSpecificity()), searchState);
    }

    // TODO does returning the whole list here really buy anything? i think not...
    public List<CcsProperty> getProperty(String name, boolean locals) {
        List<CcsProperty> values = null;
        if (locals) values = localProps.get(name);
        if (values == null) values = props.get(name);
        if (values == null) values = emptyList();
        return values;
    }

    public void addProperty(String name, CcsProperty value, boolean isLocal) {
        HashMap<String, List<CcsProperty>> theProps = isLocal ? localProps : props;
        List<CcsProperty> values = theProps.get(name);
        if (values == null) {
            values = new ArrayList<CcsProperty>();
            theProps.put(name, values);
        } else {
            CcsLogger.warn("Conflict detected for property: " + name
                    + ". Last set at: " + values.get(values.size() - 1).getOrigin()
                    + ". New setting at: " + value.getOrigin()
                    + ". Using new value.");
        }
        values.add(value);
    }

    public void activate(Specificity spec, SearchState searchState) {
        searchState.add(spec, this);
        for (Tally tally : this.tallies) tally.activate(this, spec, searchState);
    }
}

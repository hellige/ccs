package net.immute.ccs.dag;

import net.immute.ccs.CcsLogger;
import net.immute.ccs.CcsProperty;
import net.immute.ccs.SearchContext;
import net.immute.ccs.Specificity;
import net.immute.ccs.tree.Key;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.SortedMap;

import static java.util.Collections.emptyList;

public class Node {
    private final HashMap<Key, Node> children = new HashMap<Key, Node>();
    private final HashMap<String, List<CcsProperty>> props =
        new HashMap<String, List<CcsProperty>>();
    private final HashMap<String, List<CcsProperty>> localProps =
        new HashMap<String, List<CcsProperty>>();

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

    public List<CcsProperty> getProperty(String name, boolean locals) {
        List<CcsProperty> values = null;
        if (locals) {
            values = localProps.get(name);
        }
        if (values == null) {
            values = props.get(name);
        }
        if (values == null) {
            values = emptyList();
        }
        return values;
    }

    public void addProperty(String name, CcsProperty value, boolean isLocal) {
        HashMap<String, List<CcsProperty>> theProps;
        if (isLocal) theProps = localProps;
        else theProps = props;
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
}

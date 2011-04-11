package net.immute.ccs.tree;

import java.util.*;

import net.immute.ccs.CCSLogger;
import net.immute.ccs.CCSProperty;
import net.immute.ccs.SearchContext;
import net.immute.ccs.Specificity;

import static java.util.Collections.emptyList;

public class CCSNode {
    private final HashMap<Key, CCSNode> children = new HashMap<Key, CCSNode>();
    private final HashMap<String, List<CCSProperty>> props =
        new HashMap<String, List<CCSProperty>>();
    private final HashMap<String, List<CCSProperty>> localProps =
        new HashMap<String, List<CCSProperty>>();

    public CCSNode getChild(Key key) {
        return children.get(key);
    }

    public void addChild(Key key, CCSNode child) {
        children.put(key, child);
    }

    public void getChildren(Key key, SearchContext sc, boolean includeDirectChildren,
                            SortedMap<Specificity, List<CCSNode>> results) {
        for (Key pattern : children.keySet()) {
            if (pattern.matches(key, sc, includeDirectChildren)) {
                List<CCSNode> nodes = results.get(pattern.getSpecificity());
                if (nodes == null) {
                    nodes = new ArrayList<CCSNode>();
                    results.put(pattern.getSpecificity(), nodes);
                }
                nodes.add(getChild(pattern));
            }
        }
    }

    public List<CCSProperty> getProperty(String name, boolean locals) {
        List<CCSProperty> values = null;
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

    public void addProperty(String name, CCSProperty value, boolean isLocal) {
        HashMap<String, List<CCSProperty>> theProps;
        if (isLocal) theProps = localProps;
        else theProps = props;
        List<CCSProperty> values = theProps.get(name);
        if (values == null) {
            values = new ArrayList<CCSProperty>();
            theProps.put(name, values);
        } else {
            CCSLogger.warn("Conflict detected for property: " + name
                + ". Last set at: " + values.get(values.size()-1).getOrigin()
                + ". New setting at: " + value.getOrigin()
                + ". Using new value.");
        }
        values.add(value);
    }
}

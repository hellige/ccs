package net.immute.ccs.tree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.immute.ccs.CCSLogger;
import net.immute.ccs.CCSProperty;
import net.immute.ccs.SearchContext;

public class CCSNode {
    protected HashMap<Key, CCSNode> children = new HashMap<Key, CCSNode>();

    protected HashMap<String, List<CCSProperty>> props =
        new HashMap<String, List<CCSProperty>>();

    protected HashMap<String, List<CCSProperty>> localProps =
        new HashMap<String, List<CCSProperty>>();

    public CCSNode getChild(Key key) {
        return children.get(key);
    }

    public void addChild(Key key, CCSNode child) {
        children.put(key, child);
    }

    public List<CCSNode> getChildren(Key key, SearchContext sc,
        boolean includeDirectChildren) {
        List<CCSNode> matches = new ArrayList<CCSNode>();
        for (Key pattern : children.keySet()) {
            if (pattern.matches(key, sc, includeDirectChildren)) {
                matches.add(getChild(pattern));
            }
        }
        return matches;
    }

    public List<CCSProperty> getProperty(String name, boolean locals) {
        List<CCSProperty> values = null;
        if (locals) {
            values = localProps.get(name);
        }
        if (values == null) {
            values = props.get(name);
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
                + ". Last set at: " + values.get(0).getOrigin()
                + ". New setting at: " + value.getOrigin()
                + ". Using new value.");
        }
        values.add(value);
    }

    public boolean isEmpty() {
        return (props.isEmpty() && localProps.isEmpty() && children.isEmpty());
    }

}

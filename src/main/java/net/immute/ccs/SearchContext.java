package net.immute.ccs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import net.immute.ccs.tree.CCSNode;
import net.immute.ccs.tree.Key;

public class SearchContext {
    private SearchContext parent = null;

    private List<CCSNode> nodeList;

    private Key key;

    private static final Comparator<CCSProperty> PROP_COMPARATOR =
        new Comparator<CCSProperty>() {
            public int compare(CCSProperty p1, CCSProperty p2) {
                int result = p1.getSpecificity().compareTo(p2.getSpecificity());
                if (result == 0) {
                    result = p1.getPropertyNumber() - p2.getPropertyNumber();
                }
                return result;
            }
        };

    private SearchContext(CCSNode root) {
        nodeList = new ArrayList<CCSNode>();
        nodeList.add(root);
    }

    public SearchContext(CCSNode root, String element) {
        parent = new SearchContext(root);
        key = new Key(element);
        key.setRoot(true);
    }

    public SearchContext(SearchContext parent, String element) {
        this.parent = parent;
        key = new Key(element);
    }

    public SearchContext(SearchContext parent, String element, String id) {
        this.parent = parent;
        key = new Key(element);
        key.setId(id);
    }

    public SearchContext(SearchContext parent, String element, String id,
        String... classes) {
        this.parent = parent;
        key = new Key(element, classes);
        key.setId(id);
    }

    public String getProperty(String propertyName) {
        return findProperty(propertyName, true).getValue();
    }

    private CCSProperty findProperty(String propertyName, boolean locals) {
        // first cascade... look in nodes matched by this pattern.
        TreeSet<CCSProperty> props = new TreeSet<CCSProperty>(PROP_COMPARATOR);
        for (CCSNode n : getNodes()) {
            List<CCSProperty> values = n.getProperty(propertyName, locals);
            if (values != null) {
                props.addAll(values);
            }
        }
        // if the property is set, return the best match.
        if (!props.isEmpty()) {
            return props.last();
        }

        // if not, then inherit...
        if (parent != null) {
            return parent.findProperty(propertyName, false);
        }

        // if still not, then die...
        throw new NoSuchPropertyException(propertyName);
    }

    private List<CCSNode> getNodes() {
        if (nodeList == null) {
            nodeList = new ArrayList<CCSNode>();
            boolean includeDirectChildren = true;
            SearchContext p = parent;
            while (p != null) {
                for (CCSNode n : p.getNodes()) {
                    nodeList.addAll(n.getChildren(key, parent,
                        includeDirectChildren));
                }
                includeDirectChildren = false;
                p = p.parent;
            }
        }
        return nodeList;
    }
}

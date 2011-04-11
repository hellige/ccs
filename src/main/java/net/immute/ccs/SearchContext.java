package net.immute.ccs;

import java.util.*;

import net.immute.ccs.tree.Node;
import net.immute.ccs.tree.Key;

import static java.util.Collections.reverseOrder;
import static java.util.Collections.singletonList;

public class SearchContext {
    private final SearchContext parent;
    private final Key key;

    private List<List<Node>> nodeList;

    private static final Comparator<CcsProperty> PROP_COMPARATOR =
        new Comparator<CcsProperty>() {
            public int compare(CcsProperty p1, CcsProperty p2) {
                return p1.getPropertyNumber() - p2.getPropertyNumber();
            }
        };

    private SearchContext(Node root) {
        nodeList = singletonList(singletonList(root));
        parent = null;
        key = null;
    }

    public SearchContext(Node root, String element) {
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

    private CcsProperty findProperty(String propertyName, boolean locals) {
        // first, look in nodes newly matched by this pattern...
        for (List<Node> ns : getNodes()) {
            List<CcsProperty> values = new ArrayList<CcsProperty>();
            for (Node n : ns)
                values.addAll(n.getProperty(propertyName, locals));
            if (values.size() == 1)
                return values.get(0);
            else if (values.size() > 1) {
                // TODO make warning optional and add info about conflicting settings
                CcsLogger.warn("Relying on declaration order to resolve ambiguity. Are you sure you want this?");
                return Collections.max(values, PROP_COMPARATOR);
            }
        }

        // if not, then inherit...
        if (parent != null) {
            return parent.findProperty(propertyName, false);
        }

        // if still not, then die...
        throw new NoSuchPropertyException(propertyName);
    }

    private List<List<Node>> getNodes() {
        if (nodeList == null) {
            SortedMap<Specificity, List<List<Node>>> buckets =
                    new TreeMap<Specificity, List<List<Node>>>(reverseOrder());
            boolean includeDirectChildren = true;
            SearchContext p = parent;
            while (p != null) {
                for (List<Node> ns : p.getNodes()) {
                    SortedMap<Specificity, List<Node>> results =
                            new TreeMap<Specificity, List<Node>>(reverseOrder());
                    for (Node n : ns)
                        n.getChildren(key, parent, includeDirectChildren, results);
                    for (Specificity spec : results.keySet()) {
                        List<List<Node>> bucket = buckets.get(spec);
                        if (bucket == null) {
                            bucket = new ArrayList<List<Node>>();
                            buckets.put(spec, bucket);
                        }
                        bucket.add(results.get(spec));
                    }
                }
                includeDirectChildren = false;
                p = p.parent;
            }
            nodeList = new ArrayList<List<Node>>();
            for (List<List<Node>> nss : buckets.values())
                nodeList.addAll(nss);
        }
        return nodeList;
    }
}

package net.immute.ccs;

import java.util.*;

import net.immute.ccs.tree.CCSNode;
import net.immute.ccs.tree.Key;

import static java.util.Collections.reverseOrder;
import static java.util.Collections.singletonList;

public class SearchContext {
    private final SearchContext parent;
    private final Key key;

    private List<List<CCSNode>> nodeList;

    private static final Comparator<CCSProperty> PROP_COMPARATOR =
        new Comparator<CCSProperty>() {
            public int compare(CCSProperty p1, CCSProperty p2) {
                return p1.getPropertyNumber() - p2.getPropertyNumber();
            }
        };

    private SearchContext(CCSNode root) {
        nodeList = singletonList(singletonList(root));
        parent = null;
        key = null;
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
        // first, look in nodes newly matched by this pattern...
        for (List<CCSNode> ns : getNodes()) {
            List<CCSProperty> values = new ArrayList<CCSProperty>();
            for (CCSNode n : ns)
                values.addAll(n.getProperty(propertyName, locals));
            if (values.size() == 1)
                return values.get(0);
            else if (values.size() > 1) {
                // TODO make warning optional and add info about conflicting settings
                CCSLogger.warn("Relying on declaration order to resolve ambiguity. Are you sure you want this?");
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

    private List<List<CCSNode>> getNodes() {
        if (nodeList == null) {
            SortedMap<Specificity, List<List<CCSNode>>> buckets =
                    new TreeMap<Specificity, List<List<CCSNode>>>(reverseOrder());
            boolean includeDirectChildren = true;
            SearchContext p = parent;
            while (p != null) {
                for (List<CCSNode> ns : p.getNodes()) {
                    SortedMap<Specificity, List<CCSNode>> results =
                            new TreeMap<Specificity, List<CCSNode>>(reverseOrder());
                    for (CCSNode n : ns)
                        n.getChildren(key, parent, includeDirectChildren, results);
                    for (Specificity spec : results.keySet()) {
                        List<List<CCSNode>> bucket = buckets.get(spec);
                        if (bucket == null) {
                            bucket = new ArrayList<List<CCSNode>>();
                            buckets.put(spec, bucket);
                        }
                        bucket.add(results.get(spec));
                    }
                }
                includeDirectChildren = false;
                p = p.parent;
            }
            nodeList = new ArrayList<List<CCSNode>>();
            for (List<List<CCSNode>> nss : buckets.values())
                nodeList.addAll(nss);
        }
        return nodeList;
    }
}

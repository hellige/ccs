package net.immute.ccs;

import net.immute.ccs.tree.Key;
import net.immute.ccs.dag.Node;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.reverseOrder;
import static java.util.Collections.singletonList;

public class SearchContext {
    private final AtomicReference<List<List<Node>>> nodeList = new AtomicReference<List<List<Node>>>();

    private final SearchContext parent;
    private final Key key;

    private static final Comparator<CcsProperty> PROP_COMPARATOR =
        new Comparator<CcsProperty>() {
            public int compare(CcsProperty p1, CcsProperty p2) {
                return p1.getPropertyNumber() - p2.getPropertyNumber();
            }
        };

    private SearchContext(Node root) {
        nodeList.set(singletonList(singletonList(root)));
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

    public String getKey() {
        return key.toString();
    }

    public boolean hasProperty(String propertyName) {
        return findProperty(propertyName, true) != null;
    }

    public CcsProperty getProperty(String propertyName) {
        return findProperty(propertyName, true);
    }

    public String getString(String propertyName) {
        CcsProperty prop = findProperty(propertyName, true);
        if (prop == null) throw new NoSuchPropertyException(propertyName, this);
        return prop.getValue();
    }

    public String getString(String propertyName, String defaultValue) {
        CcsProperty property = getProperty(propertyName);
        String result = property == null ? defaultValue : property.getValue();
        return result;
    }

    public int getInt(String propertyName) {
        int result = Integer.parseInt(getString(propertyName));
        return result;
    }

    public int getInt(String propertyName, int defaultValue) {
        CcsProperty property = getProperty(propertyName);
        int result = property == null ? defaultValue : Integer.parseInt(property.getValue());
        return result;
    }

    public double getDouble(String propertyName) {
        double result = Double.parseDouble(getString(propertyName));
        return result;
    }

    public double getDouble(String propertyName, double defaultValue) {
        CcsProperty property = getProperty(propertyName);
        double result = property == null ? defaultValue : Double.parseDouble(property.getValue());
        return result;
    }

    public boolean getBoolean(String propertyName) {
        boolean result = Boolean.parseBoolean(getString(propertyName));
        return result;
    }

    public boolean getBoolean(String propertyName, boolean defaultValue) {
        CcsProperty property = getProperty(propertyName);
        boolean result = property == null ? defaultValue : Boolean.parseBoolean(property.getValue());
        return result;
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

        return null;
    }

    private List<List<Node>> getBucket(SortedMap<Specificity, List<List<Node>>> buckets, Specificity spec) {
        List<List<Node>> bucket = buckets.get(spec);
        if (bucket == null) {
            bucket = new ArrayList<List<Node>>();
            buckets.put(spec, bucket);
        }
        return bucket;
    }

    private List<List<Node>> getNodes() {
        if (nodeList.get() == null) {
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
                    for (Specificity spec : results.keySet())
                        getBucket(buckets, spec).add(results.get(spec));
                }
                includeDirectChildren = false;
                p = p.parent;
            }

            List<List<Node>> tmp = new ArrayList<List<Node>>();
            for (List<List<Node>> nss : buckets.values())
                tmp.addAll(nss);

            nodeList.compareAndSet(null, tmp);
        }

        return nodeList.get();
    }

    @Override
    public String toString() {
        if (parent != null) {
            if (parent.parent != null)
                return parent + " > " + key;
            else
                return key.toString();
        } else {
            return "<root>";
        }
    }
}

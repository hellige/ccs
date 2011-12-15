package net.immute.ccs;

import net.immute.ccs.dag.Key;
import net.immute.ccs.dag.Node;

import java.util.concurrent.atomic.AtomicReference;

public class SearchContext {
    private final AtomicReference<SearchState> searchState = new AtomicReference<SearchState>();

    private final SearchContext parent;
    private final Key key;

    SearchContext(Node root, CcsLogger log) {
        searchState.set(new SearchState(root, this, log));
        parent = null;
        key = null;
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
        CcsProperty prop = getSearchState().findProperty(propertyName, locals);
        if (prop != null) return prop;

        // if not, then inherit...
        if (parent != null) {
            return parent.findProperty(propertyName, false);
        }

        return null;
    }

    private SearchState getSearchState() {
        if (searchState.get() == null) {
            SearchState tmp = parent.getSearchState().newChild(this);

            boolean includeDirectChildren = true;
            SearchContext p = parent;
            while (p != null) {
                // TODO be nice to replace parent with 'this', but it's really not clear that that's even possible...
                p.getSearchState().extend(key, parent, includeDirectChildren, tmp);
                includeDirectChildren = false;
                p = p.parent;
            }

            searchState.compareAndSet(null, tmp);
        }

        return searchState.get();
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

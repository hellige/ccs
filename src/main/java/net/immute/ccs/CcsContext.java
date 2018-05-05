package net.immute.ccs;

import net.immute.ccs.impl.SearchState;
import net.immute.ccs.impl.dag.Key;
import net.immute.ccs.impl.dag.Node;

public class CcsContext {
    private final SearchState searchState;
    private final CcsContext parent;

    CcsContext(Node root, CcsLogger log, boolean logAccesses) {
        parent = null;
        searchState = new SearchState(root, this, log, logAccesses);
    }

    private CcsContext(CcsContext parent, Key key) {
        this.parent = parent;
        searchState = parent.searchState.newChild(parent.searchState, this, key);
    }

    private CcsContext(CcsContext parent, String name, String... values) {
        this(parent, new Key(name, values));
    }

    public CcsContext.Builder builder() {
        return new Builder();
    }

    public CcsContext constrain(String name) {
        return new CcsContext(this, name);
    }

    public CcsContext constrain(String name, String... values) {
        return new CcsContext(this, name, values);
    }

    public String getKey() {
        return searchState.getKey();
    }

    public boolean hasProperty(String propertyName) {
        return getProperty(propertyName) != null;
    }

    public CcsProperty getProperty(String propertyName) {
        return searchState.findProperty(propertyName);
    }

    public String getString(String propertyName) {
        CcsProperty prop = getProperty(propertyName);
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

    @Override
    public String toString() {
        if (parent != null) {
            if (parent.parent != null)
                return parent + " > " + getKey();
            else
                return getKey();
        } else {
            return "<root>";
        }
    }

    public class Builder {
        private final Key key = new Key();

        private Builder() {}

        public Builder add(String name, String... values) {
            key.addName(name);
            for (String value : values) key.addValue(name, value);
            return this;
        }

        public CcsContext build() {
            return new CcsContext(CcsContext.this, key);
        }
    }
}

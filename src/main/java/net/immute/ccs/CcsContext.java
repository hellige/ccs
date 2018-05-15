package net.immute.ccs;

import net.immute.ccs.impl.SearchState;
import net.immute.ccs.impl.dag.Dag;
import net.immute.ccs.impl.dag.Key;

import java.util.function.BiConsumer;

public class CcsContext {
    private final SearchState searchState;

    CcsContext(Dag dag, CcsLogger log, boolean logAccesses) {
        searchState = new SearchState.Builder(dag, log, logAccesses).build();
    }

    private CcsContext(CcsContext parent, Key key) {
        searchState = parent.searchState.newChild(key);
    }

    private CcsContext(CcsContext parent, String name, String value) {
        this(parent, new Key(name, value));
    }

    public CcsContext.Builder builder() {
        return new Builder();
    }

    public CcsContext constrain(String name) {
        return new CcsContext(this, name, null);
    }

    public CcsContext constrain(String name, String value) {
        return new CcsContext(this, name, value);
    }

    public void forEachProperty(BiConsumer<String, CcsProperty> consumer) {
        searchState.forEachProperty(consumer);
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
        return searchState.toString();
    }

    public class Builder {
        private Key key;

        private Builder() {}

        // TODO simplify this if it's really no longer allowed. or else find a way to make it sensible
        public Builder add(String name, String... values) {
            if (key != null)
                throw new UnsupportedOperationException();
            if (values.length == 0)
                key = new Key(name, null);
            else if (values.length == 1)
                key = new Key(name, values[0]);
            else
                throw new UnsupportedOperationException();
            return this;
        }

        public CcsContext build() {
            return new CcsContext(CcsContext.this, key);
        }
    }
}

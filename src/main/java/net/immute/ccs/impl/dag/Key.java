package net.immute.ccs.impl.dag;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Key {
    private final Map<String, Set<String>> values = new HashMap<String, Set<String>>();

    private Specificity specificity = new Specificity();

    public Key() {}

    public Key(String name, String... values) {
        addName(name);
        for (String value : values) addValue(name, value);
    }

    public void addName(String name) {
        if (!values.containsKey(name)) {
            values.put(name, new HashSet<String>());
            specificity = specificity.incElementNames();
        }
    }

    public Specificity getSpecificity() {
        return specificity;
    }

    public void addValue(String name, String value) {
        Set<String> vals = values.get(name);
        if (!vals.contains(value)) {
            vals.add(value);
            specificity = specificity.incClassSelectors();
        }
    }

    /**
     * treating this as a pattern, see whether it matches the given specific
     * key. this is asymmetric because the given key can have unmatched (extra)
     * names/values, but the current object must fully match the key. wildcards
     * also match on the current object, but not on the given key.
     * @param k the key to test against.
     * @return true if this object, as a pattern, matches the given key.
     */
    public boolean matches(Key k) {
        for (Map.Entry<String, Set<String>> pair : values.entrySet()) {
            if (!k.values.containsKey(pair.getKey())) return false;
            if (!k.values.get(pair.getKey()).containsAll(pair.getValue())) return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Set<String>> pair : values.entrySet()) {
            if (!first) result.append('/');
            result.append(pair.getKey());
            for (String v : pair.getValue()) result.append('.').append(v);
            first = false;
        }
        return result.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Key key = (Key) o;

        if (values != null ? !values.equals(key.values) : key.values != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return values != null ? values.hashCode() : 0;
    }
}

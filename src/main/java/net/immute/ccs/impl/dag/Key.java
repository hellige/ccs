package net.immute.ccs.impl.dag;

import java.util.*;
import java.util.regex.Pattern;

// TODO refactor this to make it much simpler: key-value pair
public class Key {
    private final static Pattern identRegex = Pattern.compile("^[A-Za-z$_][A-Za-z0-9$_]*$");

    // we use a linked map only so that toString() preserves the input order of names. similary,
    // we insist upon linked sets for the names.
    private final LinkedHashMap<String, LinkedHashSet<String>> values = new LinkedHashMap<>();

    private Specificity specificity = new Specificity();

    public Key() {}

    public Key(String name, String value) {
        addName(name);
        if (value != null) addValue(name, value);
    }

    public boolean addName(String name) {
        if (!values.containsKey(name)) {
            values.put(name, new LinkedHashSet<>());
            specificity = specificity.incNames();
            return true;
        }
        return false;
    }

    public Specificity getSpecificity() {
        return specificity;
    }

    public boolean addValue(String name, String value) {
        boolean changed = false;

        LinkedHashSet<String> vals = values.get(name);
        if (vals == null) {
            changed = true;
            vals = new LinkedHashSet<>();
            values.put(name, vals);
            specificity = specificity.incNames();
        }

        if (!vals.contains(value)) {
            changed = true;
            vals.add(value);
            specificity = specificity.incValues();
        }

        return changed;
    }

    public boolean addAll(Key key) {
        boolean changed = false;
        for (Map.Entry<String, LinkedHashSet<String>> pair : key.values.entrySet()) {
            changed |= addName(pair.getKey());
            for (String value : pair.getValue())
                changed |= addValue(pair.getKey(), value);
        }
        return changed;
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
        for (Map.Entry<String, LinkedHashSet<String>> pair : values.entrySet()) {
            if (!k.values.containsKey(pair.getKey())) return false;
            if (!k.values.get(pair.getKey()).containsAll(pair.getValue())) return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, LinkedHashSet<String>> pair : values.entrySet()) {
            if (!first) result.append('/');
            result.append(pair.getKey());
            for (String v : pair.getValue()) {
                if (identRegex.matcher(v).matches())
                    result.append('.').append(v);
                else
                    result.append(".'").append(v.replace("'", "\\'")).append('\'');
            }
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

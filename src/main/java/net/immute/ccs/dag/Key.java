package net.immute.ccs.dag;

import net.immute.ccs.NoSuchPropertyException;
import net.immute.ccs.SearchContext;
import net.immute.ccs.Specificity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Key {
    private final Map<String, String> attributes = new HashMap<String, String>();
    private final Set<String> classes = new HashSet<String>();

    private Specificity specificity;
    private String element;
    private String id;
    private boolean directChild;

    public Key(String element, String... classes) {
        this.element = element;
        directChild = false;
        specificity = new Specificity();
        if (element != null) {
            specificity = specificity.incElementNames();
        }
        for (String cls : classes) {
            this.classes.add(cls);
            specificity = specificity.incClassSelectors();
        }
    }

    public void setElement(String element) {
        this.element = element;
        if (element != null) {
            // TODO not quite right...
            specificity = specificity.incElementNames();
        }
    }

    public Specificity getSpecificity() {
        return specificity;
    }

    public void setId(String id) {
        this.id = id;
        specificity = specificity.incIdSelectors();
    }

    public void setDirectChild(boolean directChild) {
        this.directChild = directChild;
    }

    public void setAttribute(String key, String value) {
        attributes.put(key, value);
        specificity = specificity.incClassSelectors();
    }

    public void addClass(String cls) {
        classes.add(cls);
        specificity = specificity.incClassSelectors();
    }

    /**
     * treating this as a pattern, see whether it matches the given specific
     * key. this is asymmetric because the given key can have unmatched (extra)
     * attributes, but the current object must fully match the key. wildcards
     * also match on the current object, but not on the given key.
     * @param k the key to test against.
     * @param sc the context to use for "attribute" queries
     * @param includeDirectChildren whether the incoming key represents a single
     *  step from the parent node. i.e., should a direct-child constraint on this
     *  key succeed or fail?
     * @return true if this object, as a pattern, matches the given key.
     */
    public boolean matches(Key k, SearchContext sc,
        boolean includeDirectChildren) {
        if (directChild && !includeDirectChildren) {
            return false;
        }

        if (element != null && !element.equals(k.element)) {
            return false;
        }

        if (id != null && !id.equals(k.id)) {
            return false;
        }

        for (String cls : classes) {
            if (!k.classes.contains(cls)) {
                return false;
            }
        }

        // this will be the most expensive check, so we'll do it at the end.
        try {
            for (String attrib : attributes.keySet()) {
                if (sc == null
                    || !attributes.get(attrib).equals(sc.getString(attrib))) {
                    return false;
                }
            }
        } catch (NoSuchPropertyException e) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        if (element != null)
            result.append(element);
        else
            result.append("*");
        if (id != null) result.append("#").append(id);
        for (String c : classes) result.append(".").append(c);
        for (String a : attributes.keySet()) result.append("[").append(a).append("=").append(attributes.get(a))
                .append("]");
        return result.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Key key = (Key) o;

        if (directChild != key.directChild) return false;
        if (attributes != null ? !attributes.equals(key.attributes) : key.attributes != null) return false;
        if (classes != null ? !classes.equals(key.classes) : key.classes != null) return false;
        if (element != null ? !element.equals(key.element) : key.element != null) return false;
        if (id != null ? !id.equals(key.id) : key.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = element != null ? element.hashCode() : 0;
        result = 31 * result + (attributes != null ? attributes.hashCode() : 0);
        result = 31 * result + (classes != null ? classes.hashCode() : 0);
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (directChild ? 1 : 0);
        return result;
    }
}

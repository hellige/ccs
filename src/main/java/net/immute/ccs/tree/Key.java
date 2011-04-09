package net.immute.ccs.tree;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.immute.ccs.NoSuchPropertyException;
import net.immute.ccs.SearchContext;

public class Key {
    String element;

    String id;

    boolean root;

    boolean directChild;

    Map<String, String> attributes = new HashMap<String, String>();

    Set<String> classes = new HashSet<String>();

    public Key(String element, String... classes) {
        this.element = element;
        root = false;
        directChild = false;
        for (String cls : classes) {
            this.classes.add(cls);
        }
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setDirectChild(boolean directChild) {
        this.directChild = directChild;
    }

    public void setRoot(boolean root) {
        this.root = root;
    }

    public void setAttribute(String key, String value) {
        attributes.put(key, value);
    }

    public String getAttribute(String key) {
        return attributes.get(key);
    }

    public void addClass(String cls) {
        classes.add(cls);
    }

    /**
     * treating this as a pattern, see whether it matches the given specific
     * key. this is asymmetric because the given key can have unmatched (extra)
     * attributes, but the current object must fully match the key. wildcards
     * also match on the current object, but not on the given key.
     * @param k the key to test against.
     * @return true if this object, as a pattern, matches the given key.
     */
    public boolean matches(Key k, SearchContext sc,
        boolean includeDirectChildren) {
        if (directChild && !includeDirectChildren) {
            return false;
        }

        if (root && !k.root) {
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
                    || !attributes.get(attrib).equals(sc.getProperty(attrib))) {
                    return false;
                }
            }
        } catch (NoSuchPropertyException e) {
            return false;
        }
        return true;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result =
            PRIME * result + ((attributes == null) ? 0 : attributes.hashCode());
        result = PRIME * result + ((element == null) ? 0 : element.hashCode());
        result = PRIME * result + ((id == null) ? 0 : id.hashCode());
        result = PRIME * result + (root ? 1231 : 1237);
        result = PRIME * result + (directChild ? 1231 : 1237);
        return result;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (getClass() != obj.getClass()) return false;
        final Key other = (Key) obj;
        if (attributes == null) {
            if (other.attributes != null) return false;
        } else if (!attributes.equals(other.attributes)) return false;
        if (element == null) {
            if (other.element != null) return false;
        } else if (!element.equals(other.element)) return false;
        if (id == null) {
            if (other.id != null) return false;
        } else if (!id.equals(other.id)) return false;
        if (root != other.root) return false;
        if (directChild != other.directChild) return false;
        return true;
    }
}

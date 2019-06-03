package net.immute.ccs.impl;

import net.immute.ccs.CcsContext;
import net.immute.ccs.CcsProperty;
import net.immute.ccs.CcsTracer;
import net.immute.ccs.impl.dag.Key;
import net.immute.ccs.impl.dag.Node;
import net.immute.ccs.impl.dag.Specificity;
import net.immute.ccs.impl.dag.Tally;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

public class SearchState {
    private static class PropertySetting {
        // we compare these in opposite order, so that the
        private static final Comparator<CcsProperty> PROP_COMPARATOR =
                Comparator.comparingInt(CcsProperty::getPropertyNumber);

        final Specificity spec;
        final boolean override;
        final SortedSet<CcsProperty> values = new TreeSet<>(PROP_COMPARATOR);

        PropertySetting(Specificity spec, CcsProperty value) {
            this.spec = spec;
            this.override = value.isOverride();
            values.add(value);
        }

        PropertySetting(PropertySetting that) {
            this.spec = that.spec;
            this.override = that.override;
            values.addAll(that.values);
        }


        boolean better(PropertySetting that) {
            if (override && !that.override) return true;
            if (!override && that.override) return false;
            return that.spec.lessThan(spec);
        }
    }

    private final HashMap<Node, Specificity> nodes = new HashMap<>();
    // cache of properties newly set in this context
    private final HashMap<String, PropertySetting> properties = new HashMap<>();
    private final Map<Tally.AndTally, Tally.TallyState> tallyMap = new HashMap<>();

    private final CcsTracer tracer;
    private final CcsContext ccsContext; // TODO i think this should go...
    private final SearchState parent;
    private final Key key;
    private boolean constraintsChanged;

    public SearchState(Node root, CcsContext ccsContext, CcsTracer tracer) {
        this.ccsContext = ccsContext;
        this.parent = null;
        this.key = new Key();
        this.tracer = tracer;

        constraintsChanged = false;
        Specificity emptySpecificity = new Specificity();
        root.activate(emptySpecificity, this);
        while (constraintsChanged) {
            constraintsChanged = false;
            root.getChildren(key, emptySpecificity, this);
        }
    }

    private SearchState(SearchState parent, CcsContext ccsContext, Key key) {
        this.ccsContext = ccsContext;
        this.parent = parent;
        this.key = key;
        this.tracer = parent.tracer;
    }

    public SearchState newChild(SearchState parent, CcsContext ccsContext, Key key) {
        SearchState child = new SearchState(this, ccsContext, key);
        boolean constraintsChanged;
        do {
            constraintsChanged = false;
            SearchState p = parent;
            while (p != null) {
                constraintsChanged |= child.extendWith(p);
                p = p.parent;
            }
        } while (constraintsChanged);
        return child;
    }

    private boolean extendWith(SearchState priorState) {
        constraintsChanged = false;
        for (Map.Entry<Node, Specificity> entry : priorState.nodes.entrySet())
            entry.getKey().getChildren(key, entry.getValue(), this);
        return constraintsChanged;
    }

    @SuppressWarnings("StringConcatenationInsideStringBufferAppend")
    public CcsProperty findProperty(String propertyName) {
        CcsProperty prop = doSearch(propertyName);
        if (prop == null) {
            tracer.onPropertyNotFound(ccsContext, propertyName);
        } else {
            tracer.onPropertyFound(ccsContext, propertyName, prop);
        }
        return prop;
    }

    private CcsProperty doSearch(String propertyName) {
        PropertySetting props = properties.get(propertyName);
        if (props == null) {
            if (parent != null) return parent.doSearch(propertyName);
            return null;
        }

        if (props.values.size() > 1) {
            // more than one value newly set in this node...
            tracer.onConflict(ccsContext, propertyName, props.values);
        }

        return props.values.last();
    }

    public String getKey() {
        return key.toString();
    }

    public Tally.TallyState getTallyState(Tally.AndTally tally) {
        if (tallyMap.containsKey(tally)) return tallyMap.get(tally);
        if (parent != null) return parent.getTallyState(tally);
        return new Tally.TallyState(tally);

    }

    public void setTallyState(Tally.AndTally tally, Tally.TallyState state) {
        tallyMap.put(tally, state);
    }

    public void constrain(Key constraints) {
        constraintsChanged |= key.addAll(constraints);
    }

    public boolean add(Specificity spec, Node node) {
        Specificity existing = nodes.get(node);

        if (existing == null || existing.lessThan(spec)) {
            nodes.put(node, spec);
            return true;
        }

        return false;
    }

    public void cacheProperty(String propertyName, Specificity spec, CcsProperty property) {
        PropertySetting setting = properties.get(propertyName);

        PropertySetting newSetting = new PropertySetting(spec, property);

        if (setting == null) {
            // we don't have a local setting for this yet.
            PropertySetting parentProperty = parent != null ? parent.checkCache(propertyName) : null;
            if (parentProperty != null) {
                if (parentProperty.better(newSetting))
                    // parent copy found, parent property better, leave local cache empty.
                    return;

                // copy parent property into local cache. this is done solely to
                // support conflict detection.
                setting = new PropertySetting(parentProperty);
                properties.put(propertyName, setting);
            }
        }

        // at this point, 'setting' is pointing to whatever value is currently in properties[propertyName],
        // or null if there's nothing there.

        if (setting == null) {
            properties.put(propertyName, newSetting);
        } else if (newSetting.better(setting)) {
            // new property better than local cache. replace.
            properties.put(propertyName, newSetting);
        } else if (setting.better(newSetting)) {
            // ignore
        } else {
            // new property has same specificity/override as existing... append.
            setting.values.add(property);
        }
    }

    private PropertySetting checkCache(String propertyName) {
        PropertySetting setting = properties.get(propertyName);
        if (setting != null) return setting;
        if (parent != null) return parent.checkCache(propertyName);
        return null;
    }
}

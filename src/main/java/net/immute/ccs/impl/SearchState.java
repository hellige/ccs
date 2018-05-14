package net.immute.ccs.impl;

import net.immute.ccs.CcsLogger;
import net.immute.ccs.CcsProperty;
import net.immute.ccs.impl.dag.Dag;
import net.immute.ccs.impl.dag.Key;
import net.immute.ccs.impl.dag.Node;
import net.immute.ccs.impl.dag.Specificity;
import net.immute.ccs.impl.dag.Tally;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.BiConsumer;

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

        boolean better(PropertySetting that) {
            if (override && !that.override) return true;
            if (!override && that.override) return false;
            return that.spec.lessThan(spec);
        }
    }

    private final Dag dag;
    private final Hamt<String, PropertySetting> properties;
    private final Hamt<Tally.AndTally, Tally.TallyState> tallyMap;
    private final CcsLogger log;
    private final boolean logAccesses;
    private final String description;

    private SearchState(Builder builder) {
        this.dag = builder.dag;
        this.properties = builder.properties;
        this.tallyMap = builder.tallyMap;
        this.log = builder.log;
        this.logAccesses = builder.logAccesses;
        this.description = builder.description.toString();
    }

    public static class Builder {
        private static final String ROOT_DESCRIPTION = "<root>";

        private final Deque<Key> constraints = new ArrayDeque<>();
        private final StringBuilder description = new StringBuilder();

        private Hamt<String, PropertySetting> properties;
        private Hamt<Tally.AndTally, Tally.TallyState> tallyMap;

        private final Dag dag;
        private final CcsLogger log;
        private final boolean logAccesses;

        public Builder(Dag dag, CcsLogger log, boolean logAccesses) {
            properties = new Hamt<>();
            tallyMap = new Hamt<>();
            this.dag = dag;
            this.log = log;
            this.logAccesses = logAccesses;
            this.description.append(ROOT_DESCRIPTION);
            dag.activateRoot(this);
        }

        Builder(Key key, SearchState parent) {
            properties = parent.properties;
            tallyMap = parent.tallyMap;
            dag = parent.dag;
            log = parent.log;
            logAccesses = parent.logAccesses;
            if (!parent.description.equals(ROOT_DESCRIPTION))
                description.append(parent.description);
            constraints.add(key);
        }

        public SearchState build() {
            while (!constraints.isEmpty()) {
                Key key = constraints.pop();
                if (description.length() > 0) description.append(" > ");
                description.append(key);
                dag.traverse(key, this);
            }

            return new SearchState(this);
        }

        public void constrain(Key constraint) {
            constraints.add(constraint);
        }

        public void addProperty(String propertyName, Specificity spec, CcsProperty property) {
            properties = properties.update(propertyName, setting -> {
                PropertySetting newSetting = new PropertySetting(spec, property);

                if (setting == null) return newSetting;
                if (newSetting.better(setting)) return newSetting;
                if (setting.better(newSetting)) return setting;

                // new property has same specificity/override... extend the existing settings with this new one
                // (for conflict reporting). mutate the new setting, because the old one must remain immutable.
                // TODO make this immutability guaranteed
                newSetting.values.addAll(setting.values);
                return newSetting;
            });
        }

        public Tally.TallyState activateTally(Tally.AndTally tally, Node leg, Specificity spec) {
            tallyMap = tallyMap.update(tally, state -> {
                if (state == null)
                    state = new Tally.TallyState(tally);
                return state.activate(leg, spec);
            });
            return tallyMap.get(tally); // TODO avoid second lookup
        }
    }

    public SearchState newChild(Key key) {
        Builder builder = new Builder(key, this);
        return builder.build();
    }

    private String origins(Collection<CcsProperty> values) {
        StringBuilder b = new StringBuilder();
        boolean first = true;
        for (CcsProperty v : values) {
            if (!first) b.append(", ");
            b.append(v.getOrigin());
            first = false;
        }
        return b.toString();
    }

    @SuppressWarnings("StringConcatenationInsideStringBufferAppend")
    public CcsProperty findProperty(String propertyName) {
        CcsProperty prop = doSearch(propertyName);
        if (logAccesses) {
            StringBuilder msg = new StringBuilder();
            if (prop != null) {
                msg.append("Found property: " + propertyName
                        + " = " + prop.getValue() + "\n");
                msg.append("    at " + prop.getOrigin() + " in context: [" + this + "]");
            } else {
                msg.append("Property not found: " + propertyName + "\n");
                msg.append("    in context: [" + this + "]");
            }
            log.info(msg.toString());
        }
        return prop;
    }

    private CcsProperty doSearch(String propertyName) {
        PropertySetting props = properties.get(propertyName);
        if (props == null) return null;

        if (props.values.size() > 1) {
            // more than one value newly set in this node...
            String msg = ("Conflict detected for property '" + propertyName
                    + "' in context [" + this + "]. "
                    + "(Conflicting settings at: [" + origins(props.values)
                    + "].) Using most recent value.");
            log.warn(msg);
        }

        return props.values.last();
    }

    public void forEachProperty(BiConsumer<String, CcsProperty> consumer) {
        properties.forEach((name, setting) -> consumer.accept(name, setting.values.last()));
    }

    @Override
    public String toString() {
        return description;
    }
}

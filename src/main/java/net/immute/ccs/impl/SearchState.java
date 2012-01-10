package net.immute.ccs.impl;

import net.immute.ccs.CcsContext;
import net.immute.ccs.CcsLogger;
import net.immute.ccs.CcsProperty;
import net.immute.ccs.impl.dag.Specificity;
import net.immute.ccs.impl.dag.Key;
import net.immute.ccs.impl.dag.Node;
import net.immute.ccs.impl.dag.Tally;

import java.util.*;

import static java.util.Collections.reverseOrder;
import static java.util.Collections.singleton;

public class SearchState {
    private final SortedMap<Specificity, Set<Node>> nodes = new TreeMap<Specificity, Set<Node>>(reverseOrder());

    private final TallyMap tallyMap;
    private final CcsLogger log;
    private final CcsContext ccsContext;
    private final Key key;

    private boolean constraintsChanged;

    private static final Comparator<CcsProperty> PROP_COMPARATOR =
            new Comparator<CcsProperty>() {
                public int compare(CcsProperty p1, CcsProperty p2) {
                    return p1.getPropertyNumber() - p2.getPropertyNumber();
                }
            };

    public SearchState(Node root, CcsContext ccsContext, CcsLogger log) {
        nodes.put(new Specificity(), singleton(root));
        tallyMap = new TallyMap();
        this.ccsContext = ccsContext;
        this.key = null;
        this.log = log;
    }

    private SearchState(TallyMap tallyMap, CcsContext ccsContext, Key key, CcsLogger log) {
        this.tallyMap = tallyMap;
        this.ccsContext = ccsContext;
        this.key = key;
        this.log = log;
    }

    public SearchState newChild(CcsContext ccsContext, Key key) {
        return new SearchState(new TallyMap(tallyMap), ccsContext, key, log);
    }

    public boolean extendWith(SearchState priorState) {
        constraintsChanged = false;
        for (Map.Entry<Specificity, Set<Node>> entry : priorState.nodes.entrySet())
            for (Node n : entry.getValue())
                n.getChildren(key, entry.getKey(), this);
        return constraintsChanged;
    }

    private String origins(List<CcsProperty> values) {
        StringBuilder b = new StringBuilder();
        boolean first = true;
        for (CcsProperty v : values) {
            if (!first) b.append(", ");
            b.append(v.getOrigin());
            first = false;
        }
        return b.toString();
    }

    public CcsProperty findProperty(String propertyName, boolean locals, boolean override) {
        for (Set<Node> ns : nodes.values()) {
            List<CcsProperty> values = new ArrayList<CcsProperty>();
            for (Node n : ns)
                for (CcsProperty p : n.getProperty(propertyName, locals))
                    if (p.isOverride() == override)
                        values.add(p);
            if (values.size() == 1)
                return values.get(0);
            else if (values.size() > 1) {
                Collections.sort(values, PROP_COMPARATOR);
                log.warn("Conflict detected for property: " + propertyName
                        + " in context [" + ccsContext.toString() + "]. "
                        + "Conflicting settings at: [" + origins(values) + "]. "
                        + "Using most recent value.");
                return values.get(values.size()-1);
            }
        }
        return null;
    }

    private Set<Node> getBucket(Specificity spec) {
        Set<Node> bucket = nodes.get(spec);
        if (bucket == null) {
            bucket = new HashSet<Node>();
            nodes.put(spec, bucket);
        }
        return bucket;
    }

    public void add(Specificity spec, Node node) {
        getBucket(spec).add(node);
    }

    public String getKey() {
        return key.toString();
    }

    public Tally.TallyState getTallyState(Tally.AndTally tally) {
        return tallyMap.getTallyState(tally);
    }

    public void setTallyState(Tally.AndTally tally, Tally.TallyState state) {
        tallyMap.setTallyState(tally, state);
    }

    public void constrain(Key constraints) {
        constraintsChanged |= key.addAll(constraints);
    }

    private class TallyMap {
        private final Map<Tally.AndTally, Tally.TallyState> tallies = new HashMap<Tally.AndTally, Tally.TallyState>();
        private final TallyMap next;

        private TallyMap() {
            next = null;
        }

        private TallyMap(TallyMap next) {
            this.next = next;
        }

        public Tally.TallyState getTallyState(Tally.AndTally tally) {
            if (tallies.containsKey(tally)) return tallies.get(tally);
            if (next != null) return next.getTallyState(tally);
            return new Tally.TallyState(tally);
        }

        public void setTallyState(Tally.AndTally tally, Tally.TallyState state) {
            tallies.put(tally, state);
        }
    }
}

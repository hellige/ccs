package net.immute.ccs;

import net.immute.ccs.dag.Key;
import net.immute.ccs.dag.Node;
import net.immute.ccs.dag.Tally;

import java.util.*;

import static java.util.Collections.reverseOrder;
import static java.util.Collections.singletonList;

public class SearchState {
    private final SortedMap<Specificity, List<Node>> nodes = new TreeMap<Specificity, List<Node>>(reverseOrder());
    private final TallyMap tallyMap;

    private static final Comparator<CcsProperty> PROP_COMPARATOR =
            new Comparator<CcsProperty>() {
                public int compare(CcsProperty p1, CcsProperty p2) {
                    return p1.getPropertyNumber() - p2.getPropertyNumber();
                }
            };

    public SearchState(Node root) {
        nodes.put(new Specificity(), singletonList(root));
        tallyMap = new TallyMap();
    }

    public SearchState(SearchState searchState) {
        tallyMap = new TallyMap(searchState.tallyMap);
    }

    public void extend(Key key, SearchContext context, boolean includeDirectChildren, SearchState nextState) {
        for (Map.Entry<Specificity, List<Node>> entry : nodes.entrySet())
            for (Node n : entry.getValue())
                n.getChildren(key, entry.getKey(), context, includeDirectChildren, nextState);
    }

    public CcsProperty findProperty(String propertyName, boolean locals) {
        for (List<Node> ns : nodes.values()) {
            List<CcsProperty> values = new ArrayList<CcsProperty>();
            for (Node n : ns)
                values.addAll(n.getProperty(propertyName, locals));
            if (values.size() == 1)
                return values.get(0);
            else if (values.size() > 1) {
                // TODO make warning optional and add info about conflicting settings
                CcsLogger.warn("Relying on declaration order to resolve ambiguity. Are you sure you want this?");
                return Collections.max(values, PROP_COMPARATOR);
            }
        }
        return null;
    }

    private List<Node> getBucket(Specificity spec) {
        List<Node> bucket = nodes.get(spec);
        if (bucket == null) {
            bucket = new ArrayList<Node>();
            nodes.put(spec, bucket);
        }
        return bucket;
    }

    public void add(Specificity spec, Node node) {
        getBucket(spec).add(node);
    }

    public Tally.TallyState getTallyState(Tally.AndTally tally) {
        return tallyMap.getTallyState(tally);
    }

    public void setTallyState(Tally.AndTally tally, Tally.TallyState state) {
        tallyMap.setTallyState(tally, state);
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

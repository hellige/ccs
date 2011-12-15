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
    private final CcsLogger log;
    private final SearchContext searchContext;

    private static final Comparator<CcsProperty> PROP_COMPARATOR =
            new Comparator<CcsProperty>() {
                public int compare(CcsProperty p1, CcsProperty p2) {
                    return p1.getPropertyNumber() - p2.getPropertyNumber();
                }
            };

    public SearchState(Node root, SearchContext searchContext, CcsLogger log) {
        nodes.put(new Specificity(), singletonList(root));
        tallyMap = new TallyMap();
        this.searchContext = searchContext;
        this.log = log;
    }

    private SearchState(TallyMap tallyMap, SearchContext searchContext, CcsLogger log) {
        this.tallyMap = tallyMap;
        this.searchContext = searchContext;
        this.log = log;
    }

    public SearchState newChild(SearchContext searchContext) {
        return new SearchState(new TallyMap(tallyMap), searchContext, log);
    }

    public void extend(Key key, SearchContext context, boolean includeDirectChildren, SearchState nextState) {
        for (Map.Entry<Specificity, List<Node>> entry : nodes.entrySet())
            for (Node n : entry.getValue())
                n.getChildren(key, entry.getKey(), context, includeDirectChildren, nextState);
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

    public CcsProperty findProperty(String propertyName, boolean locals) {
        for (List<Node> ns : nodes.values()) {
            List<CcsProperty> values = new ArrayList<CcsProperty>();
            for (Node n : ns)
                values.addAll(n.getProperty(propertyName, locals));
            if (values.size() == 1)
                return values.get(0);
            else if (values.size() > 1) {
                Collections.sort(values, PROP_COMPARATOR);
                log.warn("Conflict detected for property: " + propertyName
                        + " in context [" + searchContext.toString() + "]. "
                        + "Conflicting settings at: [" + origins(values) + "]. "
                        + "Using most recent value.");
                return values.get(values.size()-1);
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

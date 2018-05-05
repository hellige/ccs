package net.immute.ccs.impl.dag;

import net.immute.ccs.impl.SearchState;

// note: all comparisons of tallies should be by reference. don't implement equals/hashcode!
public abstract class Tally {
    protected final Node[] legs;
    protected final Node node;

    private Tally(Node[] legs, Node node) {
        this.legs = legs;
        this.node = node;
    }

    public abstract void activate(Node leg, Specificity spec, SearchState searchState);

    public Node getNode() {
        return node;
    }

    public int getSize() {
        return legs.length;
    }

    public Node getLeg(int i) {
        return legs[i];
    }

    public static class TallyState {
        private final AndTally tally;
        private final Specificity[] matches;
        private final boolean fullyMatched;

        public TallyState(AndTally tally) {
            this.tally = tally;
            this.matches = new Specificity[tally.getSize()];
            this.fullyMatched = false;
        }

        public TallyState(AndTally tally, Specificity[] newSpecs, boolean fullyMatched) {
            this.tally = tally;
            this.matches = newSpecs;
            this.fullyMatched = fullyMatched;
        }

        private TallyState activate(Node leg, Specificity spec) {
            boolean fullyMatched = true;
            Specificity[] newMatches = new Specificity[tally.getSize()];

            for (int i = 0; i < tally.getSize(); i++) {
                if (tally.getLeg(i) == leg) { // NB reference equality...
                    newMatches[i] = matches[i] == null || matches[i].lessThan(spec) ? spec : matches[i];
                } else {
                    newMatches[i] = matches[i];
                    if (matches[i] == null) fullyMatched = false;
                }
            }
            return new TallyState(tally, newMatches, fullyMatched);
        }

        private Specificity getSpecificity() {
            Specificity result = new Specificity();
            for (Specificity spec : matches) result = result.add(spec);
            return result;
        }
    }

    public static class AndTally extends Tally {
        public AndTally(Node node, Node[] legs) {
            super(legs, node);
        }

        @Override
        public void activate(Node leg, Specificity spec, SearchState searchState) {
            TallyState state = searchState.getTallyState(this).activate(leg, spec);
            searchState.setTallyState(this, state);
            // seems like this could lead to spurious warnings, but see comment below...
            if (state.fullyMatched) node.activate(state.getSpecificity(), searchState);
        }
    }

    public static class OrTally extends Tally {
        public OrTally(Node node, Node[] legs) {
            super(legs, node);
        }

        @Override
        public void activate(Node leg, Specificity spec, SearchState searchState) {
            // no state for or-joins, just re-activate node with the current specificity
            // it seems that this may allow spurious warnings, if multiple legs of the disjunction match with
            // same specificity. but this is detected in SearchState, where we keep a *set* of nodes for
            // each specificity, rather than, for example, a *list*.
            node.activate(spec, searchState);
        }
    }
}
package net.immute.ccs.dag;

import net.immute.ccs.SearchState;
import net.immute.ccs.Specificity;

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

    // TODO move...
    public static class TallyState {
        private final AndTally tally;
        private final boolean[] matched;
        private final Specificity[] specs;
        private final boolean fullyMatched;

        public TallyState(AndTally tally) {
            this.tally = tally;
            this.matched = new boolean[tally.getSize()];
            this.specs = new Specificity[tally.getSize()];
            this.fullyMatched = false;
        }

        public TallyState(AndTally tally, boolean[] newMatched, Specificity[] newSpecs, boolean fullyMatched) {
            this.tally = tally;
            this.matched = newMatched;
            this.specs = newSpecs;
            this.fullyMatched = fullyMatched;
        }

        private TallyState activate(Node leg, Specificity spec) {
            boolean fullyMatched = true;
            boolean[] newMatched = new boolean[tally.getSize()];
            Specificity[] newSpecs = new Specificity[tally.getSize()];

            for (int i = 0; i < tally.getSize(); i++) {
                if (tally.getLeg(i) == leg) { // NB reference equality...
                    newMatched[i] = true;
                    newSpecs[i] = spec; // TODO take the max!!!
                } else {
                    newMatched[i] = matched[i];
                    newSpecs[i] = specs[i];
                    if (!matched[i]) fullyMatched = false;
                }
            }
            return new TallyState(tally, newMatched, newSpecs, fullyMatched);
        }

        private Specificity getSpecificity() {
            Specificity result = new Specificity();
            for (Specificity spec : specs) result = result.add(spec);
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
            if (state.fullyMatched) node.activate(state.getSpecificity(), searchState);
        }

        private int getSize() {
            return legs.length;
        }

        private Node getLeg(int i) {
            return legs[i];
        }
    }

    public static class OrTally extends Tally {
        public OrTally(Node node, Node[] legs) {
            super(legs, node);
        }

        @Override
        public void activate(Node _, Specificity spec, SearchState searchState) {
            // no state for or-joins, just re-activate node with the current specificity
            // TODO this may allow spurious warnings, if multiple legs of the disjunction match with same specificity.
            // to detect this would require results to avoid duplicates and just take the max specificity, i suppose...
            node.activate(spec, searchState);
        }
    }
}
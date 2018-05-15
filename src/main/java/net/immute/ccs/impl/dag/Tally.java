package net.immute.ccs.impl.dag;

import net.immute.ccs.impl.SearchState;

// note: all comparisons of tallies should be by reference. don't implement equals/hashcode!
public abstract class Tally {
    final Node node;
    public final Node left;
    public final Node right;

    private Tally(Node node, Node left, Node right) {
        this.node = node;
        this.left = left;
        this.right = right;
    }

    abstract void activate(Node leg, Specificity spec, SearchState.Builder searchState);

    public Node getNode() {
        return node;
    }

    public static class TallyState {
        private final AndTally tally;
        private final Specificity leftMatch;
        private final Specificity rightMatch;

        public TallyState(AndTally tally) {
            this.tally = tally;
            this.leftMatch = null;
            this.rightMatch = null;
        }

        private TallyState(AndTally tally, Specificity leftMatch, Specificity rightMatch) {
            this.tally = tally;
            this.leftMatch = leftMatch;
            this.rightMatch = rightMatch;
        }

        public TallyState activate(Node leg, Specificity spec) {
            Specificity newLeftMatch = leftMatch;
            if (tally.left == leg && (leftMatch == null || leftMatch.lessThan(spec))) // NB reference equality...
                newLeftMatch = spec;
            Specificity newRightMatch = rightMatch;
            if (tally.right == leg && (rightMatch == null || rightMatch.lessThan(spec))) // NB reference equality...
                newRightMatch = spec;
            return new TallyState(tally, newLeftMatch, newRightMatch);
        }

        private Specificity getSpecificity() {
            Specificity result = new Specificity();
            if (leftMatch != null) result = result.add(leftMatch);
            if (rightMatch != null) result = result.add(rightMatch);
            return result;
        }

        boolean fullyMatched() {
            return leftMatch != null && rightMatch != null;
        }
    }

    public static class AndTally extends Tally {
        public AndTally(Node node, Node left, Node right) {
            super(node, left, right);
        }

        @Override
        public void activate(Node leg, Specificity spec, SearchState.Builder searchState) {
            TallyState state = searchState.activateTally(this, leg, spec);
            // seems like this could lead to spurious warnings, but see comment below...
            if (state.fullyMatched()) node.activate(state.getSpecificity(), searchState);
        }
    }

    public static class OrTally extends Tally {
        public OrTally(Node node, Node left, Node right) {
            super(node, left, right);
        }

        @Override
        public void activate(Node leg, Specificity spec, SearchState.Builder searchState) {
            // no state for or-joins, just re-activate node with the current specificity
            // it seems that this may allow spurious warnings, if multiple legs of the disjunction match with
            // same specificity. but this is detected in SearchState, where we keep a *set* of properties for
            // each specificity, rather than, for example, a *list*.
            node.activate(spec, searchState);
        }
    }
}
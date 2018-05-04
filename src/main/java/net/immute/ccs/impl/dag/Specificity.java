package net.immute.ccs.impl.dag;

public class Specificity {
    private final int values;
    private final int names;

    private Specificity(int names, int values) {
        this.names = names;
        this.values = values;
    }

    public Specificity() {
        values = 0;
        names = 0;
    }

    @SuppressWarnings("RedundantIfStatement")
    public boolean lessThan(Specificity s) {
        if (values < s.values) return true;
        if (values == s.values && names < s.names) return true;
        return false;
    }

    public String toString() {
        return "<" + values + ", " + names + ">";
    }

    public Specificity add(Specificity s) {
        return new Specificity(names + s.names, values + s.values);
    }

    Specificity incNames() {
        return new Specificity(names + 1, values);
    }

    Specificity incValues() {
        return new Specificity(names, values + 1);
    }
}

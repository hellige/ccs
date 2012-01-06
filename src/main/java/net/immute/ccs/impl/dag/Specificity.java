package net.immute.ccs.impl.dag;

public class Specificity implements Comparable<Specificity> {
    private final int values;
    private final int names;

    public Specificity(int names, int values) {
        this.names = names;
        this.values = values;
    }

    public Specificity() {
        values = 0;
        names = 0;
    }

    public int compareTo(Specificity s) {
        int result = values - s.values;
        if (result == 0) result = names - s.names;
        return result;
    }

    public String toString() {
        return "<" + values + ", " + names + ">";
    }

    public Specificity add(Specificity s) {
        return new Specificity(names + s.names, values + s.values);
    }

    public Specificity incElementNames() {
        return new Specificity(names + 1, values);
    }

    public Specificity incClassSelectors() {
        return new Specificity(names, values + 1);
    }
}

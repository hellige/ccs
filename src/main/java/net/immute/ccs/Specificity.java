package net.immute.ccs;

public class Specificity implements Comparable<Specificity> {
    private final int idSelectors;
    private final int classSelectors;
    private final int elementNames;

    public Specificity(int elementNames, int classSelectors, int idSelectors) {
        this.elementNames = elementNames;
        this.classSelectors = classSelectors;
        this.idSelectors = idSelectors;
    }

    public Specificity() {
        idSelectors = 0;
        classSelectors = 0;
        elementNames = 0;
    }

    public int compareTo(Specificity s) {
        int result = idSelectors - s.idSelectors;
        if (result == 0) {
            result = classSelectors - s.classSelectors;
        }
        if (result == 0) {
            result = elementNames - s.elementNames;
        }
        return result;
    }

    public String toString() {
        return "<" + idSelectors + ", " + classSelectors + ", " + elementNames
            + ">";
    }

    public Specificity add(Specificity s) {
        return new Specificity(elementNames + s.elementNames,
                classSelectors + s.classSelectors,
                idSelectors + s.idSelectors);
    }

    public Specificity incElementNames() {
        return new Specificity(elementNames + 1, classSelectors, idSelectors);
    }

    public Specificity incClassSelectors() {
        return new Specificity(elementNames, classSelectors + 1, idSelectors);
    }

    public Specificity incIdSelectors() {
        return new Specificity(elementNames, classSelectors, idSelectors + 1);
    }
}

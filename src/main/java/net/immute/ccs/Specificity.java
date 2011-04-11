package net.immute.ccs;

public class Specificity implements Comparable<Specificity> {
    private int idSelectors;
    private int classSelectors;
    private int elementNames;

    public void incClassSelectors() {
        classSelectors++;
    }

    public void incElementNames() {
        elementNames++;
    }

    public void incIdSelectors() {
        idSelectors++;
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
}

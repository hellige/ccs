package net.immute.ccs;

public class Specificity implements Comparable<Specificity> {
    /**
     * the number of ID selectors in the selector.
     */
    private int idSelectors;

    /**
     * the number of classes, attributes and pseudo-classes in the selector.
     */
    private int classSelectors;

    /**
     * the number of element names in the selector.
     */
    private int elementNames;

    public Specificity() {}

    public Specificity(int idSelectors, int classSelectors, int elementNames) {
        this.idSelectors = idSelectors;
        this.classSelectors = classSelectors;
        this.elementNames = elementNames;
    }

    /**
     * @return the classSelectors
     */
    public int getClassSelectors() {
        return classSelectors;
    }

    /**
     * @return the elementNames
     */
    public int getElementNames() {
        return elementNames;
    }

    /**
     * @return the idSelectors
     */
    public int getIdSelectors() {
        return idSelectors;
    }

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

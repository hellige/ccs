package net.immute.ccs;

public class CCSProperty {
    private String value;

    private Origin origin;

    private int propertyNumber;

    private Specificity specificity;

    public CCSProperty(String value, Origin origin, int propertyNumber,
        Specificity specificity) {
        this.value = value;
        this.origin = origin;
        this.propertyNumber = propertyNumber;
        this.specificity = specificity;
    }

    public String getValue() {
        return value;
    }

    public Origin getOrigin() {
        return origin;
    }

    public int getPropertyNumber() {
        return propertyNumber;
    }

    public Specificity getSpecificity() {
        return specificity;
    }

    public String toString() {
        return "{value: " + value + ", number: " + propertyNumber
            + ", specificity: " + specificity + ", origin: " + origin + "}";
    }
}

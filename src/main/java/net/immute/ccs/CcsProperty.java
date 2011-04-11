package net.immute.ccs;

public class CcsProperty {
    private final String value;
    private final Origin origin;
    private final int propertyNumber;

    public CcsProperty(String value, Origin origin, int propertyNumber) {
        this.value = value;
        this.origin = origin;
        this.propertyNumber = propertyNumber;
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

    public String toString() {
        return "{value: " + value + ", number: " + propertyNumber
            + ", origin: " + origin + "}";
    }
}

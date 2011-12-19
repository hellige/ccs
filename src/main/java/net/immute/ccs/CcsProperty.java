package net.immute.ccs;

public class CcsProperty {
    private final String value;
    private final Origin origin;
    private final int propertyNumber;
    private final boolean override;

    public CcsProperty(String value, Origin origin, int propertyNumber, boolean override) {
        this.value = value;
        this.origin = origin;
        this.propertyNumber = propertyNumber;
        this.override = override;
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

    public boolean isOverride() {
        return override;
    }

    public String toString() {
        return "{value: " + value + ", number: " + propertyNumber
            + ", origin: " + origin + "}";
    }
}

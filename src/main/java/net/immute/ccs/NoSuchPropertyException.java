package net.immute.ccs;

public class NoSuchPropertyException extends RuntimeException {
    private final String propertyName;

    public NoSuchPropertyException(String propertyName) {
        this.propertyName = propertyName;
    }

    public String getPropertyName() {
        return propertyName;
    }
}

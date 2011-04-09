package net.immute.ccs;

public class NoSuchPropertyException extends RuntimeException {
    private String propertyName;

    public NoSuchPropertyException(String propertyName) {
        this.propertyName = propertyName;
    }

    public String getPropertyName() {
        return propertyName;
    }
}

package net.immute.ccs;

public class NoSuchPropertyException extends RuntimeException {
    private final String propertyName;
    private final CcsContext ccsContext;

    public NoSuchPropertyException(String propertyName, CcsContext ccsContext) {
        super(String.format("Property '%s' (search path: %s)", propertyName, ccsContext));
        this.propertyName = propertyName;
        this.ccsContext = ccsContext;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public CcsContext getCcsContext() {
        return ccsContext;
    }
}

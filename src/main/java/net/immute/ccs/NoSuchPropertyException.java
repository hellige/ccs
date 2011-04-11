package net.immute.ccs;

public class NoSuchPropertyException extends RuntimeException {
    private final String propertyName;
    private final SearchContext searchContext;

    public NoSuchPropertyException(String propertyName, SearchContext searchContext) {
        super(String.format("Property '%s' (search path: %s)", propertyName, searchContext));
        this.propertyName = propertyName;
        this.searchContext = searchContext;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public SearchContext getSearchContext() {
        return searchContext;
    }
}

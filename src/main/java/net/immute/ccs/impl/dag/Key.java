package net.immute.ccs.impl.dag;

import java.util.*;
import java.util.regex.Pattern;

// TODO refactor this to make it much simpler: key-value pair
public class Key {
    private final static Pattern identRegex = Pattern.compile("^[A-Za-z$_][A-Za-z0-9$_]*$");

    private final String name;
    private final String value;

    private Specificity specificity = new Specificity();

    public Key(String name, String value) {
        this.name = name;
        this.value = value;
        specificity.incNames(); // TODO specificity now ignores value vs. name match. this is an experiment.
    }

    public Specificity getSpecificity() {
        return specificity;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append(name);
        if (value != null) {
            if (identRegex.matcher(value).matches())
                result.append('.').append(value);
            else
                result.append(".'").append(value.replace("'", "\\'")).append('\'');
        }
        return result.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Key key = (Key) o;
        return Objects.equals(name, key.name) &&
                Objects.equals(value, key.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value);
    }
}

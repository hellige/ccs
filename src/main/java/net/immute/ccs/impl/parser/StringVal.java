package net.immute.ccs.impl.parser;

import java.util.ArrayList;

public class StringVal {
    private static interface StringElem {
        String interpolate();
    }

    private static class Literal implements StringElem {
        private final String value;

        private Literal(String value) {
            this.value = value;
        }

        @Override
        public String interpolate() {
            return value;
        }
    }

    private static class Interpolant implements StringElem {
        private final String key;

        private Interpolant(String key) {
            this.key = key;
        }

        @Override
        public String interpolate() {
            String value = System.getenv(key);
            return value == null ? "" : value;
        }
    }

    private final ArrayList<StringElem> elements = new ArrayList<>();

    StringVal() {}

    StringVal(String str) {
        elements.add(new Literal(str));
    }

    String str() {
        StringBuilder str = new StringBuilder();
        for (StringElem element : elements) str.append(element.interpolate());
        return str.toString();
    }

    boolean interpolation() {
        if (elements.size() > 1) return true;
        if (elements.get(0) instanceof Interpolant) return true;
        return false;
    }

    void addLiteral(CharSequence interpolant) {
        elements.add(new Literal(interpolant.toString()));
    }

    void addInterpolant(CharSequence interpolant) {
        elements.add(new Interpolant(interpolant.toString()));
    }

    @Override
    public String toString() {
        return str();
    }
};


package net.immute.ccs.impl.parser;

public class Value<T> {
    private final T t;

    public Value(T t) {
        this.t = t;
    }

    public T get() {
        return t;
    }

    @Override
    public String toString() {
        return t.toString();
    }
}

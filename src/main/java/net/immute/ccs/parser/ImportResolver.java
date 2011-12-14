package net.immute.ccs.parser;

import java.io.InputStream;

public interface ImportResolver {
    InputStream resolve(String uri);

    public class Null implements ImportResolver {
        @Override public InputStream resolve(String uri) {
            throw new UnsupportedOperationException("No import resolver provided! Cannot resolve: '" + uri + "'");
        }
    }
}

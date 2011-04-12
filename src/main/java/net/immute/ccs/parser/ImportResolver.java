package net.immute.ccs.parser;

import java.io.InputStream;

public interface ImportResolver {
    InputStream resolve(String uri);
}

package net.immute.ccs;

import net.immute.ccs.parser.ImportResolver;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;

public class ImportTest {
    private SearchContext load(String name) throws IOException {
        ImportResolver resolver = new ImportResolver() {
            @Override
            public InputStream resolve(String uri) {
                return getClass().getResourceAsStream("/"+uri);
            }
        };
        return new CcsDomain()
                .loadCcsStream(resolver.resolve(name), name, resolver)
                .build();
    }

    @Test
    public void testImport() throws Exception {
        SearchContext root = load("import.ccs");
        SearchContext c = new SearchContext(root, "first");
        assertEquals("correct", c.getString("test"));
    }

    @Test
    public void testNestedImport() throws Exception {
        SearchContext root = load("nested-import.ccs");
        SearchContext c = new SearchContext(root, "first");
        assertEquals("outer", c.getString("test"));
        c = new SearchContext(c, "first");
        assertEquals("correct", c.getString("test"));
    }

    @Test
    public void testImportOrder1() throws Exception {
        SearchContext root = load("import-order1.ccs");
        SearchContext c = new SearchContext(root, "first");
        assertEquals("correct", c.getString("test"));
    }

    @Test
    public void testImportOrder2() throws Exception {
        SearchContext root = load("import-order2.ccs");
        SearchContext c = new SearchContext(root, "first");
        assertEquals("third", c.getString("test"));
    }
}

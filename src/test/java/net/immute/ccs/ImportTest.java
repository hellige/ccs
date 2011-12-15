package net.immute.ccs;

import net.immute.ccs.dag.Node;
import net.immute.ccs.parser.ImportResolver;
import net.immute.ccs.parser.Loader;
import org.junit.Test;

import java.io.InputStream;

import static org.junit.Assert.assertEquals;

public class ImportTest {
    @Test
    public void testImport() throws Exception {
        Loader loader = new Loader();
        ImportResolver resolver = new ImportResolver() {
            @Override
            public InputStream resolve(String uri) {
                return getClass().getResourceAsStream("/"+uri);
            }
        };
        Node root = loader.loadCcsStream(getClass().getResourceAsStream("/import.ccs"), "import.ccs", resolver);
        SearchContext c = new SearchContext(root, "first");
        assertEquals("correct", c.getString("test"));
    }

    @Test
    public void testNestedImport() throws Exception {
        Loader loader = new Loader();
        ImportResolver resolver = new ImportResolver() {
            @Override
            public InputStream resolve(String uri) {
                return getClass().getResourceAsStream("/"+uri);
            }
        };
        Node root = loader.loadCcsStream(getClass().getResourceAsStream("/nested-import.ccs"), "import.ccs", resolver);
        SearchContext c = new SearchContext(root, "first");
        assertEquals("outer", c.getString("test"));
        c = new SearchContext(c, "first");
        assertEquals("correct", c.getString("test"));
    }

    @Test
    public void testImportOrder1() throws Exception {
        Loader loader = new Loader();
        ImportResolver resolver = new ImportResolver() {
            @Override
            public InputStream resolve(String uri) {
                return getClass().getResourceAsStream("/"+uri);
            }
        };
        Node root = loader.loadCcsStream(getClass().getResourceAsStream("/import-order1.ccs"), "import.ccs", resolver);
        SearchContext c = new SearchContext(root, "first");
        assertEquals("correct", c.getString("test"));
    }

    @Test
    public void testImportOrder2() throws Exception {
        Loader loader = new Loader();
        ImportResolver resolver = new ImportResolver() {
            @Override
            public InputStream resolve(String uri) {
                return getClass().getResourceAsStream("/"+uri);
            }
        };
        Node root = loader.loadCcsStream(getClass().getResourceAsStream("/import-order2.ccs"), "import.ccs", resolver);
        SearchContext c = new SearchContext(root, "first");
        assertEquals("third", c.getString("test"));
    }
}

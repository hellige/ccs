package net.immute.ccs;

import net.immute.ccs.oldparser.Loader;
import net.immute.ccs.parser.ImportResolver;
import net.immute.ccs.tree.Node;
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
}

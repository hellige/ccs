package net.immute.ccs;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;

public class ImportTest {
    private CcsContext load(String name) throws IOException {
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
        CcsContext c = load("import.ccs").constrain("first");
        assertEquals("correct", c.getString("test"));
    }

    @Test
    public void testNestedImport() throws Exception {
        CcsContext c = load("nested-import.ccs").constrain("first");
        assertEquals("outer", c.getString("test"));
        c = c.constrain("first");
        assertEquals("correct", c.getString("test"));
    }

    @Test
    public void testImportOrder1() throws Exception {
        CcsContext c = load("import-order1.ccs").constrain("first");
        assertEquals("correct", c.getString("test"));
    }

    @Test
    public void testImportOrder2() throws Exception {
        CcsContext c = load("import-order2.ccs").constrain("first");
        assertEquals("third", c.getString("test"));
    }
}

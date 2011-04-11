package net.immute.ccs;

import net.immute.ccs.parser.Loader;
import net.immute.ccs.tree.CCSNode;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FunctionalTest {
    @Test
    public void testCcs() throws Exception {
        Loader loader = new Loader();
        CCSNode root = loader.loadCCSStream(getClass().getResourceAsStream("/test.ccs"), "test.ccs");

        SearchContext c = new SearchContext(root, "hi");
        c = new SearchContext(c, "body");
        c = new SearchContext(c, "bar");
        c = new SearchContext(c, "em");
        SearchContext c2 = new SearchContext(c, "baz");
        assertEquals("hi", c.getProperty("foo"));
        assertEquals("there", c2.getProperty("foo"));
        assertEquals("hi", c.getProperty("foo"));
        SearchContext c3 = new SearchContext(c, "blah");
        assertEquals("hi", c3.getProperty("foo"));
        SearchContext c4 = new SearchContext(c2, "em");
        assertEquals("!", c4.getProperty("foo"));

        c = new SearchContext(root, "hi");
        c = new SearchContext(c, "body", "doit");
        c = new SearchContext(c, "bar");
        c = new SearchContext(c, "em", null, "foo");
        c2 = new SearchContext(c, "baz");
        assertEquals("hi", c.getProperty("foo"));
        assertEquals("there", c2.getProperty("foo"));
        assertEquals("hi", c.getProperty("foo"));
        c3 = new SearchContext(c, "blah");
        assertEquals("hi", c3.getProperty("foo"));
        c4 = new SearchContext(c2, "em");
        assertEquals("ARGH", c4.getProperty("foo"));
        assertEquals("WTF", c4.getProperty("rootTest"));
        assertEquals("c", c4.getProperty("childTest"));
        assertEquals("b", c4.getProperty("classTest"));
    }
}

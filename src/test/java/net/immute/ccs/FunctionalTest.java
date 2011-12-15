package net.immute.ccs;

import net.immute.ccs.dag.Node;
import net.immute.ccs.parser.Loader;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FunctionalTest {
    @Test
    public void testCcs() throws Exception {
        Loader loader = new Loader();
        Node root = loader.loadCcsStream(getClass().getResourceAsStream("/test.ccs"), "test.ccs");

        SearchContext c = new SearchContext(root, "hi");
        c = new SearchContext(c, "body");
        c = new SearchContext(c, "bar");
        c = new SearchContext(c, "em");
        SearchContext c2 = new SearchContext(c, "baz");
        assertEquals("hi", c.getString("foo"));
        assertEquals("there", c2.getString("foo"));
        assertEquals("hi", c.getString("foo"));
        SearchContext c3 = new SearchContext(c, "blah");
        assertEquals("hi", c3.getString("foo"));
        SearchContext c4 = new SearchContext(c2, "em");
        assertEquals("!", c4.getString("foo"));

        c = new SearchContext(root, "hi");
        c = new SearchContext(c, "body", "doit");
        c = new SearchContext(c, "bar");
        c = new SearchContext(c, "em", null, "foo");
        c2 = new SearchContext(c, "baz");
        assertEquals("hi", c.getString("foo"));
        assertEquals("there", c2.getString("foo"));
        assertEquals("hi", c.getString("foo"));
        c3 = new SearchContext(c, "blah");
        assertEquals("hi", c3.getString("foo"));
        c4 = new SearchContext(c2, "em");
        assertEquals("ARGH", c4.getString("foo"));
        assertEquals("WTF", c4.getString("rootTest"));
        assertEquals("c", c4.getString("childTest"));
        assertEquals("b", c4.getString("classTest"));
    }
}

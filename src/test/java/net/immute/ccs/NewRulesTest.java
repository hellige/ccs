package net.immute.ccs;

import net.immute.ccs.parser.Loader;
import net.immute.ccs.tree.CCSNode;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class NewRulesTest {
    private CCSNode load(String name) throws IOException {
        Loader loader = new Loader();
        return loader.loadCCSStream(getClass().getResourceAsStream("/" + name), name);
    }

    @Test
    public void testClosestMatch() throws Exception {
        CCSNode root = load("closest-match.ccs");
        SearchContext c = new SearchContext(root, "first");
        c = new SearchContext(c, "second");
        c = new SearchContext(c, "third");
        assertEquals("correct", c.getProperty("test"));
    }

    @Test
    public void testBestBeforeClosest() throws Exception {
        CCSNode root = load("best-before-closest.ccs");
        SearchContext c = new SearchContext(root, "first");
        c = new SearchContext(c, "second", "id");
        assertEquals("correct", c.getProperty("test"));
    }

    @Test
    public void testTiedSpecificities() throws Exception {
        CCSNode root = load("tied-specificities.ccs");
        SearchContext c = new SearchContext(root, "first");
        c = new SearchContext(c, "second", null, "class1", "class2");
        assertEquals("correct", c.getProperty("test"));
    }

    @Test
    public void testComplexTie() throws Exception {
        CCSNode root = load("complex-tie.ccs");
        SearchContext c = new SearchContext(root, "root");
        c = new SearchContext(c, "bar", null, "class1", "class2");
        assertEquals("correct", c.getProperty("test1"));
        c = new SearchContext(c, "foo");
        assertEquals("correct", c.getProperty("test2"));
    }
}

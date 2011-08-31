package net.immute.ccs;

import net.immute.ccs.parser.Loader;
import net.immute.ccs.tree.Node;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class NewRulesTest {
    private Node load(String name) throws IOException {
        Loader loader = new Loader();
        return loader.loadCcsStream(getClass().getResourceAsStream("/" + name), name);
    }

    @Test
    public void testClosestMatch() throws Exception {
        Node root = load("closest-match.ccs");
        SearchContext c = new SearchContext(root, "first");
        c = new SearchContext(c, "second");
        c = new SearchContext(c, "third");
        assertEquals("correct", c.getString("test"));
    }

    @Test
    public void testBestBeforeClosest() throws Exception {
        Node root = load("best-before-closest.ccs");
        SearchContext c = new SearchContext(root, "first");
        c = new SearchContext(c, "second", "id");
        assertEquals("correct", c.getString("test"));
    }

    @Test
    public void testTiedSpecificities() throws Exception {
        Node root = load("tied-specificities.ccs");
        SearchContext c = new SearchContext(root, "first");
        c = new SearchContext(c, "second", null, "class1", "class2");
        assertEquals("correct", c.getString("test"));
    }

    @Test
    public void testComplexTie() throws Exception {
        Node root = load("complex-tie.ccs");
        SearchContext c = new SearchContext(root, "root");
        c = new SearchContext(c, "bar", null, "class1", "class2");
        assertEquals("correct", c.getString("test1"));
        c = new SearchContext(c, "foo");
        assertEquals("correct", c.getString("test2"));
    }

    @Test
    @Ignore("relies on 'important!'")
    public void testOuterOverride() throws Exception {
        Node root = load("outer-override.ccs");
        SearchContext c = new SearchContext(root, "root");
        c = new SearchContext(c, "env", "dev");
        c = new SearchContext(c, "user", "me");
        c = new SearchContext(c, "foo");
        assertEquals("correct", c.getString("test"));
    }
}

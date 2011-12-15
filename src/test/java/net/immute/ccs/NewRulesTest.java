package net.immute.ccs;

import net.immute.ccs.parser.Loader;
import net.immute.ccs.dag.Node;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class NewRulesTest {
    private Node load(String name) throws IOException {
        Loader loader = new Loader();
        return loader.loadCcsStream(getClass().getResourceAsStream("/" + name), name);
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

    @Test
    public void testConjunction() throws Exception {
        Node root = load("conjunction.ccs");
        SearchContext c = new SearchContext(root, "root");
        c = new SearchContext(c, "a");
        c = new SearchContext(c, "b");
        assertEquals("correct1", c.getString("test"));
        SearchContext c2 = new SearchContext(c, "c", null, "class1");
        assertEquals("correct2", c2.getString("test"));

        c2 = new SearchContext(c, "d");
        assertEquals("correct3", c2.getString("test"));
    }

    @Test
    public void testDisjunction() throws Exception {
        Node root = load("disjunction.ccs");
        SearchContext c = new SearchContext(root, "root");
        c = new SearchContext(c, "a");
        c = new SearchContext(c, "b");
        c = new SearchContext(c, "c");
        assertEquals("correct1", c.getString("test"));
        c = new SearchContext(c, "c");
        assertEquals("correct2", c.getString("test"));
        c = new SearchContext(c, "c", null, "b");
        assertEquals("correct1", c.getString("test"));
    }

    @Test
    public void testContext() throws Exception {
        Node root = load("context.ccs");
        SearchContext c = new SearchContext(root, "root");
        c = new SearchContext(c, "b");
        c = new SearchContext(c, "a");
        assertEquals("correct1", c.getString("test"));
        c = new SearchContext(c, "b");
        c = new SearchContext(c, "a");
        assertEquals("correct2", c.getString("test"));
    }

    @Test
    public void testTrailingCombinator() throws Exception {
        Node root = load("trailing-combinator.ccs");
        SearchContext c = new SearchContext(root, "root");
        c = new SearchContext(c, "b");
        c = new SearchContext(c, "a");
        assertEquals("bottom", c.getString("test"));
    }

    @Test
    public void testDirectChild() throws Exception {
        Node root = load("direct-child.ccs");
        SearchContext c = new SearchContext(root, "root");
        c = new SearchContext(c, "a");
        c = new SearchContext(c, "b");
        assertEquals("inner", c.getString("test"));

        c = new SearchContext(root, "root");
        c = new SearchContext(c, "a");
        c = new SearchContext(c, "c");
        c = new SearchContext(c, "b");
        assertEquals("outer", c.getString("test"));
    }
}

package net.immute.ccs;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class NewRulesTest {
    private SearchContext load(String name) throws IOException {
        return new CcsDomain().loadCcsStream(getClass().getResourceAsStream("/" + name), name).build();
    }

    @Test
    public void oldFunctionalTest() throws Exception {
        SearchContext root = load("test.ccs");

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

    @Test
    public void testBestBeforeClosest() throws Exception {
        SearchContext c = load("best-before-closest.ccs");
        c = new SearchContext(c, "first");
        c = new SearchContext(c, "second", "id");
        assertEquals("correct", c.getString("test"));
    }

    @Test
    public void testTiedSpecificities() throws Exception {
        SearchContext c = load("tied-specificities.ccs");
        c = new SearchContext(c, "first");
        c = new SearchContext(c, "second", null, "class1", "class2");
        assertEquals("correct", c.getString("test"));
    }

    @Test
    public void testComplexTie() throws Exception {
        SearchContext c = load("complex-tie.ccs");
        c = new SearchContext(c, "bar", null, "class1", "class2");
        assertEquals("correct", c.getString("test1"));
        c = new SearchContext(c, "foo");
        assertEquals("correct", c.getString("test2"));
    }

    @Test
    public void testConjSpecificities() throws Exception {
        SearchContext c = load("conj-specificities.ccs");
        c = new SearchContext(c, "a");
        c = new SearchContext(c, "b", "b");
        c = new SearchContext(c, "c", "c");
        assertEquals("correct", c.getString("test"));
    }

    @Test
    public void testDisjSpecificities() throws Exception {
        SearchContext c = load("disj-specificities.ccs");
        c = new SearchContext(c, "a");
        c = new SearchContext(c, "b");
        assertEquals("correct1", c.getString("test"));
        c = new SearchContext(c, "x", "c");
        assertEquals("correct2", c.getString("test"));
        c = new SearchContext(c, "y", "d");
        assertEquals("correct1", c.getString("test"));

        c = new SearchContext(c, "f", "f");
        c = new SearchContext(c, "g");
        c = new SearchContext(c, "h", "k");
        assertEquals("correct3", c.getString("test"));
    }

    @Test
    public void testConjunction() throws Exception {
        SearchContext c = load("conjunction.ccs");
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
        SearchContext c = load("disjunction.ccs");
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
        SearchContext c = load("context.ccs");
        c = new SearchContext(c, "b");
        c = new SearchContext(c, "a");
        assertEquals("correct1", c.getString("test"));
        c = new SearchContext(c, "b");
        c = new SearchContext(c, "a");
        assertEquals("correct2", c.getString("test"));
    }

    @Test
    public void testTrailingCombinator() throws Exception {
        SearchContext c = load("trailing-combinator.ccs");
        c = new SearchContext(c, "b");
        c = new SearchContext(c, "a");
        assertEquals("bottom", c.getString("test"));

        SearchContext c2 = new SearchContext(c, "e");
        c2 = new SearchContext(c2, "c");
        c2 = new SearchContext(c2, "b");
        c2 = new SearchContext(c2, "d");
        assertEquals("bottom", c2.getString("test2"));
    }

    @Test
    public void testDirectChild() throws Exception {
        SearchContext root = load("direct-child.ccs");
        SearchContext
                c = new SearchContext(root, "a");
        c = new SearchContext(c, "b");
        assertEquals("inner", c.getString("test"));

        c = new SearchContext(root, "root");
        c = new SearchContext(c, "a");
        c = new SearchContext(c, "c");
        c = new SearchContext(c, "b");
        assertEquals("outer", c.getString("test"));
    }

    @Test
    public void testLocalBeatsHeritable() throws Exception {
        SearchContext c = load("local-beats-heritable.ccs");
        c = new SearchContext(c, "a");
        assertEquals("local", c.getString("test"));
        c = new SearchContext(c, "b");
        assertEquals("heritable", c.getString("test"));
    }

    @Test
    public void testOrderDependentDisj() throws Exception {
        SearchContext root = load("order-dependent-disj.ccs");
        SearchContext c = new SearchContext(root, "b");
        assertEquals(2, c.getInt("x"));

        c = new SearchContext(root, "a");
        c = new SearchContext(c, "c");
        c = new SearchContext(c, "d");
        assertEquals(1, c.getInt("x"));

        c = new SearchContext(root, "d");
        c = new SearchContext(c, "f");
        assertEquals(2, c.getInt("y"));
    }
}

package net.immute.ccs;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class NewRulesTest {
    private CcsContext load(String name) throws IOException {
        return new CcsDomain().loadCcsStream(getClass().getResourceAsStream("/" + name), name).build();
    }

    @Test
    public void oldFunctionalTest() throws Exception {
        CcsContext root = load("test.ccs");

        CcsContext c = root.constrain("hi");
        c = c.constrain("body");
        c = c.constrain("bar");
        c = c.constrain("em");
        CcsContext c2 = c.constrain("baz");
        assertEquals("hi", c.getString("foo"));
        assertEquals("there", c2.getString("foo"));
        assertEquals("hi", c.getString("foo"));
        CcsContext c3 = c.constrain("blah");
        assertEquals("hi", c3.getString("foo"));
        CcsContext c4 = c2.constrain("em");
        assertEquals("!", c4.getString("foo"));

        c = root.constrain("hi");
        c = c.constrain("body", "doit");
        c = c.constrain("bar");
        c = c.constrain("em", null, "foo");
        c2 = c.constrain("baz");
        assertEquals("hi", c.getString("foo"));
        assertEquals("there", c2.getString("foo"));
        assertEquals("hi", c.getString("foo"));
        c3 = c.constrain("blah");
        assertEquals("hi", c3.getString("foo"));
        c4 = c2.constrain("em");
        assertEquals("ARGH", c4.getString("foo"));
        assertEquals("WTF", c4.getString("rootTest"));
        assertEquals("c", c4.getString("childTest"));
        assertEquals("b", c4.getString("classTest"));
    }

    @Test
    public void testBestBeforeClosest() throws Exception {
        CcsContext c = load("best-before-closest.ccs");
        c = c.constrain("first");
        c = c.constrain("second", "id");
        assertEquals("correct", c.getString("test"));
    }

    @Test
    public void testTiedSpecificities() throws Exception {
        CcsContext c = load("tied-specificities.ccs");
        c = c.constrain("first");
        c = c.constrain("second", null, "class1", "class2");
        assertEquals("correct", c.getString("test"));
    }

    @Test
    public void testComplexTie() throws Exception {
        CcsContext c = load("complex-tie.ccs");
        c = c.constrain("bar", null, "class1", "class2");
        assertEquals("correct", c.getString("test1"));
        c = c.constrain("foo");
        assertEquals("correct", c.getString("test2"));
    }

    @Test
    public void testConjSpecificities() throws Exception {
        CcsContext c = load("conj-specificities.ccs");
        c = c.constrain("a");
        c = c.constrain("b", "b");
        c = c.constrain("c", "c");
        assertEquals("correct", c.getString("test"));
    }

    @Test
    public void testDisjSpecificities() throws Exception {
        CcsContext c = load("disj-specificities.ccs");
        c = c.constrain("a");
        c = c.constrain("b");
        assertEquals("correct1", c.getString("test"));
        c = c.constrain("x", "c");
        assertEquals("correct2", c.getString("test"));
        c = c.constrain("y", "d");
        assertEquals("correct1", c.getString("test"));

        c = c.constrain("f", "f");
        c = c.constrain("g");
        c = c.constrain("h", "k");
        assertEquals("correct3", c.getString("test"));
    }

    @Test
    public void testConjunction() throws Exception {
        CcsContext c = load("conjunction.ccs");
        c = c.constrain("a");
        c = c.constrain("b");
        assertEquals("correct1", c.getString("test"));
        CcsContext c2 = c.constrain("c", null, "class1");
        assertEquals("correct2", c2.getString("test"));

        c2 = c.constrain("d");
        assertEquals("correct3", c2.getString("test"));
    }

    @Test
    public void testDisjunction() throws Exception {
        CcsContext c = load("disjunction.ccs");
        c = c.constrain("a");
        c = c.constrain("b");
        c = c.constrain("c");
        assertEquals("correct1", c.getString("test"));
        c = c.constrain("c");
        assertEquals("correct2", c.getString("test"));
        c = c.constrain("c", null, "b");
        assertEquals("correct1", c.getString("test"));
    }

    @Test
    public void testContext() throws Exception {
        CcsContext c = load("context.ccs");
        c = c.constrain("b");
        c = c.constrain("a");
        assertEquals("correct1", c.getString("test"));
        c = c.constrain("b");
        c = c.constrain("a");
        assertEquals("correct2", c.getString("test"));
    }

    @Test
    public void testTrailingCombinator() throws Exception {
        CcsContext c = load("trailing-combinator.ccs");
        c = c.constrain("b");
        c = c.constrain("a");
        assertEquals("bottom", c.getString("test"));

        CcsContext c2 = c.constrain("e");
        c2 = c2.constrain("c");
        c2 = c2.constrain("b");
        c2 = c2.constrain("d");
        assertEquals("bottom", c2.getString("test2"));
    }

    @Test
    public void testDirectChild() throws Exception {
        CcsContext root = load("direct-child.ccs");
        CcsContext c = root.constrain("a");
        c = c.constrain("b");
        assertEquals("inner", c.getString("test"));

        c = root.constrain("root");
        c = c.constrain("a");
        c = c.constrain("c");
        c = c.constrain("b");
        assertEquals("outer", c.getString("test"));
    }

    @Test
    public void testLocalBeatsHeritable() throws Exception {
        CcsContext c = load("local-beats-heritable.ccs");
        c = c.constrain("a");
        assertEquals("local", c.getString("test"));
        c = c.constrain("b");
        assertEquals("heritable", c.getString("test"));
    }

    @Test
    public void testOrderDependentDisj() throws Exception {
        CcsContext root = load("order-dependent-disj.ccs");
        CcsContext c = root.constrain("b");
        assertEquals(2, c.getInt("x"));

        c = root.constrain("a");
        c = c.constrain("c");
        c = c.constrain("d");
        assertEquals(1, c.getInt("x"));

        c = root.constrain("d");
        c = c.constrain("f");
        assertEquals(2, c.getInt("y"));
    }
}

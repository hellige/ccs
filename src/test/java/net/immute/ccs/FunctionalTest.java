package net.immute.ccs;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStreamReader;

import static org.junit.Assert.assertEquals;

public class FunctionalTest {
    private CcsContext load(String name) throws IOException {
        return new CcsDomain().loadCcsStream(new InputStreamReader(getClass().getResourceAsStream("/" + name)), name)
                .build();
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
        c = c.constrain("second", "class1", "class2");
        assertEquals("correct", c.getString("test"));
    }

    @Test
    public void testComplexTie() throws Exception {
        CcsContext c = load("complex-tie.ccs");
        c = c.constrain("bar", "class1", "class2");
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
        CcsContext c2 = c.constrain("c", "class1");
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
        c = c.constrain("c", "b");
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
    public void testOrderDependentDisj() throws Exception {
        CcsContext root = load("order-dependent-disj.ccs");
        CcsContext c = root.constrain("b");
        assertEquals(2, c.getInt("x"));

        c = root.constrain("a");
        c = c.constrain("c");
        c = c.constrain("d");
        assertEquals(3, c.getInt("x"));

        c = root.constrain("d");
        c = c.constrain("f");
        assertEquals(2, c.getInt("y"));
    }

    @Test
    public void testOverride() throws Exception {
        CcsContext root = load("override.ccs");
        CcsContext c = root.constrain("a", "b", "c").constrain("d");
        assertEquals("correct", c.getString("test"));
    }

    @Test
    public void testSameStep() throws Exception {
        CcsContext root = load("same-step.ccs");
        CcsContext c = root.constrain("a", "b", "c").constrain("d", "e");
        assertEquals("nope", c.getString("test"));
        c = root.builder().add("a", "b", "c").add("d", "e").build();
        assertEquals("yep", c.getString("test"));
    }

    @Test
    public void testConstraintsInCcs() throws Exception {
        CcsContext root = load("constraints-in-ccs.ccs");
        CcsContext c = root.constrain("a", "b");
        assertEquals("correct", c.getString("test"));
    }

    @Test
    public void testGetLong() throws IOException {
        CcsContext root = load("types.ccs");
        long number = root.getLong("number");
        assertEquals(100L, number);
    }

    @Test
    public void testGetLongWithDefault() throws IOException {
        CcsContext root = load("types.ccs");
        long number = root.getLong("notPresent", 100L);
        assertEquals(100L, number);
    }

    @Test(expected = NumberFormatException.class)
    public void testGetIntExplodes() throws IOException {
        CcsContext root = load("types.ccs");
        int number = root.getInt("longNumber");
    }

    @Test
    public void testGetLongDoesNotExplode() throws IOException {
        CcsContext root = load("types.ccs");
        long number = root.getLong("longNumber");
        assertEquals(1000000000000L, number);
    }
}

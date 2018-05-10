package net.immute.ccs;

import net.immute.ccs.impl.Hamt;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class HamtTest {
    @Test
    public void basicUpdates() {
        Hamt<Integer, String> hamt = new Hamt<>();
        Hamt<Integer, String> withTen = hamt.update(10, v -> "test");
        assertEquals("test", withTen.get(10));
        assertEquals("test2", withTen.update(10, v -> v+"2").get(10));
        assertEquals("test", withTen.get(10));
        assertNull(hamt.get(10));
    }

    @Test
    public void nodeExpansion() {
        int k1 = 2048 + 10;
        int k2 = 10;
        // verify that these will collide in the first level...
        assertEquals(Integer.valueOf(k1).hashCode() & 0x1f, Integer.valueOf(k2).hashCode() & 0x1f);

        Hamt<Integer, String> hamt = new Hamt<Integer, String>()
                .update(k1, v -> "test")
                .update(k2, v -> "test2");
        assertEquals("test", hamt.get(k1));
        assertEquals("test2", hamt.get(k2));
    }
}

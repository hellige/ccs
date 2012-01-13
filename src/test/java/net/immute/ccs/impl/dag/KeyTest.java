package net.immute.ccs.impl.dag;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class KeyTest {
    @Test
    public void toStringTest() {
        Key k = new Key("test", "a.b", "c'd", "d\"e", "f.g'h", "i.j\"k");
        assertEquals("test.'a.b'.'d\"e'.'i.j\"k'.'c\\'d'.'f.g\\'h'", k.toString());
    }
}

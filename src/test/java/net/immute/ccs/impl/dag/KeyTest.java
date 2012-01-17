package net.immute.ccs.impl.dag;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class KeyTest {
    @Test
    public void toStringTest() {
        Key k = new Key("test", "a.b", "c'd", "d\"e", "f.g'h", "i.j\"k");
        assertEquals("test.'a.b'.'c\\'d'.'d\"e'.'f.g\\'h'.'i.j\"k'", k.toString());
    }
}

package net.immute.ccs;

import java.io.IOException;

import net.immute.ccs.parser.Loader;
import net.immute.ccs.tree.CCSNode;

public class Main {

    /**
     * @param args
     */
    public static void main(String[] args) throws IOException {
        Loader loader = new Loader();
        CCSNode root =
            loader.loadCCSStream(Main.class.getResourceAsStream("test.css"),
                "test.css");

        SearchContext c = new SearchContext(root, "hi");
        c = new SearchContext(c, "body");
        c = new SearchContext(c, "bar");
        c = new SearchContext(c, "em");
        SearchContext c2 = new SearchContext(c, "baz");
        System.err.printf("c's foo is %s.\n", c.getProperty("foo"));
        System.err.printf("c2's foo is %s.\n", c2.getProperty("foo"));
        System.err.printf("c's foo is %s.\n", c.getProperty("foo"));
        SearchContext c3 = new SearchContext(c, "blah");
        System.err.printf("c3's foo is %s.\n", c3.getProperty("foo"));
        SearchContext c4 = new SearchContext(c2, "em");
        System.err.printf("c4's foo is %s.\n", c4.getProperty("foo"));

        c = new SearchContext(root, "hi");
        c = new SearchContext(c, "body", "doit");
        c = new SearchContext(c, "bar");
        c = new SearchContext(c, "em", null, "foo");
        c2 = new SearchContext(c, "baz");
        System.err.printf("c's foo is %s.\n", c.getProperty("foo"));
        System.err.printf("c2's foo is %s.\n", c2.getProperty("foo"));
        System.err.printf("c's foo is %s.\n", c.getProperty("foo"));
        c3 = new SearchContext(c, "blah");
        System.err.printf("c3's foo is %s.\n", c3.getProperty("foo"));
        c4 = new SearchContext(c2, "em");
        System.err.printf("c4's foo is %s.\n", c4.getProperty("foo"));
        System.err.printf("c4's rootTest is %s.\n", c4.getProperty("rootTest"));
        System.err.printf("c4's childTest is %s.\n", c4
            .getProperty("childTest"));
        System.err.printf("c4's classTest is %s.\n", c4
            .getProperty("classTest"));
    }
}

package net.immute.ccs.dag;

// TODO if this is really kept isolated to within Loader, etc., maybe rename to DagBuilder or something...
public class Dag {
    private final Node root = new Node();

    private int nextProperty = 0;

    public int nextProperty() {
        return nextProperty++;
    }

    public Node getRoot() {
        return root;
    }
}

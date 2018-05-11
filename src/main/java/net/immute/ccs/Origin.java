package net.immute.ccs;

public class Origin {
    private final String filename;
    private final int lineNum;

    public Origin(String filename, int lineNumber) {
        this.filename = filename;
        lineNum = lineNumber;
    }

    public String toString() {
        return filename + ":" + lineNum;
    }
}

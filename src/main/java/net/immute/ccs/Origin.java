package net.immute.ccs;

import java.util.*;

public class Origin {
    private String _filename;

    private int _lineNum;

    private List _predicates = new ArrayList();

    public Origin(String filename, int lineNumber) {
        _filename = filename;
        _lineNum = lineNumber;
    }

    public String getFilename() {
        return _filename;
    }

    public int getLineNumber() {
        return _lineNum;
    }

    public void setPredicates(List predicates) {
        _predicates = predicates;
    }

    public String getPredicateString() {
        StringBuffer tmp = new StringBuffer("{");
        for (Iterator it = _predicates.iterator(); it.hasNext();) {
            tmp.append(it.next().toString());
            if (it.hasNext()) {
                tmp.append(" -> ");
            }
        }
        tmp.append("}");
        return tmp.toString();
    }

    public String toString() {
        return _filename + " (line " + _lineNum + ")";
    }
}

package net.immute.ccs.parser;

import net.immute.ccs.CCSLogger;
import net.immute.ccs.CCSProperty;
import net.immute.ccs.Origin;
import net.immute.ccs.tree.CCSNode;
import net.immute.ccs.tree.Key;
import org.w3c.css.sac.*;

import java.io.LineNumberReader;
import java.util.Stack;

public class CCSDocumentHandler implements DocumentHandler {
    private final CCSNode rootNode;
    private final String fileName;
    private final LineNumberReader reader;

    private SelectorList currentSelector = null;
    private CCSNode currentNode = null;
    private int propertyNumber = 1;

    public CCSDocumentHandler(CCSNode rootNode, String fileName,
        LineNumberReader reader) {
        this.rootNode = rootNode;
        this.fileName = fileName;
        this.reader = reader;
    }

    public void comment(String arg0) throws CSSException {
        // ignored
    }

    public void endDocument(InputSource arg0) throws CSSException {}

    public void endFontFace() throws CSSException {
        CCSLogger.error("not implemented: endFontFace()");
    }

    public void endMedia(SACMediaList arg0) throws CSSException {
        CCSLogger.error("not implemented: endMedia()");
    }

    public void endPage(String arg0, String arg1) throws CSSException {
        CCSLogger.error("not implemented: endPage()");
    }

    public void endSelector(SelectorList arg0) throws CSSException {
        currentSelector = null;
        currentNode = null;
    }

    public void ignorableAtRule(String arg0) throws CSSException {
    // ignored...
    }

    public void importStyle(String arg0, SACMediaList arg1, String arg2)
        throws CSSException {
    // TODO: handle imports!
    }

    public void namespaceDeclaration(String arg0, String arg1)
        throws CSSException {
        CCSLogger.error("not implemented: namespaceDeclaration()");
    }

    public void property(String name, LexicalUnit value, boolean important)
            throws CSSException {
        // first find/create the correct node for this selector...
        // we'll also compute the specificity numbers as we go.
        if (currentNode == null) {
            currentNode = rootNode;
            if (currentSelector.getLength() != 1) {
                CCSLogger.error("unhandled selector length: "
                    + currentSelector.getLength());
            }
            Selector s = currentSelector.item(0);
            Stack<Key> selectors = new Stack<Key>();
            while (validCombinator(s)) {
                DescendantSelector ds = (DescendantSelector) s;
                Key key =
                    handleSimpleSelector(ds.getSimpleSelector());
                if (ds.getSelectorType() == Selector.SAC_CHILD_SELECTOR) {
                    key.setDirectChild(true);
                }
                selectors.push(key);
                s = ds.getAncestorSelector();
            }

            // final parent...
            if (!(s instanceof SimpleSelector)) {
                throw new CSSException(
                    "not a simple selector or a combinator, what is it? " + s);
            }
            selectors
                .push(handleSimpleSelector((SimpleSelector) s));

            // then build our nodes...
            while (!selectors.isEmpty()) {
                Key key = selectors.pop();
                CCSNode tmpNode = currentNode.getChild(key);
                if (tmpNode == null) {
                    tmpNode = new CCSNode();
                    currentNode.addChild(key, tmpNode);
                }
                currentNode = tmpNode;
            }
        }

        // then set the property...
        // TODO the origin line number here is broken. i'm guessing flute's looking ahead...
        CCSProperty property =
            new CCSProperty(value.getStringValue(), new Origin(fileName, reader
                .getLineNumber()), propertyNumber);
        currentNode.addProperty(name, property, false);
        propertyNumber++;
    }

    private boolean validCombinator(Selector s) {
        switch (s.getSelectorType()) {
            case Selector.SAC_DESCENDANT_SELECTOR:
            case Selector.SAC_CHILD_SELECTOR:
                return true;
        }
        return false;
    }

    private Key handleSimpleSelector(SimpleSelector ss)
            throws CSSException {
        switch (ss.getSelectorType()) {
            case Selector.SAC_ANY_NODE_SELECTOR:
                // shouldn't * show up here? but it doesn't... see below.
                return new Key(null);
            case Selector.SAC_ELEMENT_NODE_SELECTOR:
                ElementSelector es = (ElementSelector) ss;
                // actually * is parsed as an ElementSelector with null
                // LocalName, which
                // is actually exactly what we want, although we have to be
                // careful
                // about incrementing the count of element names...
                return new Key(es.getLocalName());
            case Selector.SAC_CONDITIONAL_SELECTOR:
                ConditionalSelector cs = (ConditionalSelector) ss;
                Condition c = cs.getCondition();
                Key k = handleSimpleSelector(cs.getSimpleSelector());
                while (c.getConditionType() == Condition.SAC_AND_CONDITION) {
                    CombinatorCondition cc = (CombinatorCondition) c;
                    handleSimpleCondition(k, cc.getSecondCondition());
                    c = cc.getFirstCondition();
                }
                handleSimpleCondition(k, c);
                return k;
        }
        throw new CSSException("UNHANDLED SELECTOR: " + ss);
    }

    private void handleSimpleCondition(Key key, Condition c) throws CSSException {
        switch (c.getConditionType()) {
            case Condition.SAC_ATTRIBUTE_CONDITION:
                AttributeCondition ac = (AttributeCondition) c;
                key.setAttribute(ac.getLocalName(), ac.getValue());
                return;
            case Condition.SAC_ID_CONDITION:
                ac = (AttributeCondition) c;
                key.setId(ac.getValue());
                return;
            case Condition.SAC_CLASS_CONDITION:
                ac = (AttributeCondition) c;
                key.addClass(ac.getValue());
                return;
            case Condition.SAC_PSEUDO_CLASS_CONDITION:
                ac = (AttributeCondition) c;
                // TODO check ac!
                key.setRoot(true);
                return;
        }
        throw new CSSException("UNHANDLED CONDITION: " + c);
    }

    public void startDocument(InputSource source) throws CSSException {}

    public void startFontFace() throws CSSException {
        CCSLogger.error("not implemented: startFontFace()");
    }

    public void startMedia(SACMediaList arg0) throws CSSException {
        CCSLogger.error("not implemented: startMedia()");
    }

    public void startPage(String arg0, String arg1) throws CSSException {
        CCSLogger.error("not implemented: startPage()");
    }

    public void startSelector(SelectorList selector) throws CSSException {
        currentSelector = selector;
    }
}

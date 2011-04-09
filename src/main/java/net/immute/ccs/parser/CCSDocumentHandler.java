package net.immute.ccs.parser;

import java.io.LineNumberReader;
import java.util.Stack;

import net.immute.ccs.CCSLogger;
import net.immute.ccs.CCSProperty;
import net.immute.ccs.Origin;
import net.immute.ccs.Specificity;
import net.immute.ccs.tree.CCSNode;
import net.immute.ccs.tree.Key;

import org.w3c.css.sac.AttributeCondition;
import org.w3c.css.sac.CSSException;
import org.w3c.css.sac.CombinatorCondition;
import org.w3c.css.sac.Condition;
import org.w3c.css.sac.ConditionalSelector;
import org.w3c.css.sac.DescendantSelector;
import org.w3c.css.sac.DocumentHandler;
import org.w3c.css.sac.ElementSelector;
import org.w3c.css.sac.InputSource;
import org.w3c.css.sac.LexicalUnit;
import org.w3c.css.sac.SACMediaList;
import org.w3c.css.sac.Selector;
import org.w3c.css.sac.SelectorList;
import org.w3c.css.sac.SimpleSelector;

public class CCSDocumentHandler implements DocumentHandler {
    SelectorList currentSelector = null;

    CCSNode currentNode = null;

    CCSNode rootNode;

    int propertyNumber = 1;

    String fileName;

    LineNumberReader reader;

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
        CCSLogger.internalError("not implemented: endFontFace()");
    }

    public void endMedia(SACMediaList arg0) throws CSSException {
        CCSLogger.internalError("not implemented: endMedia()");
    }

    public void endPage(String arg0, String arg1) throws CSSException {
        CCSLogger.internalError("not implemented: endPage()");
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
        CCSLogger.internalError("not implemented: namespaceDeclaration()");
    }

    public void property(String name, LexicalUnit value, boolean important)
        throws CSSException {
        Specificity specificity = new Specificity();

        // first find/create the correct node for this selector...
        // we'll also compute the specificity numbers as we go.
        if (currentNode == null) {
            currentNode = rootNode;
            if (currentSelector.getLength() != 1) {
                CCSLogger.internalError("unhandled selector length: "
                    + currentSelector.getLength());
            }
            Selector s = currentSelector.item(0);
            Stack<Key> selectors = new Stack<Key>();
            while (validCombinator(s)) {
                DescendantSelector ds = (DescendantSelector) s;
                Key key =
                    handleSimpleSelector(ds.getSimpleSelector(), specificity);
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
                .push(handleSimpleSelector((SimpleSelector) s, specificity));

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
        CCSProperty property =
            new CCSProperty(value.getStringValue(), new Origin(fileName, reader
                .getLineNumber()), propertyNumber, specificity);
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

    private Key handleSimpleSelector(SimpleSelector ss, Specificity specificity)
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
                if (es.getLocalName() != null) {
                    specificity.incElementNames();
                }
                return new Key(es.getLocalName());
            case Selector.SAC_CONDITIONAL_SELECTOR:
                ConditionalSelector cs = (ConditionalSelector) ss;
                Condition c = cs.getCondition();
                Key k =
                    handleSimpleSelector(cs.getSimpleSelector(), specificity);
                while (c.getConditionType() == Condition.SAC_AND_CONDITION) {
                    CombinatorCondition cc = (CombinatorCondition) c;
                    handleSimpleCondition(k, cc.getSecondCondition(),
                        specificity);
                    c = cc.getFirstCondition();
                }
                handleSimpleCondition(k, c, specificity);
                return k;
        }
        throw new CSSException("UNHANDLED SELECTOR: " + ss);
    }

    private void handleSimpleCondition(Key key, Condition c,
        Specificity specificity) throws CSSException {
        switch (c.getConditionType()) {
            case Condition.SAC_ATTRIBUTE_CONDITION:
                AttributeCondition ac = (AttributeCondition) c;
                specificity.incClassSelectors();
                key.setAttribute(ac.getLocalName(), ac.getValue());
                return;
            case Condition.SAC_ID_CONDITION:
                ac = (AttributeCondition) c;
                specificity.incIdSelectors();
                key.setId(ac.getValue());
                return;
            case Condition.SAC_CLASS_CONDITION:
                ac = (AttributeCondition) c;
                specificity.incClassSelectors();
                key.addClass(ac.getValue());
                return;
            case Condition.SAC_PSEUDO_CLASS_CONDITION:
                ac = (AttributeCondition) c;
                specificity.incClassSelectors();
                key.setRoot(true);
                return;
        }
        throw new CSSException("UNHANDLED CONDITION: " + c);
    }

    public void startDocument(InputSource source) throws CSSException {}

    public void startFontFace() throws CSSException {
        CCSLogger.internalError("not implemented: startFontFace()");
    }

    public void startMedia(SACMediaList arg0) throws CSSException {
        CCSLogger.internalError("not implemented: startMedia()");
    }

    public void startPage(String arg0, String arg1) throws CSSException {
        CCSLogger.internalError("not implemented: startPage()");
    }

    public void startSelector(SelectorList selector) throws CSSException {
        currentSelector = selector;
    }
}

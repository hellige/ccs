package net.immute.ccs.parser;

import org.parboiled.BaseParser;
import org.parboiled.Parboiled;
import org.parboiled.Rule;
import org.parboiled.annotations.BuildParseTree;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.support.ParseTreeUtils;
import org.parboiled.support.ParsingResult;

public class ParboiledTest {
    @BuildParseTree
    static class CcsParser extends BaseParser<Void> {
        Rule nl() {
            return FirstOf('\n', Sequence('\r', Optional('\n')));
        }

        Rule space() {
            return FirstOf(AnyOf(" \t\f"), nl());
        }

        Rule sp() {
            return ZeroOrMore(FirstOf(blockComment(),
                    Sequence("//", ZeroOrMore(NoneOf("\n\r"), FirstOf(nl(), EOI))),
                    space()));
        }
        
        Rule blockComment() {
            return Sequence("/*", ZeroOrMore(FirstOf(blockComment(), Sequence(TestNot("*/"), ANY))), "*/");
        }

        Rule string() {
            return FirstOf(
                    Sequence('\'', ZeroOrMore(NoneOf("\'\n\r")), '\''),
                    Sequence('\"', ZeroOrMore(NoneOf("\"\n\r")), '\"'));
        }

        Rule property() {
            return String("prop"); // TODO
        }
        
        Rule selector() {
            return String("selector"); // TODO
        }

        Rule imprt() {
            return Sequence("@import", sp(), string());
        }

        Rule context() {
            return Sequence("@context", sp(), Optional(AnyOf("+>")), sp(), '(', sp(), selector(), sp(), ')',
                    sp(), Optional(';'), sp());
        }

        Rule rule() {
            return Sequence(FirstOf(imprt(), property(), Sequence(selector(), sp(), '{', sp(),
                    ZeroOrMore(rule()), sp(), '}')), sp(), Optional(';'), sp());
        }

        Rule ruleset() {
            return Sequence(sp(), ZeroOrMore(context()), sp(), ZeroOrMore(rule()), sp(), EOI);
        }
    }

    public static void main(String[] args) {
        String input = args.length > 0 ? args[0] : "\n@context (selector);\n @import /* hi */ 'foo';\n@import 'bar';";
        CcsParser parser = Parboiled.createParser(CcsParser.class);
        ParsingResult<?> result = new ReportingParseRunner(parser.ruleset()).run(input);
        String parseTreePrintOut = ParseTreeUtils.printNodeTree(result);
        System.out.println(parseTreePrintOut);
    }
}

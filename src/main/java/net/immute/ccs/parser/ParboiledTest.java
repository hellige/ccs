package net.immute.ccs.parser;

import org.parboiled.BaseParser;
import org.parboiled.Parboiled;
import org.parboiled.Rule;
import org.parboiled.annotations.BuildParseTree;
import org.parboiled.annotations.SuppressSubnodes;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.support.ParseTreeUtils;
import org.parboiled.support.ParsingResult;

public class ParboiledTest {
    @BuildParseTree
    static class CcsParser extends BaseParser<Void> {
        @SuppressSubnodes
        Rule nl() {
            return FirstOf('\n', Sequence('\r', Optional('\n')));
        }

        Rule space() {
            return FirstOf(AnyOf(" \t\f"), nl());
        }

        @SuppressSubnodes
        Rule sp() {
            return ZeroOrMore(FirstOf(blockComment(),
                    Sequence("//", ZeroOrMore(NoneOf("\n\r"), FirstOf(nl(), EOI))),
                    space()));
        }

        @SuppressSubnodes
        Rule blockComment() {
            return Sequence("/*", ZeroOrMore(FirstOf(blockComment(), Sequence(TestNot("*/"), ANY))), "*/");
        }

        @SuppressSubnodes
        Rule string() {
            return FirstOf(
                    Sequence('\'', ZeroOrMore(NoneOf("\'\n\r")), '\''),
                    Sequence('\"', ZeroOrMore(NoneOf("\"\n\r")), '\"'));
        }

        Rule val() {
            return string(); // TODO hex, long, double
        }

        Rule identChar() {
            return FirstOf(CharRange('A', 'Z'), CharRange('a', 'z'), CharRange('0', '9'), '$', '_');
        }

        @SuppressSubnodes
        Rule ident() {
            return FirstOf(OneOrMore(identChar()), string());
        }

        Rule property() {
            return Sequence(Optional("inherit"), sp(), ident(), sp(), '=', sp(), val(), sp()); //qi::lexeme[val >> !ident];
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
                    ZeroOrMore(rule()), '}')), sp(), Optional(';'), sp());
        }

        Rule ruleset() {
            return Sequence(sp(), ZeroOrMore(context()), sp(), ZeroOrMore(rule()), sp(), EOI);
        }
    }

    public static void main(String[] args) {
        String input = args.length > 0 ? args[0] : "\n@context (selector);\n @import /* hi */ 'foo';\n@import 'bar';" +
                "selector { foo = 'test' };";
        CcsParser parser = Parboiled.createParser(CcsParser.class);
        ParsingResult<?> result = new ReportingParseRunner(parser.ruleset()).run(input);
        String parseTreePrintOut = ParseTreeUtils.printNodeTree(result);
        System.out.println(parseTreePrintOut);
    }
}

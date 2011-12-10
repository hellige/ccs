package net.immute.ccs.parser;

import org.parboiled.BaseParser;
import org.parboiled.Parboiled;
import org.parboiled.Rule;
import org.parboiled.annotations.BuildParseTree;
import org.parboiled.annotations.MemoMismatches;
import org.parboiled.annotations.SuppressNode;
import org.parboiled.annotations.SuppressSubnodes;
import org.parboiled.errors.ErrorUtils;
import org.parboiled.parserunners.RecoveringParseRunner;
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

        @SuppressNode
        Rule sp() {
            return ZeroOrMore(FirstOf(blockComment(),
                    Sequence("//", ZeroOrMore(NoneOf("\n\r")), FirstOf(nl(), EOI)),
                    space()));
        }

        @SuppressSubnodes
        Rule blockComment() {
            return Sequence("/*", ZeroOrMore(FirstOf(blockComment(), Sequence(TestNot("*/"), ANY))), "*/");
        }

        @SuppressSubnodes
        Rule stringLit() {
            return FirstOf(
                    Sequence('\'', ZeroOrMore(NoneOf("\'\n\r")), '\''),
                    Sequence('\"', ZeroOrMore(NoneOf("\"\n\r")), '\"'));
        }

        Rule boolLit() {
            return FirstOf("true", "false");
        }

        @SuppressSubnodes
        @MemoMismatches
        Rule hexLit() {
            return Sequence('0', IgnoreCase('x'),
                    OneOrMore(FirstOf(CharRange('a', 'f'), CharRange('A', 'F'), CharRange('0', '9'))));
        }

        Rule Digit() {
            return CharRange('0', '9');
        }

        Rule Exponent() {
            return Sequence(AnyOf("eE"), Optional(AnyOf("+-")), OneOrMore(Digit()));
        }

        @SuppressSubnodes
        Rule doubleLit() {
            return FirstOf(
                    Sequence(OneOrMore(Digit()), '.', ZeroOrMore(Digit()), Optional(Exponent())),
                    Sequence('.', OneOrMore(Digit()), Optional(Exponent())),
                    Sequence(OneOrMore(Digit()), Exponent())
            );
        }

        @SuppressSubnodes
        Rule intLit() {
            return Sequence(Optional('-'), OneOrMore(Digit()));
        }

        Rule val() {
            return FirstOf(boolLit(), hexLit(), doubleLit(), intLit(), stringLit());
        }

        Rule identChar() {
            return FirstOf(CharRange('A', 'Z'), CharRange('a', 'z'), CharRange('0', '9'), '$', '_');
        }

        @SuppressSubnodes
        Rule ident() {
            return FirstOf(OneOrMore(identChar()), stringLit());
        }

        Rule property() {
            return Sequence(Optional("inherit"), sp(), ident(), sp(), '=', sp(), val(), sp()); // TODO qi::lexeme[val >> !ident];
        }

        Rule stepsuffix() {
            return FirstOf(
                    Sequence('.', ident(), Optional(stepsuffix())),
                    Sequence('#', ident(), Optional(stepsuffix())),
                    Sequence('[', sp(), ident(), sp(), '=', sp(), val(), sp(), ']', Optional(stepsuffix())));
        }

        Rule step() {
            return FirstOf(
                    Sequence(FirstOf('*', ident()), Optional(stepsuffix())),
                    stepsuffix(),
                    Sequence('(', sum(), ')'));
        }

        Rule term() {
            return Sequence(step(), ZeroOrMore(sp(), Optional('>'), sp(), step()));
        }

        Rule product() {
            return Sequence(term(), ZeroOrMore(sp(), '+', sp(), term()));
        }

        Rule sum() {
            return Sequence(product(), ZeroOrMore(Sequence(sp(), ',', sp(), product())));
        }

        Rule selector() {
            return Sequence(Optional(AnyOf("+>,")), sp(), sum());
        }

        Rule imprt() {
            return Sequence("@import", sp(), stringLit());
        }

        Rule context() {
            return Sequence("@context", sp(), Optional(AnyOf("+>")), sp(), '(', sp(), selector(), sp(), ')',
                    sp(), Optional(';'), sp());
        }

        Rule rule() {
            return Sequence(sp(), FirstOf(imprt(), property(), Sequence(selector(), sp(), '{', sp(),
                    ZeroOrMore(rule()), '}')), sp(), Optional(';'), sp());
        }

        Rule ruleset() {
            return Sequence(sp(), ZeroOrMore(context()), sp(), ZeroOrMore(rule()), sp(), EOI);
        }
    }

    public static void main(String[] args) {
        String input = args.length > 0 ? args[0] : "\n@context (foo#bar);\n @import /* hi */ 'foo';\n@import 'bar';" +
                ".class > #id[a = 'b'] asdf { foo = 'test'; bar = 32; baz = 0.3; bum = 1e1 };";
        CcsParser parser = Parboiled.createParser(CcsParser.class);
        ParsingResult<?> result = new RecoveringParseRunner(parser.ruleset()).run(input);
        if (!result.hasErrors())
            System.out.println(ParseTreeUtils.printNodeTree(result));
        else
            System.out.println(ErrorUtils.printParseErrors(result));
    }
}

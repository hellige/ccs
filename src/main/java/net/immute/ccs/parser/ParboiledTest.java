package net.immute.ccs.parser;

import net.immute.ccs.tree.Key;
import org.parboiled.BaseParser;
import org.parboiled.Parboiled;
import org.parboiled.Rule;
import org.parboiled.annotations.*;
import org.parboiled.errors.ErrorUtils;
import org.parboiled.parserunners.RecoveringParseRunner;
import org.parboiled.support.ParseTreeUtils;
import org.parboiled.support.ParsingResult;
import org.parboiled.support.Var;

import java.util.ArrayList;
import java.util.List;

public class ParboiledTest {
    @BuildParseTree
    static class CcsBaseParser<T> extends BaseParser<T> {
        protected <T> boolean append(Var<? extends List<? super T>> ts, T t) {
            ts.get().add(t);
            return true;
        }

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
        Rule stringLit(Var<? super Value<String>> value) {
            return FirstOf(
                    Sequence('\'', ZeroOrMore(NoneOf("\'\n\r")), value.set(new Value<String>(match())), '\''),
                    Sequence('\"', ZeroOrMore(NoneOf("\"\n\r")), value.set(new Value<String>(match())), '\"'));
        }

        Rule boolLit(Var<Value<?>> value) {
            return Sequence(FirstOf("true", "false"), value.set(new Value<Boolean>(Boolean.valueOf(match()))));
        }

        @SuppressSubnodes
        Rule hexLit(Var<? super Value<Long>> value) {
            return Sequence('0', IgnoreCase('x'),
                    OneOrMore(FirstOf(CharRange('a', 'f'), CharRange('A', 'F'), CharRange('0', '9'))),
                    value.set(new Value<Long>(Long.parseLong(match(), 16))));
        }

        Rule Digit() {
            return CharRange('0', '9');
        }

        Rule Exponent() {
            return Sequence(AnyOf("eE"), Optional(AnyOf("+-")), OneOrMore(Digit()));
        }

        @SuppressSubnodes
        Rule doubleLit(Var<? super Value<Double>> value) {
            return Sequence(FirstOf(
                    Sequence(OneOrMore(Digit()), '.', ZeroOrMore(Digit()), Optional(Exponent())),
                    Sequence('.', OneOrMore(Digit()), Optional(Exponent())),
                    Sequence(OneOrMore(Digit()), Exponent())),
                    value.set(new Value<Double>(Double.parseDouble(match()))));
        }

        @SuppressSubnodes
        Rule intLit(Var<? super Value<Long>> value) {
            return Sequence(Sequence(Optional('-'), OneOrMore(Digit())),
                    value.set(new Value<Long>(Long.parseLong(match()))));
        }

        Rule val(Var<Value<?>> value) {
            return FirstOf(boolLit(value), hexLit(value), doubleLit(value), intLit(value), stringLit(value));
        }

        Rule identChar() {
            return FirstOf(CharRange('A', 'Z'), CharRange('a', 'z'), CharRange('0', '9'), '$', '_');
        }

        @SuppressSubnodes
        Rule ident(Var<String> ident) {
            Var<Value<String>> tmp = new Var<Value<String>>();
            return FirstOf(
                    Sequence(OneOrMore(identChar()), ident.set(match())),
                    Sequence(stringLit(tmp), ident.set(tmp.get().get())));
        }
    }

    @BuildParseTree
    static class SelectorParser extends CcsBaseParser<Selector> {
        protected boolean setElement(Var<Key> key, String element) {
            key.get().setElement(element);
            return true;
        }

        protected boolean addClass(Var<Key> key, String cls) {
            key.get().addClass(cls);
            return true;
        }

        protected boolean setId(Var<Key> key, String id) {
            key.get().setId(id);
            return true;
        }

        protected boolean setAttribute(Var<Key> key, String attr, Value<?> value) {
            key.get().setAttribute(attr, value.toString()); // TODO use real value, not string
            return true;
        }

        @Cached
        Rule stepsuffix(Var<Key> key) {
            Var<String> tmp = new Var<String>();
            Var<Value<?>> attrVal = new Var<Value<?>>();
            return FirstOf(
                    Sequence('.', ident(tmp), addClass(key, tmp.get()), Optional(stepsuffix(key))),
                    Sequence('#', ident(tmp), setId(key, tmp.get()), Optional(stepsuffix(key))),
                    Sequence('[', sp(), ident(tmp), sp(), '=', sp(), val(attrVal), sp(), ']',
                            setAttribute(key, tmp.get(), attrVal.get()), Optional(stepsuffix(key))));
        }

        Rule step() {
            Var<Key> key = new Var<Key>(new Key(null));
            Var<String> tmp = new Var<String>();
            return FirstOf(
                    Sequence(FirstOf('*', Sequence(ident(tmp), setElement(key, tmp.get()))), Optional(stepsuffix(key)),
                            push(new Selector.Step(key.get()))),
                    Sequence(stepsuffix(key), push(new Selector.Step(key.get()))),
                    Sequence('(', sum(), ')'));
        }

        Rule term() {
            return Sequence(step(), ZeroOrMore(sp(), FirstOf(
                    Sequence('>', sp(), step(), push(new Selector.Child(pop(1), pop()))),
                    Sequence(step(), push(new Selector.Descendant(pop(1), pop()))))));
        }

        Rule product() {
            return Sequence(term(), ZeroOrMore(sp(), '+', sp(), term(), push(pop(1).conjoin(pop()))));
        }

        Rule sum() {
            return Sequence(product(), ZeroOrMore(Sequence(sp(), ',', sp(), product(), push(pop(1).disjoin(pop())))));
        }
    }

    @BuildParseTree
    static class CcsParser extends CcsBaseParser<List<AstRule>> {
        protected final SelectorParser selectorParser = Parboiled.createParser(SelectorParser.class);

        // TODO consider explicit 'override'
        Rule property(Var<List<AstRule>> rules) {
            Var<String> name = new Var<String>();
            Var<Value<?>> value = new Var<Value<?>>();
            return Sequence(Optional("inherit"), sp(), ident(name), sp(), '=', sp(), val(value), sp(),
                    append(rules, new AstRule.PropDef(name.get(), value.get()))); // TODO qi::lexeme[val >> !ident];
        }

        Rule selector(Var<Selector> result) {
            return Sequence(selectorParser.sum(), sp(), Optional(AnyOf("+>")), result.set(selectorParser.pop())); // TODO +>
        }

        Rule imprt(Var<List<AstRule>> rules) {
            Var<Value<String>> location = new Var<Value<String>>();
            return Sequence("@import", sp(), stringLit(location),
                    append(rules, new AstRule.Import(location.get().get())));
        }

        Rule nested(Var<List<AstRule>> rules) {
            Var<Selector> selector = new Var<Selector>();
            Var<List<AstRule>> tmp = new Var<List<AstRule>>();
            return Sequence(selector(selector), sp(), '{', sp(),
                    rules.enterFrame(), ZeroOrMore(rule(rules)), '}', tmp.set(rules.get()), rules.exitFrame(),
                    append(rules, new AstRule.Nested(selector.get(), tmp.get())));
        }

        @Cached
        Rule rule(Var<List<AstRule>> rules) {
            return Sequence(sp(), FirstOf(imprt(rules), property(rules), nested(rules)), sp(), Optional(';'), sp());
        }

        Rule context(Var<Selector> result) {
            return Sequence("@context", sp(), '(', sp(), selector(result), sp(), ')',
                    sp(), Optional(';'), sp());
        }

        Rule ruleset() {
            Var<Selector> context = new Var<Selector>();
            Var<List<AstRule>> rules = new Var<List<AstRule>>(new ArrayList<AstRule>());
            // TODO include context in final result...
            return Sequence(sp(), Optional(context(context)), sp(),
                    ZeroOrMore(rule(rules)), sp(), push(rules.get()), EOI);
        }
    }

    public static void main(String[] args) {
        String input = args.length > 0 ? args[0] : "\n@context (foo#bar);\n @import /* hi */ 'foo';\n@import 'bar';" +
                ".class > #id[a = 'b'] (a + b + c) asdf { foo = 'test'; bar = 32; baz = 0.3; bum = 1e1 };";
        CcsParser parser = Parboiled.createParser(CcsParser.class);
        ParsingResult<?> result = new RecoveringParseRunner(parser.ruleset()).run(input);
        if (!result.hasErrors())
            System.out.println(ParseTreeUtils.printNodeTree(result));
        else
            System.out.println(ErrorUtils.printParseErrors(result));
    }
}

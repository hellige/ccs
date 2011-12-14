package net.immute.ccs.parser;

import net.immute.ccs.Origin;
import net.immute.ccs.dag.Key;
import org.parboiled.BaseParser;
import org.parboiled.Parboiled;
import org.parboiled.Rule;
import org.parboiled.annotations.Cached;
import org.parboiled.annotations.SuppressNode;
import org.parboiled.annotations.SuppressSubnodes;
import org.parboiled.buffers.DefaultInputBuffer;
import org.parboiled.buffers.InputBuffer;
import org.parboiled.common.FileUtils;
import org.parboiled.parserunners.RecoveringParseRunner;
import org.parboiled.support.ParsingResult;
import org.parboiled.support.Var;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.Reader;

public class CcsParser {
    public ParsingResult<AstRule> parse(Reader input, String fileName) throws IOException {
        CharArrayWriter tmp = new CharArrayWriter();
        FileUtils.copyAll(input, tmp);
        InputBuffer buffer = new DefaultInputBuffer(tmp.toCharArray());
        RulesetParser parser = Parboiled.createParser(RulesetParser.class, fileName);
        return new RecoveringParseRunner<AstRule>(parser.ruleset()).run(buffer);
    }

    static class CcsBaseParser<T> extends BaseParser<T> {
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
            Var<Key> key = new Var<Key>();
            Var<String> tmp = new Var<String>();
            // note: we have to set key's initial value below (rather than via the constructor) if we want things
            // to work in jarjar. which we do.
            return Sequence(key.set(new Key(null)), FirstOf(
                    Sequence(FirstOf('*', Sequence(ident(tmp), setElement(key, tmp.get()))), Optional(stepsuffix(key)),
                            push(new Selector.Step(key.get()))),
                    Sequence(stepsuffix(key), push(new Selector.Step(key.get()))),
                    Sequence('(', sum(), ')')));
        }

        Rule term() {
            return Sequence(step(), ZeroOrMore(sp(), FirstOf(
                    Sequence('>', sp(), step(), push(new Selector.Descendant(pop(1), pop().asDirectChild()))),
                    Sequence(step(), push(new Selector.Descendant(pop(1), pop()))))));
        }

        Rule product() {
            return Sequence(term(), ZeroOrMore(sp(), '+', sp(), term(), push(pop(1).conjoin(pop()))));
        }

        Rule sum() {
            return Sequence(product(), ZeroOrMore(Sequence(sp(), ',', sp(), product(), push(pop(1).disjoin(pop())))));
        }
    }

    static class RulesetParser extends CcsBaseParser<AstRule.Nested> {
        protected final SelectorParser selectorParser = Parboiled.createParser(SelectorParser.class);

        protected final String fileName;

        RulesetParser(String fileName) {
            this.fileName = fileName;
        }

        // TODO consider explicit 'override'
        Rule property() {
            Var<String> name = new Var<String>();
            Var<Value<?>> value = new Var<Value<?>>();
            // TODO do something with "inherit"...
            return Sequence(Optional("inherit"), sp(), ident(name), sp(), '=', sp(), val(value),
                    peek().append(new AstRule.PropDef(name.get(), value.get(), new Origin(fileName, position().line))),
                    sp()); // put space after the append() so that we're sure to have the right line number...
                    // TODO qi::lexeme[val >> !ident];
        }

        Rule selector(Var<Selector> result) {
            return Sequence(selectorParser.sum(), sp(), Optional(AnyOf("+>")), result.set(selectorParser.pop())); // TODO +>
        }

        Rule imprt() {
            Var<Value<String>> location = new Var<Value<String>>();
            return Sequence("@import", sp(), stringLit(location),
                    peek().append(new AstRule.Import(location.get().get())));
        }

        Rule nested() {
            Var<Selector> selector = new Var<Selector>();
            return Sequence(selector(selector), sp(), '{', sp(),
                    push(new AstRule.Nested(selector.get())), ZeroOrMore(rule()), '}',
                    peek(1).append(pop()));
        }

        @Cached
        Rule rule() {
            return Sequence(sp(), FirstOf(imprt(), property(), nested()), sp(), Optional(';'), sp());
        }

        Rule context() {
            Var<Selector> tmp = new Var<Selector>();
            return Sequence("@context", sp(), '(', sp(), selector(tmp), sp(), ')',
                    sp(), Optional(';'), sp(), peek().setSelector(tmp.get()));
        }

        Rule ruleset() {
            return Sequence(push(new AstRule.Nested(null)), sp(), Optional(context()), sp(),
                    ZeroOrMore(rule()), sp(), EOI);
        }
    }
}

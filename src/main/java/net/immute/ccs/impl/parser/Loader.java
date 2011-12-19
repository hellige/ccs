package net.immute.ccs.impl.parser;

import net.immute.ccs.CcsLogger;
import net.immute.ccs.ImportResolver;
import net.immute.ccs.Origin;
import net.immute.ccs.impl.dag.DagBuilder;
import net.immute.ccs.impl.dag.Key;
import org.parboiled.BaseParser;
import org.parboiled.Parboiled;
import org.parboiled.Rule;
import org.parboiled.annotations.Cached;
import org.parboiled.annotations.SuppressNode;
import org.parboiled.annotations.SuppressSubnodes;
import org.parboiled.buffers.DefaultInputBuffer;
import org.parboiled.buffers.InputBuffer;
import org.parboiled.common.FileUtils;
import org.parboiled.errors.ErrorUtils;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.support.ParsingResult;
import org.parboiled.support.Var;

import java.io.*;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class Loader {
    private final CcsLogger log;

    public Loader(CcsLogger log) {
        this.log = log;
    }

    public CcsLogger getLogger() {
        return log;
    }

    public void loadCcsStream(InputStream stream, String fileName, DagBuilder dag, ImportResolver importResolver)
            throws IOException {
        AstRule rule = parseCcsStream(stream, fileName, importResolver, new Stack<String>());
        if (rule == null) return;

        // everything parsed, no errors. now it's safe to modify the dag...
        rule.addTo(dag.getBuildContext(), dag.getBuildContext());
    }

    AstRule parseCcsStream(InputStream stream, String fileName, ImportResolver importResolver, Stack<String> inProgress)
            throws IOException {
        Reader reader = new InputStreamReader(stream);
        ParsingResult<AstRule> result = parse(reader, fileName);
        reader.close();

        if (result.hasErrors()) {
            log.error("Errors parsing " + fileName + ":" + ErrorUtils.printParseErrors(result));
            return null;
        }

        AstRule rule = result.resultValue;
        if (!rule.resolveImports(importResolver, this, inProgress)) return null;

        return rule;
    }

    private ParsingResult<AstRule> parse(Reader input, String fileName) throws IOException {
        CharArrayWriter tmp = new CharArrayWriter();
        FileUtils.copyAll(input, tmp);
        InputBuffer buffer = new DefaultInputBuffer(tmp.toCharArray());
        RulesetParser parser = Parboiled.createParser(RulesetParser.class, fileName);
        return new ReportingParseRunner<AstRule>(parser.ruleset()).run(buffer);
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
        @SuppressWarnings("InfiniteRecursion")
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

    static class SelectorParser extends CcsBaseParser<SelectorLeaf> {
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

        protected boolean setDirect(Var<Key> key) {
            key.get().setDirectChild(true);
            return true;
        }

        @Cached
        @SuppressWarnings("InfiniteRecursion")
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
            return Sequence(key.set(new Key(null)), Optional('>', sp(), setDirect(key)), FirstOf(
                    Sequence(FirstOf('*', Sequence(ident(tmp), setElement(key, tmp.get()))), Optional(stepsuffix(key)),
                            push(SelectorLeaf.step(key.get()))),
                    Sequence(stepsuffix(key), push(SelectorLeaf.step(key.get()))),
                    Sequence('(', sum(), ')')));
        }

        Rule term() {
            return Sequence(step(), ZeroOrMore(sp(), Sequence(step(), push(pop(1).descendant(pop())))));
        }

        Rule product() {
            return Sequence(term(), ZeroOrMore(sp(), '+', sp(), term(), push(pop(1).conjunction(pop()))));
        }

        Rule sum() {
            return Sequence(product(), ZeroOrMore(Sequence(sp(), ',', sp(), product(),
                    push(pop(1).disjunction(pop())))));
        }
    }

    static class RulesetParser extends CcsBaseParser<AstRule.Nested> {
        protected final SelectorParser selectorParser = Parboiled.createParser(SelectorParser.class);

        protected final String fileName;

        RulesetParser(String fileName) {
            this.fileName = fileName;
        }

        protected static class Modifiers {
            private final Set<String> modifiers = new HashSet<String>();

            boolean add(String modifier) {
                if (modifiers.contains(modifier)) return false;
                modifiers.add(modifier);
                return true;
            }

            boolean local() { return modifiers.contains("local"); }
            boolean override() { return modifiers.contains("override"); }
        }

        Rule modifiers(Var<Modifiers> modifiers) {
            return Sequence(modifiers.set(new Modifiers()),
                    ZeroOrMore(Sequence(FirstOf("override", "local"), modifiers.get().add(match())), sp()));
        }

        Rule property() {
            Var<Modifiers> modifiers = new Var<Modifiers>();
            Var<String> name = new Var<String>();
            Var<Value<?>> value = new Var<Value<?>>();
            return Sequence(modifiers(modifiers),
                    ident(name), sp(), '=',
                    sp(), val(value), peek().append(
                    new AstRule.PropDef(name.get(), value.get(), new Origin(fileName, position().line),
                            modifiers.get().local(), modifiers.get().override())),
                    sp()); // put space after the append() so that we're sure to have the right line number...
            // TODO qi::lexeme[val >> !ident];
        }

        Rule selector(Var<SelectorBranch> result) {
            Var<Boolean> conj = new Var<Boolean>();
            return Sequence(conj.set(false), selectorParser.sum(), sp(), Optional("+", conj.set(true)),
                    result.set(conj.get()
                            ? SelectorBranch.conjunction(selectorParser.pop())
                            : SelectorBranch.descendant(selectorParser.pop())));
        }

        Rule imprt() {
            Var<Value<String>> location = new Var<Value<String>>();
            return Sequence("@import", sp(), stringLit(location),
                    peek().append(new AstRule.Import(location.get().get())));
        }

        Rule nested() {
            Var<SelectorBranch> selector = new Var<SelectorBranch>();
            return Sequence(selector(selector), sp(), push(new AstRule.Nested(selector.get())), FirstOf(
                    Sequence(':', sp(), FirstOf(imprt(), property())),
                    Sequence('{', sp(), ZeroOrMore(rule()), '}')),
                    peek(1).append(pop()));
        }

        @Cached
        Rule rule() {
            return Sequence(sp(), FirstOf(imprt(), property(), nested()), sp(), Optional(';'), sp());
        }

        Rule context() {
            Var<SelectorBranch> tmp = new Var<SelectorBranch>();
            return Sequence("@context", sp(), '(', sp(), selector(tmp), sp(), ')',
                    sp(), Optional(';'), sp(), peek().setSelector(tmp.get()));
        }

        Rule ruleset() {
            return Sequence(push(new AstRule.Nested(null)), sp(), Optional(context()), sp(),
                    ZeroOrMore(rule()), sp(), EOI);
        }
    }
}

package net.immute.ccs.impl.parser;

import net.immute.ccs.CcsLogger;
import org.junit.Test;
import org.parboiled.support.ParsingResult;

import java.io.IOException;
import java.io.StringReader;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ParserTest {
    ParsingResult<?> parseString(Parser parser, String str) throws IOException {
        return parser.parse(new StringReader(str), "literal");
    }

    private void pass(Parser p, String ccs) throws IOException {
        assertFalse(parseString(p, ccs).hasErrors());
    }

    private void fail(Parser p, String ccs) throws IOException {
        assertTrue(parseString(p, ccs).hasErrors());
    }

    @Test
    public void parserTests() throws IOException {
        Parser p = new Parser(new CcsLogger.StderrCcsLogger());
        pass(p, "");
        pass(p, "@import 'file'");
        pass(p, "@context (foo #bar > baz)");
        pass(p, "@context (foo .bar > baz +)");
        pass(p, "prop = 'val'");
        pass(p, "elem#id {}");
        pass(p, "elem#id {prop = 'val'}");
        pass(p, ".class.class blah > elem#id {prop=43}");
        pass(p, ".class.class blah > elem#id {prop=2.3}");
        pass(p, ".class.class blah > elem#id {prop=\"val\"}");
        pass(p, ".class.class blah > elem#id {prop=0xAB12}");
        fail(p, ".class.class blah > elem# id {prop=2.3}");
        fail(p, ".class. class > elem#id {prop=\"val\"}");
        fail(p, "blah");
        fail(p, "@import 'file'; @context (foo)");
        pass(p, ".class { @import 'file' }");
        fail(p, ".class { @context + (foo) }");
        pass(p, "elem#id { prop = 'val'; prop2 = 31337 }");
        pass(p, "* > * foo *.blah { p = 1; }");
        pass(p, "[prop='val'].foo[ p = 'hmm'] { p = 1; }");
        pass(p, "a b + c d {p=1}");
        pass(p, "(a b) + (c d) {p=1}");
        pass(p, ".\"foo\" 'bar' {'test' = 1};");
    }

    @Test
    public void commentTests() throws IOException {
        Parser p = new Parser(new CcsLogger.StderrCcsLogger());
        pass(p, "// single line comment\n");
        pass(p, "// single line comment nonl");
        pass(p, "/* multi-line comment */");
        pass(p, "prop = /* comment */ 'val'");
        pass(p, "prop = /* comment /*nest*/ more */ 'val'");
        pass(p, "elem#id /* comment */ {prop = 'val'}");
        pass(p, "// comment\nelem { prop = 'val' prop = 'val' }");
    }

    @Test
    public void uglyAbutmentTests() throws IOException {
        Parser p = new Parser(new CcsLogger.StderrCcsLogger());
        fail(p, "foo {p = 1x = 2}");
        fail(p, "foo {p = 'x'x = 2}");
        pass(p, "foo {p = 1 x = 2}");
        pass(p, "foo{p=1;x=2}");
        fail(p, "foo{@localp=1}");
        pass(p, "foo{@local p=1}");
        fail(p, "foo{@overridep=1}");
        fail(p, "foo{@override@localp=1}");
        pass(p, "foo{@override @local p=1}");
    }

    @Test
    public void selectorSectionTests() throws IOException {
        Parser p = new Parser(new CcsLogger.StderrCcsLogger());
        pass(p, "foo + { bar {}}");
        pass(p, "foo + { > bar + > baz {}}");
        pass(p, "bar > baz {}");
        pass(p, "bar + baz {}");
        pass(p, "> bar + baz {}");
    }
}
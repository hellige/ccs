package net.immute.ccs.impl.parser;

import net.immute.ccs.CcsLogger;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ParserTest {
    private final Parser2 p = new Parser2(new CcsLogger.StderrCcsLogger());

    boolean parseString(String str) throws IOException {
        try {
            p.parse(new StringReader(str), "literal");
            return true;
        } catch (Parser2.ParseError e) {
            return false;
        }
    }

    private void pass(String ccs) throws IOException {
        assertTrue(parseString(ccs));
    }

    private void fail(String ccs) throws IOException {
        assertFalse(parseString(ccs));
    }

    @Test
    public void parserTests() throws IOException {
        pass("");
        pass("@import 'file'");
        pass("@context (foo.bar > baz)");
        fail("@context (foo x.bar > # baz >)");
        pass("prop = 'val'");
        pass("elem.id {}");
        pass("elem.id {prop = 'val'}");
        fail("elem.id {prop = @override 'hi'}");
        pass("a.class.class blah > elem.id {prop=3}");
        pass("a.class.class blah > elem.id {prop=2.3}");
        pass("a.class.class blah > elem.id {prop=\"val\"}");
        fail("a.class.class blah elem.id prop=\"val\" }");
        pass("a.class.class blah > elem.id {prop=0xAB12}");
        pass("a.class.class blah > elem. id {prop=2.3}");
        pass("a.class. class > elem.id {prop=\"val\"}");
        fail("blah");
        fail("@import 'file'; @context (foo)");
        fail("@yuno?");
        pass("@import 'file' ; @constrain foo");
        pass("a.class { @import 'file' }");
        fail("a.class { @context (foo) }");
        pass("elem.id { prop = 'val'; prop2 = 31337 }");
        pass("prop.'val'/a.foo/p.'hmm' { p = 1; }");
        pass("a b > c d {p=1}");
        pass("(a > b) (c > d) {p=1}");
        pass("a > b > c {p=1}");
        pass("a > (b c) > d {p=1}");
        pass("a.\"foo\" 'bar' {'test' = 1}");
    }

    @Test
    public void commentTests() throws IOException {
        pass("// single line comment\n");
        pass("// single line comment nonl");
        pass("/* multi-line comment */");
        pass("prop = /* comment */ 'val'");
        pass("prop = /*/ comment */ 'val'");
        pass("prop = /**/ 'val'");
        pass("prop = /* comment /*nest*/ more */ 'val'");
        pass("elem.id /* comment */ {prop = 'val'}");
        fail("elem.id /* comment {prop = 'val'}");
        pass("// comment\nelem { prop = 'val' prop = 'val' }");
    }

    @Test
    public void uglyAbutmentTests() throws IOException {
        fail("foo {p = 1x = 2}");
        pass("foo {p = 1x p2 = 2}");
        pass("foo {p = 'x'x = 2}");
        pass("foo {p = 1 x = 2}");
        fail("value=12asdf.foo {}");
        pass("value=12asdf.foo nextsel {}");
        pass("foo {p = 1 x = 2}");
        pass("foo{p=1;x=2}");
        fail("foo{@overridep=1}");
        pass("foo{@override /*hi*/ p=1}");
        pass("@import'asdf'");
        fail("@constrainasdf");
        pass("@import 'asdf' \n ; \n @constrain asdf \n ; @import 'foo'  ");
        pass("@import /*hi*/ 'asdf'");
        pass("env.foo/* some comment */{ }");
    }

    @Test
    public void selectorSectionTests() throws IOException {
        pass("foo > { bar {}}");
        pass("foo > { bar > baz {}}");
        pass("bar > baz {}");
        pass("bar baz {}");
    }

    @Test
    public void constraintTests() throws IOException {
        pass("a.b: @constrain a.c");
    }

    @Test
    public void interpolationTests() throws IOException {
        pass("a = 'hi'");
        fail("a = 'hi");
        fail("a = 'hi\\");
        fail("a = 'hi\\4 there'");
        pass("a = 'h${there}i'");
        fail("a = 'h$there}i'");
        fail("a = 'h${t-here}i'");
    }

    // TODO when property values are a variant or whatever instead of just strings
//    Value<?> parseAndReturnValue(String input) throws IOException {
//        AstRule.Nested ast = p.parseCcsStream(new StringReader(input),
//                "<literal>", new ImportResolver.Null(), new Stack<String>());
//        if (ast == null) return null;
//        if (ast.rules.size() != 1) return false;
//        ast::PropDef *propDef = boost::get<ast::PropDef>(&ast.rules_[0]);
//        if (!propDef) return false;
//        T *v = boost::get<T>(&propDef->value_.val_);
//        if (!v) return false;
//        out = *v;
//        return true;
//    }
//
//    @Test
//    public void integerParseTests() throws IOException {
//        int64_t v64 = 0;
//        ASSERT_TRUE(p.parseAndReturnValue("value = 100", v64));
//        EXPECT_EQ(100, v64);
//        ASSERT_TRUE(parser.parseAndReturnValue("value = 0", v64));
//        EXPECT_EQ(0, v64);
//        ASSERT_TRUE(parser.parseAndReturnValue("value = -0", v64));
//        EXPECT_EQ(0, v64);
//        ASSERT_TRUE(parser.parseAndReturnValue("value = -100", v64));
//        EXPECT_EQ(-100, v64);
//        ASSERT_TRUE(parser.parseAndReturnValue("value = 0x1a", v64));
//        EXPECT_EQ(26, v64);
//        ASSERT_TRUE(parser.parseAndReturnValue("value = 0x1F", v64));
//        EXPECT_EQ(31, v64);
//        ASSERT_TRUE(parser.parseAndReturnValue("value = 0x0", v64));
//        EXPECT_EQ(0, v64);
//        ASSERT_FALSE(parser.parseAndReturnValue("value = 100.123", v64));
//        ASSERT_FALSE(parser.parseAndReturnValue("value = '100", v64));
//    }
//
//    @Test
//    public void doubleParseTests() throws IOException {
//        double vDouble = 0.0;
//        ASSERT_TRUE(parser.parseAndReturnValue("value = 100.", vDouble));
//        EXPECT_DOUBLE_EQ(100., vDouble);
//        ASSERT_TRUE(parser.parseAndReturnValue("value = 100.0000", vDouble));
//        EXPECT_DOUBLE_EQ(100., vDouble);
//        ASSERT_TRUE(parser.parseAndReturnValue("value = 0.0000", vDouble));
//        EXPECT_DOUBLE_EQ(0., vDouble);
//        ASSERT_TRUE(parser.parseAndReturnValue("value = -0.0000", vDouble));
//        EXPECT_DOUBLE_EQ(0., vDouble);
//        ASSERT_TRUE(parser.parseAndReturnValue("value = 1.0e-2", vDouble));
//        EXPECT_DOUBLE_EQ(0.01, vDouble);
//        ASSERT_TRUE(parser.parseAndReturnValue("value = 1.0E-2", vDouble));
//        EXPECT_DOUBLE_EQ(0.01, vDouble);
//        ASSERT_TRUE(parser.parseAndReturnValue("value = 1e-2", vDouble));
//        EXPECT_DOUBLE_EQ(0.01, vDouble);
//        ASSERT_TRUE(parser.parseAndReturnValue("value = 1E-2", vDouble));
//        EXPECT_DOUBLE_EQ(0.01, vDouble);
//        ASSERT_FALSE(parser.parseAndReturnValue("value = 100", vDouble));
//        ASSERT_FALSE(parser.parseAndReturnValue("value = '100.0", vDouble));
//    }
}
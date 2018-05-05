package net.immute.ccs;

import net.immute.ccs.impl.parser.Parser;
import net.immute.ccs.impl.parser.Parser.Token.Type;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class AcceptanceTest {
    private final String name;
    private final String ccs;
    private final List<String> assertions;

    public AcceptanceTest(String name, String ccs, List<String> assertions) {
        this.name = name;
        this.ccs = ccs;
        this.assertions = assertions;
    }

    private static void expect(BufferedReader reader, String expect) throws Exception {
        assertEquals(expect, reader.readLine());
    }

    private static void doUntil(BufferedReader reader, String delim, Consumer<String> f) throws Exception {
        String line;
        while (!(line = reader.readLine()).equals(delim)) // eof will throw an NPE, which is fine
            f.accept(line);
    }

    private static String readUntil(BufferedReader reader, String delim) throws Exception {
        StringBuilder result = new StringBuilder();
        doUntil(reader, delim, str -> { result.append(str); result.append("\n"); });
        return result.toString();
    }

    private static Object[] parseTestCase(BufferedReader reader) throws Exception {
        String name = reader.readLine();
        if (name == null) return null;
        expect(reader, "---");
        String ccs = readUntil(reader, "---");
        List<String> assertions = new ArrayList<>();
        doUntil(reader, "===", assertions::add);
        expect(reader, "");
        return new Object[] {name, ccs, assertions};
    }

    @Parameterized.Parameters(name="{index}: {0}")
    public static Iterable<Object[]> data() throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                AcceptanceTest.class.getResourceAsStream("/tests.txt")));
        List<Object[]> tests = new ArrayList<>();
        Object[] test;
        while ((test = parseTestCase(reader)) != null)
            tests.add(test);
        return tests;
    }

    @Test
    public void runTest() throws Exception {
        System.out.println("Test case: " + name);
        System.out.println(" CCS:\n" + ccs + " ---");
        CcsContext root = load(name, ccs);
        for (String assertion : assertions)
            parseAssertion(name, root, assertion);
        System.out.println("   PASSED\n");
    }

    private CcsContext load(String name, String ccs) throws IOException {
        return new CcsDomain()
                .loadCcsStream(new StringReader(ccs), "<test case: " + name + ">", new ImportResolver.Null())
                .build();
    }

    private String expectTok(Parser.Lexer lex, Type type) {
        Parser.Token tok = lex.consume();
        if (tok.type != type)
            fail("Expected token of type " + type + " in test case, but found " + tok);
        return tok.value.toString();
    }

    private void parseAssertion(String testName, CcsContext ctx, String line) {
        Parser.Lexer lex = new Parser.Lexer(new StringReader(line));
        System.out.println(" Assertion: " + line);
        String msg = "Test case: " + testName + ". Assertion: " + line;
        CcsContext.Builder builder = ctx.builder();
        boolean same = true;
        while (lex.peek().type != Type.EOS) {
            switch (lex.peek().type) {
                case IDENT: {
                    if (!same) builder = builder.build().builder();
                    same = false;
                    String name = lex.consume().value.toString();
                    if (lex.peek().type == Type.EQ) {
                        // this is a bit of a special case: there's no constraints at all, just a property query on the
                        // root context
                        finishAssertion(msg, lex, builder.build(), name);
                        continue;
                    }
                    List<String> values = new ArrayList<>();
                    while (lex.peek().type == Type.DOT) {
                        lex.consume();
                        values.add(expectTok(lex, Type.IDENT));
                    }
                    builder.add(name, values.toArray(new String[values.size()]));
                    break;
                }

                case SLASH:
                    same = true;
                    lex.consume();
                    break;

                case COLON: {
                    lex.consume();
                    finishAssertion(msg, lex, builder.build(), expectTok(lex, Type.IDENT));
                    break;
                }

                default: {
                    fail("Test case " + testName + ": Unexpected token in test case : " + lex.peek());
                }
            }
        }
    }

    private void finishAssertion(String msg, Parser.Lexer lex, CcsContext ctx, String name) {
        expectTok(lex, Type.EQ);
        String val;
        switch (lex.peek().type) {
            case INT:
                val = expectTok(lex, Type.INT);
                break;
            default:
                val = expectTok(lex, Type.IDENT);
                break;
        }
        assertEquals(msg, val, ctx.getString(name));
    }
}

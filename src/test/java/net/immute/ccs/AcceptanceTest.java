package net.immute.ccs;

import net.immute.ccs.impl.parser.Parser;
import net.immute.ccs.impl.parser.Parser.Token.Type;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class AcceptanceTest {
    private CcsContext load(String name, String ccs) throws IOException {
        return new CcsDomain()
                .loadCcsStream(new StringReader(ccs), "<test case: " + name + ">", new ImportResolver.Null())
                .build();
    }

    private void expect(BufferedReader reader, String expect) throws Exception {
        assertEquals(expect, reader.readLine());
    }

    private void doUntil(BufferedReader reader, String delim, Consumer<String> f) throws Exception {
        String line;
        while (!(line = reader.readLine()).equals(delim)) // eof will throw an NPE, which is fine
            f.accept(line);
    }

    private String readUntil(BufferedReader reader, String delim) throws Exception {
        StringBuilder result = new StringBuilder();
        doUntil(reader, delim, str -> { result.append(str); result.append("\n"); });
        return result.toString();
    }

    private boolean parseTestCase(BufferedReader reader) throws Exception {
        String name = reader.readLine();
        if (name == null) return false;
        System.out.println("Test case: " + name);
        expect(reader, "---");
        String ccs = readUntil(reader, "---");
        System.out.println(" CCS:\n" + ccs + "\n---");
        CcsContext root = load(name, ccs);
        doUntil(reader, "===", line -> parseAssertion(name, root, line));
        expect(reader, "");
        System.out.println("   PASSED");
        return true;
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

    @Test
    public void acceptanceTests() throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/tests.txt")));
        //noinspection StatementWithEmptyBody
        while (parseTestCase(reader));
    }
}

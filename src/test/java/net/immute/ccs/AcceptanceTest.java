package net.immute.ccs;

import net.immute.ccs.impl.parser.Parser;
import net.immute.ccs.impl.parser.Parser.Token.Type;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
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
        while (lex.peek().type != Type.EOS) {
            switch (lex.peek().type) {
                case IDENT: {
                    String name = lex.consume().value.toString();
                    if (lex.peek().type == Type.DOT) {
                        lex.consume();
                        String value = expectTok(lex, Type.IDENT);
                        ctx = ctx.constrain(name, value);
                    } else {
                        ctx = ctx.constrain(name);
                    }
                    break;
                }

                case COLON: {
                    lex.consume();
                    String name = expectTok(lex, Type.IDENT);
                    expectTok(lex, Type.EQ);
                    String val = expectTok(lex, Type.IDENT);
                    assertEquals("Test case: " + testName + ". Assertion: " + line,
                                 val, ctx.getString(name));
                    break;
                }

                default: {
                    fail("Test case " + testName + ": Unexpected token in test case : " + lex.peek());
                }
            }
        }
    }

    @Test
    public void acceptanceTests() throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/tests.txt")));
        //noinspection StatementWithEmptyBody
        while (parseTestCase(reader));
    }
}

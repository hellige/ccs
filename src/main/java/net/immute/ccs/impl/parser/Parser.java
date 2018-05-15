package net.immute.ccs.impl.parser;

import net.immute.ccs.CcsLogger;
import net.immute.ccs.ImportResolver;
import net.immute.ccs.Origin;
import net.immute.ccs.impl.dag.DagBuilder;
import net.immute.ccs.impl.dag.Key;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.NoSuchElementException;
import java.util.Stack;
import java.util.regex.Pattern;

public class Parser {
    private final CcsLogger log;

    public Parser(CcsLogger log) {
        this.log = log;
    }

    CcsLogger getLogger() {
        return log;
    }

    public void loadCcsStream(Reader reader, String fileName, DagBuilder dag, ImportResolver importResolver)
            throws IOException {
        AstRule rule = parseCcsStream(reader, fileName, importResolver, new Stack<>());
        if (rule == null) return;

        // everything parsed, no errors. now it's safe to modify the dag...
        rule.addTo(dag.getBuildContext());
    }

    AstRule.Nested parseCcsStream(Reader stream, String fileName, ImportResolver importResolver,
                                  Stack<String> inProgress) throws IOException {
        try (Reader reader = new BufferedReader(stream)) {
            AstRule.Nested rule = new ParserImpl(fileName, reader).parseRuleset();
            if (!rule.resolveImports(importResolver, this, inProgress)) return null;
            return rule;
        } catch (ParseError e) {
            log.error("Errors parsing " + fileName + ":" + e);
            return null;
        }
    }

    AstRule parse(Reader stream, String fileName) throws IOException {
        try (Reader reader = new BufferedReader(stream)) {
            return new ParserImpl(fileName, reader).parseRuleset();
        }
    }

    private static class Location {
        final int line;
        private final int column;

        Location(int line, int column) {
            this.line = line;
            this.column = column;
        }

        @Override
        public String toString() {
            return "<" + line + ":" + column + '>';
        }
    }

    static class ParseError extends RuntimeException {
        ParseError(Location where, String what) {
            super(where + ": " + what);
        }
    }

// TODO collapse IDENT and STRING?

    // TODO this should be cleaned up, all these stupid "values" is dumb and
// confusing...
    public static class Token {
        public enum Type {
            EOS,
            LPAREN, RPAREN, LBRACE, RBRACE, SEMI, COLON, COMMA, DOT, GT, EQ, SLASH,
            CONSTRAIN, CONTEXT, IMPORT, OVERRIDE,
            INT, DOUBLE, IDENT, NUMID, STRING
        }

        public Type type;
        public final Location location;
        public final StringBuilder value = new StringBuilder();
        StringVal stringValue;
        long intValue; // only valid in case of ints...
        double doubleValue; // only valid in case of numbers...

        Token(Type type, Location loc) {
            this.type = type;
            this.location = loc;
        }

        Token(Type type, char first, Location loc) {
            this.type = type;
            this.location = loc;
            value.append(first);
        }

        void append(char c) { value.append(c); }

        boolean hasValue(String s) {
            return value.toString().equals(s);
        }

        @Override
        public String toString() {
            switch (type) {
                case EOS: return "end-of-input";
                case LPAREN: return "'('";
                case RPAREN: return "')";
                case LBRACE: return  "'{'";
                case RBRACE: return "'}'";
                case SEMI: return "';'";
                case COLON: return "':'";
                case COMMA: return "','";
                case DOT: return "'.'";
                case GT: return "'>'";
                case EQ: return "'='";
                case SLASH: return "'/'";
                case CONSTRAIN: return "'@constrain'";
                case CONTEXT: return "'@context'";
                case IMPORT: return "'@import'";
                case OVERRIDE: return "'@override'";
                case INT: return "integer";
                case DOUBLE: return "double";
                case IDENT: return "identifier";
                case NUMID: return "numeric/identifier";
                case STRING: return "string literal";
            }
            throw new NoSuchElementException("Unknown token type: " + type);
        }
    }

    private static class Buf {
        static final int EOF = -1;

        private final Reader stream;
        private int line = 1;
        private int column = 0;
        private int peekChar;

        Buf(Reader stream) {
            this.stream = stream;
            try {
                peekChar = stream.read();
            } catch (IOException e) {
                throw new ParseError(new Location(line, column), "IOException reading from stream: " + e);
            }
        }

        int get() {
            try {
                int c = peekChar;
                peekChar = stream.read();
                // this way of tracking location gives funny results when get() returns
                // a newline, but we don't actually care about that anyway...
                column++;
                if (c == '\n') {
                    line++;
                    column = 0;
                }
                return c;
            } catch (IOException e) {
                throw new ParseError(new Location(line, column), "IOException reading from stream: " + e);
            }
        }

        int peek() {
            return peekChar;
        }

        Location location() { return new Location(line, column); }
        Location peekLocation() { return new Location(line, column+1); }
    }

    @SuppressWarnings("RedundantIfStatement")
    public static class Lexer {
        private final Buf stream;
        private Token next;

        public Lexer(Reader stream) {
            this.stream = new Buf(stream);
            next = nextToken();
        }

        public Token peek() { return next; }

        public Token consume() {
            Token tmp = next;
            next = nextToken();
            return tmp;
        }

        private Token nextToken() {
            int c = stream.get();

            while (Character.isWhitespace(c) || comment(c)) c = stream.get();

            Location where = stream.location();

            switch (c) {
                case Buf.EOF: return new Token(Token.Type.EOS, where);
                case '(': return new Token(Token.Type.LPAREN, where);
                case ')': return new Token(Token.Type.RPAREN, where);
                case '{': return new Token(Token.Type.LBRACE, where);
                case '}': return new Token(Token.Type.RBRACE, where);
                case ';': return new Token(Token.Type.SEMI, where);
                case ':': return new Token(Token.Type.COLON, where);
                case ',': return new Token(Token.Type.COMMA, where);
                case '.': return new Token(Token.Type.DOT, where);
                case '>': return new Token(Token.Type.GT, where);
                case '=': return new Token(Token.Type.EQ, where);
                case '/': return new Token(Token.Type.SLASH, where);
                case '@': {
                    Token tok = ident(c, where);
                    if (tok.hasValue("@constrain")) tok.type = Token.Type.CONSTRAIN;
                    else if (tok.hasValue("@context")) tok.type = Token.Type.CONTEXT;
                    else if (tok.hasValue("@import")) tok.type = Token.Type.IMPORT;
                    else if (tok.hasValue("@override")) tok.type = Token.Type.OVERRIDE;
                    else throw new ParseError(where, "Unrecognized @-command: " + tok.value);
                    return tok;
                }
                case '\'': return string(c, where);
                case '"': return string(c, where);
            }

            if (numIdInitChar(c)) return numId(c, where);
            if (identInitChar(c)) return ident(c, where);

            throw new ParseError(where, "Unexpected character: '" + (char)c + "' (0x" + Integer.toHexString(c) + ")");
        }

        private boolean comment(int c) {
            if (c != '/') return false;
            if (stream.peek() == '/') {
                stream.get();
                //noinspection StatementWithEmptyBody
                for (int tmp = stream.get(); tmp != '\n' && tmp != Buf.EOF; tmp = stream.get());
                return true;
            } else if (stream.peek() == '*') {
                stream.get();
                multilineComment();
                return true;
            }
            return false;
        }

        private void multilineComment() {
            while (true) {
                int c = stream.get();
                if (c == Buf.EOF)
                    throw new ParseError(stream.location(), "Unterminated multi-line comment");
                if (c == '*' && stream.peek() == '/') {
                    stream.get();
                    return;
                }
                if (c == '/' && stream.peek() == '*') {
                    stream.get();
                    multilineComment();
                }
            }
        }

        private boolean identInitChar(int c) {
            if (c == '$') return true;
            if (c == '_') return true;
            if ('A' <= c && c <= 'Z') return true;
            if ('a' <= c && c <= 'z') return true;
            return false;
        }

        private boolean identChar(int c) {
            if (identInitChar(c)) return true;
            if ('0' <= c && c <= '9') return true;
            return false;
        }

        private boolean interpolantChar(int c) {
            if (c == '_') return true;
            if ('0' <= c && c <= '9') return true;
            if ('A' <= c && c <= 'Z') return true;
            if ('a' <= c && c <= 'z') return true;
            return false;
        }

        private Token string(int first, Location where) {
            StringVal result = new StringVal();
            StringBuilder current = new StringBuilder();
            while (stream.peek() != first) {
                switch (stream.peek()) {
                    case Buf.EOF:
                        throw new ParseError(stream.peekLocation(), "Unterminated string literal");
                    case '$': {
                        stream.get();
                        if (stream.peek() != '{')
                            throw new ParseError(stream.peekLocation(), "Expected '{'");
                        stream.get();
                        if (current.length() != 0) result.addLiteral(current);
                        current.setLength(0);
                        StringBuilder interpolant = new StringBuilder();
                        while (stream.peek() != '}') {
                            if (!interpolantChar(stream.peek()))
                                throw new ParseError(stream.peekLocation(),
                                                     "Character not allowed in string interpolant: '" +
                                                             (char)stream.peek() + "' (0x" + Integer.toHexString(stream.peek())
                                                             + ")");
                            interpolant.append((char)stream.get());
                        }
                        stream.get();
                        result.addInterpolant(interpolant);
                        break;
                    }
                    case '\\': {
                        stream.get();
                        int escape = stream.get();
                        switch (escape) {
                            case Buf.EOF: throw new ParseError(stream.location(), "Unterminated string literal");
                            case '$': current.append('$'); break;
                            case '\'': current.append('\''); break;
                            case '"': current.append('"'); break;
                            case '\\': current.append('\\'); break;
                            case 't': current.append('\t'); break;
                            case 'n': current.append('\n'); break;
                            case 'r': current.append('\r'); break;
                            case '\n': break; // escaped newline: ignore
                            default: throw new ParseError(stream.location(), "Unrecognized escape sequence: '\\"
                                    + (char)escape + "' (0x" + Integer.toHexString(escape) + ")");
                        }
                        break;
                    }
                    default:
                        current.append((char)stream.get());
                        break;
                }
            }
            stream.get();
            if (current.length() != 0) result.addLiteral(current);
            Token tok = new Token(Token.Type.STRING, where);
            tok.stringValue = result;
            return tok;
        }

        private boolean numIdInitChar(int c) {
            if ('0' <= c && c <= '9') return true;
            if (c == '-' || c == '+') return true;
            return false;
        }

        private boolean numIdChar(int c) {
            if (numIdInitChar(c)) return true;
            if (identChar(c)) return true;
            if (c == '.') return true;
            return false;
        }

        private final Pattern intRe = Pattern.compile("[-+]?[0-9]+");
        private final Pattern doubleRe = Pattern.compile("[-+]?[0-9]+\\.?[0-9]*([eE][-+]?[0-9]+)?");

        private Token numId(int first, Location where) {
            if (first == '0' && stream.peek() == 'x') {
                stream.get();
                return hexLiteral(where);
            }

            Token token = new Token(Token.Type.NUMID, (char)first, where);
            while (numIdChar(stream.peek())) token.append((char)stream.get());

            if (intRe.matcher(token.value).matches()) {
                try {
                    token.type = Token.Type.INT;
                    token.intValue = Long.valueOf(token.value.toString());
                    return token;
                } catch (NumberFormatException e) {
                    throw new ParseError(where, "Internal error: "
                            + "integer regex matched, but Long.valueOf() failed! '"
                            + token.value + "': " + e);
                }
            } else if (doubleRe.matcher(token.value).matches()) {
                try {
                    token.type = Token.Type.DOUBLE;
                    token.doubleValue = Double.valueOf(token.value.toString());
                    return token;
                } catch (NumberFormatException e) {
                    throw new ParseError(where, "Internal error: "
                            + "double regex matched, but lexical cast failed! '"
                            + token.value + "': " + e);
                }
            }

            return token; // it's a generic NUMID
        }

        private int hexChar(int c) {
            if ('0' <= c && c <= '9') return c - '0';
            if ('a' <= c && c <= 'f') return 10 + c - 'a';
            if ('A' <= c && c <= 'F') return 10 + c - 'A';
            return -1;
        }

        private Token hexLiteral(Location where) {
            Token token = new Token(Token.Type.INT, where);
            for (int n = hexChar(stream.peek()); n != -1;
                 n = hexChar(stream.peek())) {
                token.intValue = token.intValue * 16 + n;
                token.append((char)stream.get());
            }
            token.doubleValue = token.intValue;
            return token;
        }

        private Token ident(int first, Location where) {
            Token token = new Token(Token.Type.IDENT, (char)first, where);
            while (identChar(stream.peek())) token.append((char)stream.get());
            return token;
        }
    }

    private static class ParserImpl {
        private final String fileName;
        private final Lexer lex;
        private Token cur;
        private Token last;

        ParserImpl(String fileName, Reader stream) {
            this.fileName = fileName;
            lex = new Lexer(stream);
        }

        AstRule.Nested parseRuleset() {
            AstRule.Nested ast = new AstRule.Nested();
            advance();
            if (advanceIf(Token.Type.CONTEXT)) ast.setSelector(parseContext());
            while (cur.type != Token.Type.EOS) parseRule(ast);
            return ast;
        }

        private void advance() { last = cur; cur = lex.consume(); }

        private boolean advanceIf(Token.Type type) {
            if (cur.type == type) {
                advance();
                return true;
            }
            return false;
        }

        private void expect(Token.Type type) {
            if (!advanceIf(type))
                throw new ParseError(cur.location, "Expected " + type + ", found " + cur.type);
        }

        private SelectorBranch parseContext() {
            expect(Token.Type.LPAREN);
            SelectorBranch result = parseSelector();
            expect(Token.Type.RPAREN);
            advanceIf(Token.Type.SEMI);
            return result;
        }

        private void parseRule(AstRule.Nested ast) {
            // the only ambiguity is between ident as start of a property setting
            // and ident as start of a selector, i.e.:
            //   foo = bar
            //   foo : bar = 'baz'
            // we can use the presence of '=' to disambiguate. parsePrimRule() performs
            // this lookahead without consuming the additional token.
            if (parsePrimRule(ast)) {
                advanceIf(Token.Type.SEMI);
                return;
            }

            AstRule.Nested nested = new AstRule.Nested(parseSelector());

            if (advanceIf(Token.Type.COLON)) {
                if (!parsePrimRule(nested))
                    throw new ParseError(cur.location,
                                         "Expected @import, @constrain, or property setting");
                advanceIf(Token.Type.SEMI);
            } else if (advanceIf(Token.Type.LBRACE)) {
                while (!advanceIf(Token.Type.RBRACE)) parseRule(nested);
            } else {
                throw new ParseError(cur.location, "Expected ':' or '{' following selector");
            }

            ast.append(nested);
        }

        private boolean parsePrimRule(AstRule.Nested ast) {
            switch (cur.type) {
                case IMPORT:
                    advance();
                    expect(Token.Type.STRING);
                    if (last.stringValue.interpolation())
                        throw new ParseError(last.location, "Interpolation not allowed in import statements");
                    ast.append(new AstRule.Import(last.stringValue.str()));
                    return true;
                case CONSTRAIN:
                    advance();
                    ast.append(new AstRule.Constraint(parseSingleStep()));
                    return true;
                case OVERRIDE:
                    advance();
                    ast.append(parseProperty(true));
                    return true;
                case IDENT:
                case STRING:
                    if (lex.peek().type == Token.Type.EQ) {
                        ast.append(parseProperty(false));
                        return true;
                    }
                    break;
            }
            return false;
        }

        private AstRule.PropDef parseProperty(boolean override) {
            String name = parseIdent("property name");
            expect(Token.Type.EQ);

            // we set the origin from the location of the equals sign. it's a bit
            // arbitrary, but it seems as good as anything.
            Origin origin = new Origin(fileName, last.location.line);

            Value<?> value;

            switch (cur.type) {
                case INT:
                    value = new Value<>(cur.intValue); break;
                case DOUBLE:
                    value = new Value<>(cur.doubleValue); break;
                case STRING:
                    value = new Value<>(cur.stringValue); break;
                case NUMID:
                    value = new Value<>(new StringVal(cur.value.toString())); break;
                case IDENT:
                    if (cur.hasValue("true")) value = new Value<>(true);
                    else if (cur.hasValue("false")) value = new Value<>(false);
                    else value = new Value<>(new StringVal(cur.value.toString()));
                    break;
                default:
                    throw new ParseError(cur.location, cur.type
                            + " cannot occur here. Expected property value "
                            + "(number, identifier, string, or boolean)");
            }
            advance();
            return new AstRule.PropDef(name, value, origin, override);
        }

        private SelectorBranch parseSelector() {
            SelectorLeaf leaf = parseSum();
            if (advanceIf(Token.Type.GT)) {
                throw new UnsupportedOperationException(); // TODO
                //return SelectorBranch.descendant(leaf);
            } else {
                return SelectorBranch.conjunction(leaf);
            }
        }

        private SelectorLeaf parseSum() {
            SelectorLeaf left = parseProduct();
            while (advanceIf(Token.Type.COMMA))
                left = left.disjunction(parseProduct());
            return left;
        }

        private boolean couldStartStep(Token token) {
            switch(token.type) {
                case IDENT:
                case STRING:
                case LPAREN:
                    return true;
                default:
                    return false;
            }
        }

        private SelectorLeaf parseProduct() {
            SelectorLeaf left = parseTerm();
            // term starts with ident or '(', which is enough to disambiguate...
            while (couldStartStep(cur))
                left = left.conjunction(parseTerm());
            return left;
        }

        private SelectorLeaf parseTerm() {
            SelectorLeaf left = parseStep();
            while (cur.type == Token.Type.GT) {
                // here we have to distinguish another step from a trailing '>'. again,
                // peeking for ident or '(' does the trick.
                if (!couldStartStep(lex.peek())) return left;
                advance();
                left = left.descendant(parseStep());
            }
            return left;
        }

        private SelectorLeaf parseStep() {
            if (advanceIf(Token.Type.LPAREN)) {
                SelectorLeaf result = parseSum();
                expect(Token.Type.RPAREN);
                return result;
            } else {
                return SelectorLeaf.step(parseSingleStep());
            }
        }

        private Key parseSingleStep() {
            Key key = new Key();
            do {
                String name = parseIdent("selector name");
                key.addName(name);
                while (advanceIf(Token.Type.DOT))
                    key.addValue(name, parseIdent("selector value"));
            } while (advanceIf(Token.Type.SLASH));
            return key;
        }

        private String parseIdent(String what) {
            if (advanceIf(Token.Type.IDENT)) return last.value.toString();
            if (advanceIf(Token.Type.STRING)) {
                if (last.stringValue.interpolation())
                    throw new ParseError(last.location, "Interpolation not allowed in " + what);
                return last.stringValue.str();
            }
            throw new ParseError(cur.location, cur.type + " cannot occur here. Expected " + what);
        }
    }
}
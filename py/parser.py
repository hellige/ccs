import re

class ParseError(Exception):
    pass # TODO


class Location:
    def __init__(self, line, column):
        self.line = line
        self.column = column

    def __str__(self):
        return '<{}:{}>'.format(self.line, self.column)


class Token:
    EOS = 'end-of-input'
    LPAREN = "'('"
    RPAREN = "')'"
    LBRACE = "'{'"
    RBRACE = "'}'"
    SEMI = "';'"
    COLON = "':'"
    COMMA = "','"
    DOT = "'.'"
    GT = "'>'" # TODO delete!
    EQ = "'='"
    CONSTRAIN = "'@constrain'"
    CONTEXT = "'@context'"
    IMPORT = "'@import'"
    OVERRIDE = "'@override'"
    INT = 'integer'
    DOUBLE = 'double'
    IDENT = 'identifier'
    NUMID = 'numeric/identifier'
    STRING = 'string literal'

    def __init__(self, typ, location):
        self.type = typ
        self.location = location

    def __str__(self):
        return self.type


class Buf:
    EOF = -1

    def __init__(self, stream):
        self.stream = stream
        self.line = 1
        self.column = 0
        self.peek_char = self.stream.read()

    def get(self):
        c = self.peek_char
        self.peek_char = self.stream.read()
        # this way of tracking location gives funny results when get() returns
        # a newline, but we don't actually care about that anyway...
        self.column += 1
        if c == '\n':
            self.line += 1
            self.column = 0
        return c

    def peek(self):
        return self.peek_char

    def location(self):
        return Location(self.line, self.column)

    def peek_location(self):
        return Location(self.line, self.column+1)


class Lexer:
    def __init__(self, stream):
        self.stream = Buf(stream)
        self.next = self.next_token()

    def peek(self):
        return self.next
    
    def consume(self):
        tmp = self.next
        self.next = self.next_token()
        return tmp

    def next_token(self):
        c = self.stream.get()

        while c.isspace() or self.comment(c):
            c = self.stream.get()

        where = self.stream.location()

        def const(typ): return lambda c, loc: Token(typ, loc)

        recognizers = {
            Buf.EOF: const(Token.EOS),
            '(': const(Token.LPAREN),
            ')': const(Token.RPAREN),
            '{': const(Token.LBRACE),
            '}': const(Token.RBRACE),
            ';': const(Token.SEMI),
            ':': const(Token.COLON),
            ',': const(Token.COMMA),
            '.': const(Token.DOT),
            '>': const(Token.GT),
            '=': const(Token.EQ),
            '@': self.command,
            '\'': self.string,
            '"': self.string
        }

        if c in recognizers:
            return recognizers[c](c, where)

        if self.numid_init_char(c):
            return self.numid(c, where)

        if self.ident_init_char(c):
            return self.ident(c, where)

        raise ParseError(where, 
            "Unexpected character: '{}' (0x{})".format(c, hex(ord(c))))

    def command(self, c, where):
        tok = self.ident(c, where)
        if tok.has_value('@constrain'): tok.type = Token.CONSTRAIN
        elif tok.has_value('@context'): tok.type = Token.CONTEXT
        elif tok.has_value('@import'): tok.type = Token.IMPORT
        elif tok.has_value('@override'): tok.type = Token.OVERRIDE
        else:
            raise ParseError(where,
                "Unrecognized @-command: {}".format(tok.value))

    def comment(self, c):
        if c != '/': return False
        if self.stream.peek() == '/':
            self.stream.get()
            tmp = self.stream.get()
            while tmp != '\n' and tmp != Buf.EOF:
                tmp = self.stream.get()
            return True
        elif self.stream.peek() == '*':
            self.stream.get()
            self.multiline_comment()
            return True
        return False

    def multiline_comment(self):
        while True:
            c = self.stream.get()
            if c == Buf.EOF:
                raise ParseError(self.stream.location(),
                    "Unterminated multi-line comment")
            if c == '*' and self.stream.peek() == '/':
                self.stream.get()
                return
            if c == '/' and self.stream.peek() == '*':
                self.stream.get()
                self.multiline_comment()

    def ident_init_char(self, c):
        if c == '$': return True
        if c == '_': return True
        if 'A' <= c and c <= 'Z': return True
        if 'a' <= c and c <= 'z': return True
        return False

    def ident_char(self, c):
        if self.ident_init_char(c): return True
        if '0' <= c and c <= '9': return True
        return False

    def numid_init_char(self, c):
        if '0' <= c and c <= '9': return True
        if c == '-' or c == '+': return True
        return False

    def numid_char(self, c):
        if self.numid_init_char(c): return True
        if self.ident_char(c): return True
        if c == '.': return True
        return False

    def interpolant_char(self, c):
        if c == '_': return True
        if '0' <= c and c <= '9': return True
        if 'A' <= c and c <= 'Z': return True
        if 'a' <= c and c <= 'z': return True
        return False

    def string(self, first, where):
        result = StringVal() # TODO write me!
        current = '' # TODO ok to just use strings and +=???
        while self.stream.peek() != first:
            peek = self.stream.peek()
            if peek == Buf.EOF:
                raise ParseError(self.stream.peek_location(),
                    "Unterminated string literal")
            elif peek == '$':
                self.stream.get()
                if self.stream.peek() != '{':
                    raise ParseError(self.stream.peek_location(),
                        "Expected '{'")
                self.stream.get()
                if len(current) > 0:
                    result.add_literal(current)
                current = ''
                interpolant = ''
                while self.stream.peek() != '}':
                    if not self.interpolant_char(self.stream.peek()):
                        raise ParseError(self.stream.peek_location(),
                            "Character not allowed in string interpolant: {} (0x{})".format(
                                self.stream.peek(), hex(ord(self.stream.peek()))))
                    interpolant += self.stream.get()
                self.stream.get()
                result.add_interpolant(interpolant)
            elif peek == '\\':
                self.stream.get()
                escape = self.stream.get()
                escapes = "$'\"\\tnr"
                if escape in escapes:
                    current += escape
                elif escape == '\n':
                    pass # escaped newline: ignore
                else:
                    raise ParseError(self.stream.location(),
                        "Unrecognized escape sequence: '\\{}' (0x{})".format(
                            escape, hex(ord(escape))))
            else:
                current += self.stream.get()
        self.stream.get()
        if len(current) > 0:
            result.add_literal(current)
        tok = Token(Token.STRING, where)
        tok.string_value = result # TODO field needs to exist!
        return tok
    
    INT_RE = re.compile('[-+]?[0-9]+')
    DOUBLE_RE = re.compile('[-+]?[0-9]+\\.?[0-9]*([eE][-+]?[0-9]+)?')

    def numid(self, first, where):
        if first == '0' and self.stream.peek() == 'x':
            self.stream.get()
            return self.hex_literal(where)

        token = Token(Token.NUMID, first, where) # TODO this cons doesn't exist

        while self.numid_char(self.stream.peek()):
            token.append(self.stream.get())
        
        if self.INT_RE.matches(token.value):
            token.type = Token.INT
            token.int_value = int(token.value)
        elif self.DOUBLE_RE.matches(token.value):
            token.type = Token.DOUBLE
            token.double_value = float(token.value)

        return token # it's a generic NUMID

    def hex_char(self, c):
        if '0' <= c and c <= '9': return ord(c) - ord('0')
        if 'a' <= c and c <= 'f': return 10 + ord(c) - ord('a')
        if 'A' <= c and c <= 'F': return 10 + ord(c) - ord('A')
        return -1

    def hex_literal(self, where):
        token = Token(Token.INT, where)
        n = self.hex_char(self.stream.peek())
        while n != -1:
            token.intValue = token.intValue * 16 + n
            token.append(self.stream.get())
            n = self.hex_char(self.stream.peek())
        token.doubleValue = token.intValue
        return token

    def ident(self, first, where):
        token = Token(Token.IDENT, first, where) # TODO again no cons
        while self.ident_char(self.stream.peek()):
            token.append(self.stream.get())
        return token
        

class Parser:
    def __init__(self, filename, stream):
        self.filename = filename
        self.lex = Lexer(stream)
        self.cur = None
        self.last = None
    
    def parse_ruleset(self):
        ast = ast.Nested()
        self.advance()
        if self.advance_if(Token.CONTEXT):
            ast.set_selector(self.parse_context())
        while self.cur.type != Token.EOS:
            self.parse_rule(ast)
        return ast

    def advance(self):
        self.last = self.cur
        self.cur = self.lex.consume()

    def advance_if(self, typ):
        if self.cur.type == typ:
            self.advance()
            return True
        return False

    def expect(self, typ):
        if not self.advance_if(typ):
            raise ParseError(self.cur.location,
                f"Expected {typ}, found {self.cur.type}")

    def parse_context(self):
        self.expect(Token.LPAREN)
        result = self.parse_selector()
        self.expect(Token.RPAREN)
        self.advance_if(Token.SEMI)
        return result

    def parse_rule(self, ast):
        # the only ambiguity is between ident as start of a property setting
        # and ident as start of a selector, i.e.:
        #   foo = bar
        #   foo : bar = 'baz'
        # we can use the presence of '=' to disambiguate. parse_primrule() performs
        # this lookahead without consuming the additional token.
        if self.parse_primrule(ast):
            self.advance_if(Token.SEMI)
            return

        nested = ast.Nested(self.parse_selector())

        if self.advance_if(Token.COLON):
            if not self.parse_primrule(nested):
                raise ParseError(self.cur.location,
                    "Expected @import, @constrain, or property setting")
            self.advance_if(Token.SEMI)
        elif self.advance_if(Token.LBRACE):
            while not self.advance_if(Token.RBRACE):
                self.parse_rule(nested)
        else:
            raise ParseError(self.cur.location,
                "Expected ':' or '{' following selector")

        ast.append(nested)
        
    def parse_primrule(self, ast):
        if self.cur.type == Token.IMPORT:
            self.advance()
            self.expect(Token.STRING)
            if self.last.string_value.interpolation():
                raise ParseError(self.last.location,
                    "Interpolation not allowed in import statements")
            ast.append(ast.Import(self.last.string_value.str()))
            return True
        elif self.cur.type == Token.CONSTRAIN:
            self.advance()
            ast.append(ast.Constraint(self.parse_single_step()))
            return True
        elif self.cur.type == Token.OVERRIDE:
            self.advance()
            ast.append(self.parse_property(True))
            return True
        elif self.cur.type in [Token.IDENT, Token.STRING]:
            if self.lex.peek().type == Token.EQ:
                ast.append(self.parse_property(False))
                return True
        return False
        
    def parse_property(self, override):
        name = self.parse_ident("property name")
        self.expect(Token.EQ)

        # we set the origin from the location of the equals sign. it's a bit
        # arbitrary, but it seems as good as anything.
        origin = Origin(self.filename, self.last.location.line)

        # TODO this is very different from the java code but it sure
        # would be nice to keep this simpler... if this doesn't work out,
        # refer to the java/c++ code for the typed Value stuff
        if self.cur.type not in [Token.INT, Token.DOUBLE, Token.STRING,
                Token.NUMID, Token.IDENT]:
            raise ParseError(self.cur.location,
                f"{self.cur.type} cannot occur here. Expected property value "
                + "(number, identifier, string, or boolean)")

        result = ast.PropDef(name, self.cur.value, origin, override)
        self.advance()
        return result
            
    def parse_selector(self):
        leaf = self.parse_sum()
        if (self.advance_if(Token.GT)):
            raise ParseError(self.cur.location, "No longer supported") # TODO
        return SelectorBranch.conjunction(leaf)

    def parse_sum(self):
        left = self.parse_product()
        while self.advance_if(Token.COMMA):
            left = left.disjunction(self.parse_product())
        return left
    
    def could_start_step(self, token):
        return token.type in [Token.IDENT, Token.STRING, Token.LPAREN]

    def parse_product(self):
        left = self.parse_term()
        # term starts with ident of '(', which is enough to disambiguate...
        while self.could_start_step(self.cur):
            left = left.conjunction(self.parse_term())
        return left
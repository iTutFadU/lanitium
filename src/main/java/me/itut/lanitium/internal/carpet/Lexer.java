package me.itut.lanitium.internal.carpet;

import carpet.script.*;
import carpet.script.exception.ExpressionException;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Lexer {
    public int pos, lineno, linepos;
    public final String input;
    public final Expression expression;
    public final Context context;
    public LanitiumConfig.Structured config;
    public ArrayDeque<TokenTree.StackFrame> stack = new ArrayDeque<>();

    public static final Set<String> OPS = new HashSet<>(Set.of(
        "+", "-", "*", "/", "%", "^",
        "&&", "||", "==", "!=", "<", "<=", ">", ">=",
        "=", "+=", ":", ":=", "::", "->", "&", "|", "..", "..="
    ));

    public static final Set<String> UOPS = new HashSet<>(Set.of(
        "!", "-", "+", ":", "..."
    ));

    public static final boolean[] ALLOWED_IN_OPS = new boolean['~' + 1];

    static {
        for (char c : "~!@$%^&*-+=|:<>./?".toCharArray()) ALLOWED_IN_OPS[c] = true;
    }

    public Lexer(int pos, int lineno, int linepos, String input, Expression expression, Context context) {
        this.pos = pos;
        this.lineno = lineno;
        this.linepos = linepos;
        this.input = input;
        this.expression = expression;
        this.context = context;
    }

    public void lex() {
        skipWhitespace();
        Token token = TokenInterface.at(this);
        String ident = parseIdent();
        if (!"lanitium".equals(ident))
            throw new ExpressionException(context, expression, TokenInterface.morph(token, TokenTypeInterface.VARIABLE, ident), "Expected 'lanitium' after '@'");

        skipWhitespace();
        LanitiumConfig cfg = parseConfig(TokenInterface.relocate(token, this));
        if (!(cfg instanceof LanitiumConfig.Map(Map<LanitiumConfig, LanitiumConfig> cfgEntries)))
            throw new ExpressionException(context, expression, "Top-level '@lanitium' config must be a map");

        LanitiumConfig.Version version = switch (cfgEntries.get(LanitiumConfig.VERSION)) {
            case LanitiumConfig.Int _ -> throw new ExpressionException(context, expression, "No stable version exists yet, use 'nightly'");
            case LanitiumConfig.Str(String name) -> switch (name) {
                case "nightly" -> LanitiumConfig.Version.Nightly.NIGHTLY;
                default -> throw new ExpressionException(context, expression, "Invalid version name, expected 'nightly'");
            };
            case null -> throw new ExpressionException(context, expression, "Key 'version' must be present; no stable version exists yet, use 'nightly'");
            default -> throw new ExpressionException(context, expression, "Version must be either an int or a string");
        };
        assert version == LanitiumConfig.Version.Nightly.NIGHTLY;

        config = new LanitiumConfig.Structured.Nightly();

        stack.add(new TokenTree.StackFrame(null, Token.NONE, new ArrayList<>()));
        lexPart();
    }

    private char peek() {
        return pos < input.length() ? input.charAt(pos) : 0;
    }

    private char peek(int dist) {
        return pos + dist < input.length() ? input.charAt(pos + dist) : 0;
    }

    private boolean skipWhitespace() {
        boolean skipped = false;
        for (char c;;) {
            while (Character.isWhitespace(c = peek())) {
                skipped = true;
                if (c == '\n') {
                    lineno++;
                    linepos = 0;
                } else linepos++;
                pos++;
            }

            if (c == '/') switch (peek(1)) {
                case '/' -> {
                    skipped = true;
                    linepos += 2;
                    pos += 2;
                    while (pos < input.length() && (c = peek()) != '\n') {
                        linepos++;
                        pos++;
                    }
                    continue;
                }
                case '*' -> {
                    Token sacrifice = TokenInterface.at(this);
                    skipped = true;
                    linepos += 2;
                    pos += 2;
                    for (;;) {
                        if (pos >= input.length())
                            throw new ExpressionException(context, expression, sacrifice, "Unterminated block comment");

                        c = peek();
                        if (c == '*' && peek(1) == '/') {
                            linepos += 2;
                            pos += 2;
                            break;
                        } else if (c == '\n') {
                            lineno++;
                            linepos = 0;
                        } else linepos++;
                        pos++;
                    }
                    continue;
                }
            }

            return skipped;
        }
    }

    private void addToken(Token token) {
        addTree(new TokenTree.Single(token));
    }

    private void addTree(TokenTree tree) {
        stack.getFirst().contents().add(tree);
    }

    private void push(TokenTree.BracketType type, Token open) {
        stack.push(new TokenTree.StackFrame(type, open, new ArrayList<>()));
    }

    private @Nullable TokenTree lastTree() {
        List<TokenTree> trees = stack.getFirst().contents();
        return !trees.isEmpty() ? trees.getFirst() : null;
    }

    private String parseIdent() {
        char c = peek();
        if (c != '_' && !Character.isLetter(c)) return "";
        int start = pos;
        do {
            linepos++;
            pos++;
        } while ((c = peek()) == '_' || Character.isLetterOrDigit(c));
        return input.substring(start, pos);
    }

    private LanitiumConfig parseConfig(Token sacrifice) {
        char c = peek();
        switch (c) {
            case '\'' -> {
                StringBuilder builder = new StringBuilder();
                linepos++;
                pos++;
                int from = pos;
                while (pos < input.length()) {
                    c = peek();
                    if (c == '\'') {
                        if (peek(1) == '\'') {
                            builder.append(input.substring(from, pos + 1));
                            linepos += 2;
                            from = pos += 2;
                            continue;
                        }

                        builder.append(input.substring(from, pos));
                        linepos++;
                        pos++;
                        return new LanitiumConfig.Str(builder.toString());
                    }

                    if (Character.isISOControl(c)) {
                        sacrifice.pos = pos;
                        sacrifice.linepos = linepos;
                        throw new ExpressionException(context, expression, sacrifice, "Control characters are not allowed in '@lanitium' strings");
                    }
                    linepos++;
                    pos++;
                }

                throw new ExpressionException(context, expression, sacrifice, "Unterminated string");
            }
            case '[' -> {
                linepos++;
                pos++;
                List<LanitiumConfig> values = new ArrayList<>();
                for (;;) {
                    skipWhitespace();
                    if (peek() == ']') {
                        linepos++;
                        pos++;
                        return new LanitiumConfig.List(List.copyOf(values));
                    }

                    values.add(parseConfig(TokenInterface.at(this)));
                    skipWhitespace();

                    switch (peek()) {
                        case ',' -> {
                            linepos++;
                            pos++;
                        }
                        case ']' -> {
                            linepos++;
                            pos++;
                            return new LanitiumConfig.List(List.copyOf(values));
                        }
                        default -> throw new ExpressionException(context, expression, TokenInterface.relocate(sacrifice, this), "Expected either ',' or ']'");
                    }
                }
            }
            case '{' -> {
                linepos++;
                pos++;
                Map<LanitiumConfig, LanitiumConfig> entries = new HashMap<>();
                for (;;) {
                    skipWhitespace();
                    if (peek() == '}') {
                        linepos++;
                        pos++;
                        return new LanitiumConfig.Map(Map.copyOf(entries));
                    }

                    Token beforeKey = TokenInterface.at(this);
                    LanitiumConfig key = parseConfig(beforeKey);
                    if (entries.containsKey(key))
                        throw new ExpressionException(context, expression, beforeKey, "Duplicate key");
                    skipWhitespace();

                    if (!(peek() == '-' && peek(1) == '>'))
                        throw new ExpressionException(context, expression, TokenInterface.relocate(sacrifice, this), "Expected '->'");
                    linepos += 2;
                    pos += 2;
                    skipWhitespace();

                    entries.put(key, parseConfig(TokenInterface.at(this)));
                    skipWhitespace();

                    switch (peek()) {
                        case ',' -> {
                            linepos++;
                            pos++;
                        }
                        case '}' -> {
                            linepos++;
                            pos++;
                            return new LanitiumConfig.Map(Map.copyOf(entries));
                        }
                        default -> throw new ExpressionException(context, expression, TokenInterface.relocate(sacrifice, this), "Expected either ',' or '}'");
                    }
                }
            }
        }

        if (c == '-' || c == '+' || c >= '0' && c <= '9') {
            int start = pos;
            do {
                linepos++;
                pos++;
            } while ((c = peek()) >= '0' && c <= '9');
            try {
                return new LanitiumConfig.Int(Long.parseLong(input, start, pos, 10));
            } catch (ArithmeticException e) {
                throw new ExpressionException(context, expression, sacrifice, "Invalid int: " + e.getMessage());
            }
        }

        return switch (parseIdent()) {
            case "false" -> LanitiumConfig.Bool.FALSE;
            case "true" -> LanitiumConfig.Bool.TRUE;
            case String ident -> throw new ExpressionException(context, expression, TokenInterface.morph(sacrifice, TokenTypeInterface.VARIABLE, ident), "Expected either 'false', 'true', an integer, a literal string, '[', or '{'");
        };
    }

    private static boolean isHex(char c) {
        return c <= 'f' && HEX[c] >= 0;
    }

    private static final int[] HEX = {
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
         0,  1,  2,  3,  4,  5,  6,  7,  8,  9, -1, -1, -1, -1, -1, -1,
        -1, 10, 11, 12, 13, 14, 15, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, 10, 11, 12, 13, 14, 15,
    };

    private enum EscapeCodeType { NORMAL, INTERPOLATION, NEWLINE }

    private EscapeCodeType escapeCode(Token sacrifice, StringBuilder builder) {
        char c = peek();
        switch (c) {
            case '(' -> { return EscapeCodeType.INTERPOLATION; }
            case '\\', '\'', '"' -> builder.append(c);
            case '\n' -> { return EscapeCodeType.NEWLINE; }
            case 'n' -> builder.append('\n');
            case 't' -> builder.append('\t');
            case 'r' -> builder.append('\r');
            case 'f' -> builder.append('\f');
            case 'b' -> builder.append('\b');
            case 's' -> builder.append(' ');
            case 'x' -> {
                linepos++;
                pos++;
                if (!isHex(peek()) || !isHex(peek(1)))
                    throw new ExpressionException(context, expression, TokenInterface.relocate(sacrifice, this), "Expected 2 hexadecimal digits after '\\x'");
                builder.append((char)(HEX[peek()] << 4 | HEX[peek()]));
                linepos += 2;
                pos += 2;
                return EscapeCodeType.NORMAL;
            }
            case 'u' -> {
                linepos++;
                pos++;
                if (peek() != '{') {
                    if (!isHex(peek()) || !isHex(peek(1)) || !isHex(peek(2)) || !isHex(peek(3)))
                        throw new ExpressionException(context, expression, TokenInterface.relocate(sacrifice, this), "Expected either 4 hexadecimal digits or '{' after '\\u'");
                    builder.append((char)(HEX[peek()] << 12 | HEX[peek(1)] << 8 | HEX[peek(2)] << 4 | HEX[peek(3)]));
                    linepos += 4;
                    pos += 4;
                    return EscapeCodeType.NORMAL;
                }

                linepos++;
                pos++;
                TokenInterface.relocate(sacrifice, this);
                int ch = 0;
                boolean pointless = false;
                for (;;) {
                    if (!isHex(c = peek())) {
                        if (c == '}') break;
                        throw new ExpressionException(context, expression, TokenInterface.relocate(sacrifice, this), "Expected '}");
                    }

                    ch = ch << 4 | HEX[c];
                    if (ch > 0x10FFFF) pointless = true;
                    linepos++;
                    pos++;
                }

                if (pointless || ch >= 0xD800 && ch <= 0xDFFF)
                    throw new ExpressionException(context, expression, sacrifice, "Invalid codepoint");

                builder.append(ch);
                linepos++;
                pos++;
                return EscapeCodeType.NORMAL;
            }
            default -> throw new ExpressionException(context, expression, sacrifice, "Unknown escape sequence");
        }
        linepos++;
        pos++;
        return EscapeCodeType.NORMAL;
    }

    private void checkAfterOp(Token sacrifice) {
        if (lastTree() == null || lastTree() instanceof TokenTree.Single(Token lastToken) && (
            TokenInterface.type(lastToken) == TokenTypeInterface.UNARY_OPERATOR
         || TokenInterface.type(lastToken) == TokenTypeInterface.OPERATOR
         || TokenInterface.type(lastToken) == TokenTypeInterface.COMMA))
            throw new ExpressionException(context, expression, sacrifice, "Expected a value before an operator");
    }

    private void checkDot(Token sacrifice) {
        if (lastTree() instanceof TokenTree.Single(Token lastToken) && ".".equals(lastToken.surface))
            throw new ExpressionException(context, expression, sacrifice, "Expected an identifier after '.'");
    }

    private boolean prevValue() {
        return lastTree() instanceof TokenTree.Brackets || lastTree() instanceof TokenTree.Single(Token lastToken) && !(
            TokenInterface.type(lastToken) == TokenTypeInterface.OPERATOR
         || TokenInterface.type(lastToken) == TokenTypeInterface.UNARY_OPERATOR
         || TokenInterface.type(lastToken) == TokenTypeInterface.COMMA
         || TokenInterface.type(lastToken) == TokenTypeInterface.MARKER
        );
    }

    private void lexPart() {
        int minDepth = stack.size();
        for (;;) {
            skipWhitespace();
            if (pos >= input.length()) {
                if (stack.size() > 1)
                    throw new ExpressionException(context, expression, stack.pop().open(), "Mismatched brackets");
                return;
            }

            Token token = TokenInterface.at(this);
            char c = peek();
            switch (c) {
                case '(' -> {
                    push(TokenTree.BracketType.ROUND, TokenInterface.morph(token, TokenTypeInterface.OPEN_PAREN, "("));
                    linepos++;
                    pos++;
                    checkDot(token);
                    addToken(token);
                    continue;
                }
                case '[' -> {
                    push(TokenTree.BracketType.SQUARE, TokenInterface.morph(token, TokenTypeInterface.OPEN_PAREN, "["));
                    linepos++;
                    pos++;
                    checkDot(token);
                    addToken(token);
                    continue;
                }
                case '{' -> {
                    push(TokenTree.BracketType.CURLY, TokenInterface.morph(token, TokenTypeInterface.OPEN_PAREN, "{"));
                    linepos++;
                    pos++;
                    checkDot(token);
                    addToken(token);
                    continue;
                }
                case ',' -> {
                    TokenInterface.morph(token, TokenTypeInterface.COMMA, ",");
                    linepos++;
                    pos++;
                    checkAfterOp(token);
                    addToken(token);
                    continue;
                }
                case ';' -> {
                    TokenInterface.morph(token, TokenTypeInterface.MARKER, ";");
                    linepos++;
                    pos++;
                    if (lastTree() != null) checkAfterOp(token);
                    addToken(token);
                    continue;
                }
                case ')', ']', '}' -> {
                    TokenInterface.morph(token, TokenTypeInterface.CLOSE_PAREN, "" + c);
                    TokenTree.StackFrame top = stack.pop();
                    if (switch (c) {
                        case ')' -> top.type() != TokenTree.BracketType.ROUND && top.type() != TokenTree.BracketType.INTERPOLATION;
                        case ']' -> top.type() != TokenTree.BracketType.SQUARE;
                        case '}' -> top.type() != TokenTree.BracketType.CURLY;
                        default -> throw new AssertionError();
                    }) throw new ExpressionException(context, expression, token, top.type() == null ? "Unexpected closing bracket" : "Mismatched brackets");
                    linepos++;
                    pos++;
                    checkAfterOp(token);
                    addTree(new TokenTree.Brackets(top.type(), top.open(), List.copyOf(top.contents()), token));
                    if (stack.size() < minDepth) return;
                    continue;
                }
                case '\'', '#' -> {
                    TokenInterface.setType(token, TokenTypeInterface.STRINGPARAM);
                    if (prevValue() && !(lastTree() instanceof TokenTree.Single(Token lastToken) && TokenInterface.type(lastToken) == TokenTypeInterface.VARIABLE))
                        throw new ExpressionException(context, expression, token, "Unexpected token");
                    checkDot(token);

                    int hashes = 0;
                    while (c == '#') {
                        hashes++;
                        linepos++;
                        pos++;
                        c = peek();
                    }

                    if (c != '\'')
                        throw new ExpressionException(context, expression, TokenInterface.relocate(token, this), "Expected `'` after hashes");
                    linepos++;
                    pos++;
                    Token sacrifice = new Token();

                    if (hashes == 0 && peek() == '\'' && peek(1) == '\'') {
                        linepos += 2;
                        pos += 2;
                        while ((c = peek()) != '\n' && Character.isWhitespace(c)) {
                            linepos++;
                            pos++;
                        }

                        if (pos >= input.length())
                            throw new ExpressionException(context, expression, token, "Unterminated string");

                        if (c != '\n')
                            throw new ExpressionException(context, expression, TokenInterface.relocate(token, this), "Expected a newline after `'''`");
                        lineno++;
                        linepos = 0;
                        pos++;
                        List<TokenTree.StringLine> lines = new ArrayList<>();

                        for (;;) {
                            int from = pos;
                            Token start = TokenInterface.at(this);
                            while ((c = peek()) != '\n' && Character.isWhitespace(c)) {
                                linepos++;
                                pos++;
                            }

                            if (c == '\'' && peek(1) == '\'' && peek(2) == '\'') {
                                String indent = input.substring(from, pos);
                                boolean addNewline = false;
                                Token lastToken = null;
                                for (TokenTree.StringLine line : lines) {
                                    String dedented = line.start().surface;
                                    if (dedented.startsWith(indent)) dedented = dedented.substring(indent.length());
                                    else if (indent.startsWith(dedented)) dedented = "";
                                    else throw new ExpressionException(context, expression, line.start(), "The indentation for a string line must be consistent with the one for closing `'''`");

                                    if (lastToken != null) {
                                        if (addNewline) lastToken.append('\n');
                                        lastToken.append(dedented);
                                    } else {
                                        line.start().surface = dedented;
                                        lastToken = line.start();
                                    }

                                    for (TokenTree.StringInterpolation interpolation : line.interpolations()) {
                                        addToken(lastToken);
                                        addTree(interpolation.interpolation());
                                        lastToken = interpolation.token();
                                    }
                                    addNewline = !line.noNewline();
                                }

                                if (lastToken == null) {
                                    token.surface = "";
                                    lastToken = token;
                                }
                                addToken(lastToken);
                                linepos += 3;
                                pos += 3;
                                break;
                            }

                            TokenInterface.setType(start, TokenTypeInterface.STRINGPARAM);
                            Token after = start;
                            StringBuilder builder = new StringBuilder();
                            List<TokenTree.StringInterpolation> interpolations = new ArrayList<>();
                            boolean noNewline = false;
                            line: for (;;) {
                                if (pos >= input.length())
                                    throw new ExpressionException(context, expression, token, "Unterminated string");

                                switch (c = peek()) {
                                    case '\\' -> {
                                        builder.append(input, from, pos);
                                        linepos++;
                                        pos++;
                                        switch (escapeCode(TokenInterface.relocate(sacrifice, this), builder)) {
                                            case NORMAL -> {}
                                            case NEWLINE -> noNewline = true;
                                            case INTERPOLATION -> {
                                                push(TokenTree.BracketType.INTERPOLATION, TokenInterface.morph(TokenInterface.at(this), TokenTypeInterface.OPEN_PAREN, "\\("));
                                                linepos++;
                                                pos++;
                                                lexPart();

                                                after.surface = builder.toString();
                                                builder.delete(0, builder.length());
                                                TokenTree interpolation = stack.getFirst().contents().removeLast();
                                                after = TokenInterface.at(this);
                                                TokenInterface.setType(after, TokenTypeInterface.STRINGPARAM);
                                                interpolations.add(new TokenTree.StringInterpolation(interpolation, after));
                                            }
                                        }
                                        from = pos;
                                    }
                                    case '\'' -> {
                                        if (peek(1) == '\'' && peek(2) == '\'')
                                            throw new ExpressionException(context, expression, TokenInterface.relocate(sacrifice, this), "Closing `'''` must be on a separate line");
                                        linepos++;
                                        pos++;
                                    }
                                    case '\n' -> {
                                        after.surface = builder.append(input, from, pos).toString();
                                        lines.add(new TokenTree.StringLine(start, interpolations, noNewline));
                                        lineno++;
                                        linepos = 0;
                                        pos++;
                                        break line;
                                    }
                                    default -> {
                                        linepos++;
                                        pos++;
                                    }
                                }
                            }
                        }
                        continue;
                    }

                    int from = pos;
                    Token current = token;
                    StringBuilder builder = new StringBuilder();

                    str: for (;;) {
                        if (pos >= input.length())
                            throw new ExpressionException(context, expression, token, "Unterminated string");

                        switch (c = peek()) {
                            case '\\' -> {
                                linepos++;
                                pos++;
                                int count = 0;
                                while (peek() == '#') {
                                    if (count++ >= hashes)
                                        throw new ExpressionException(context, expression, TokenInterface.relocate(token, this), "Too many hashes for an escape sequence");
                                    linepos++;
                                    pos++;
                                }

                                if (count < hashes) break;
                                builder.append(input, from, pos - hashes - 1);
                                switch (escapeCode(sacrifice, builder)) {
                                    case NORMAL -> {}
                                    case NEWLINE -> {}
                                    case INTERPOLATION -> {
                                        current.surface = builder.toString();
                                        builder.delete(0, builder.length());
                                        addToken(current);

                                        push(TokenTree.BracketType.INTERPOLATION, TokenInterface.morph(TokenInterface.at(this), TokenTypeInterface.OPEN_PAREN, "\\("));
                                        linepos++;
                                        pos++;
                                        lexPart();

                                        current = TokenInterface.at(this);
                                        TokenInterface.setType(current, TokenTypeInterface.STRINGPARAM);
                                    }
                                }
                                from = pos;
                            }
                            case '\'' -> {
                                linepos++;
                                pos++;
                                int count = 0;
                                while (count < hashes && peek() == '#') {
                                    linepos++;
                                    pos++;
                                }

                                if (count < hashes) break;
                                current.surface = builder.append(input, from, pos - hashes - 1).toString();
                                addToken(current);
                                break str;
                            }
                            case '\n' -> throw new ExpressionException(context, expression, TokenInterface.relocate(sacrifice, this), "Newlines are only allowed in multi-line strings");
                            default -> {
                                linepos++;
                                pos++;
                            }
                        }
                    }
                    continue;
                }
                case '"' -> throw new ExpressionException(context, expression, token, "Strings use single quotes");
            }

            if (c == '_' || Character.isLetter(c)) {
                TokenInterface.morph(token, TokenTypeInterface.VARIABLE, parseIdent());
                if (prevValue())
                    throw new ExpressionException(context, expression, token, "Unexpected token");

                addToken(token);
                continue;
            }

            if (c >= '0' && c <= '9' || c == '.' && peek(1) >= '0' && peek(1) <= '9') {
                TokenInterface.setType(token, TokenTypeInterface.LITERAL);
                if (prevValue())
                    throw new ExpressionException(context, expression, token, "Unexpected token");
                checkDot(token);

                if (c == '0') switch (peek(1)) {
                    case 'b', 'B' -> {
                        linepos += 2;
                        pos += 2;
                        while ((c = peek()) == '_') {
                            linepos++;
                            pos++;
                        }

                        if (!(c == '0' || c == '1'))
                            throw new ExpressionException(context, expression, TokenInterface.relocate(token, this), "Expected binary digits");

                        StringBuilder builder = new StringBuilder("0b");
                        while (c == '_' || c == '0' || c == '1') {
                            if (c != '_') builder.append(c);
                            linepos++;
                            pos++;
                            c = peek();
                        }

                        token.surface = builder.toString();
                        addToken(token);
                        continue;
                    }
                    case 'o', 'O' -> {
                        linepos += 2;
                        pos += 2;
                        while ((c = peek()) == '_') {
                            linepos++;
                            pos++;
                        }

                        if (!(c >= '0' && c <= '7'))
                            throw new ExpressionException(context, expression, TokenInterface.relocate(token, this), "Expected binary digits");

                        StringBuilder builder = new StringBuilder("0o");
                        while (c == '_' || c >= '0' && c <= '7') {
                            if (c != '_') builder.append(c);
                            linepos++;
                            pos++;
                            c = peek();
                        }

                        token.surface = builder.toString();
                        addToken(token);
                        continue;
                    }
                    case 'x', 'X' -> {
                        linepos += 2;
                        pos += 2;
                        while ((c = peek()) == '_') {
                            linepos++;
                            pos++;
                        }

                        if (!isHex(c))
                            throw new ExpressionException(context, expression, TokenInterface.relocate(token, this), "Expected binary digits");

                        StringBuilder builder = new StringBuilder("0x");
                        while (c == '_' || isHex(c)) {
                            if (c != '_') builder.append(c);
                            linepos++;
                            pos++;
                            c = peek();
                        }

                        token.surface = builder.toString();
                        addToken(token);
                        continue;
                    }
                }

                boolean firstDot = c == '.';
                StringBuilder builder = new StringBuilder();
                if (!firstDot) while (c == '_' || c >= '0' && c <= '9') {
                    if (c != '_') builder.append(c);
                    linepos++;
                    pos++;
                    c = peek();
                }

                if (c == '.') {
                    linepos++;
                    int from = ++pos;
                    while ((c = peek()) == '_') {
                        linepos++;
                        pos++;
                    }

                    if ((firstDot || pos > from) && !(c >= '0' && c <= '9'))
                        throw new ExpressionException(context, expression, TokenInterface.relocate(token, this), "Expected decimal digits");

                    builder.append('.');
                    while (c == '_' || c >= '0' && c <= '9') {
                        if (c != '_') builder.append(c);
                        linepos++;
                        pos++;
                        c = peek();
                    }
                }

                if (c == 'e' || c == 'E') {
                    linepos++;
                    pos++;
                    builder.append('E');
                    if ((c = peek()) == '-' || c == '+') {
                        builder.append(c);
                        linepos++;
                        pos++;
                        c = peek();
                    }

                    int from = pos;
                    while (c == '_') {
                        linepos++;
                        pos++;
                        c = peek();
                    }

                    if (!(c >= '0' && c <= '9'))
                        throw new ExpressionException(context, expression, TokenInterface.relocate(token, this), "Expected decimal digits");

                    while (c == '_' || c >= '0' && c <= '9') {
                        if (c != '_') builder.append(c);
                        linepos++;
                        pos++;
                        c = peek();
                    }
                }

                token.surface = builder.toString();
                addToken(token);
                continue;
            }

            if (c == '.' && peek(1) != '.') {
                TokenInterface.morph(token, prevValue() ? TokenTypeInterface.OPERATOR : TokenTypeInterface.UNARY_OPERATOR, ".");
                linepos++;
                pos++;
                addToken(token);
                continue;
            }

            if (c > '~' || !ALLOWED_IN_OPS[c])
                throw new ExpressionException(context, expression, token, "Unexpected character");

            Set<String> ops = prevValue() ? OPS : UOPS;
            TokenInterface.setType(token, ops == OPS ? TokenTypeInterface.OPERATOR : TokenTypeInterface.UNARY_OPERATOR);
            StringBuilder builder = new StringBuilder();
            int lastPos = pos, lastLinepos = linepos;

            do {
                linepos++;
                pos++;
                builder.append(c);
                String match = builder.toString();
                if (ops.contains(match)) {
                    token.surface = match;
                    lastLinepos = linepos;
                    lastPos = pos;
                }
            } while ((c = peek()) <= '~' && ALLOWED_IN_OPS[c]);

            linepos = lastLinepos;
            pos = lastPos;
            if (token.surface == null)
                throw new ExpressionException(context, expression, token, "Unknown operator");
            addToken(token);
        }
    }
}

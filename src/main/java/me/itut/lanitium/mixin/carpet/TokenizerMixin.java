package me.itut.lanitium.mixin.carpet;

import carpet.script.Context;
import carpet.script.Expression;
import carpet.script.Token;
import carpet.script.Tokenizer;
import carpet.script.exception.ExpressionException;
import me.itut.lanitium.internal.carpet.InterpolatedString;
import me.itut.lanitium.internal.carpet.TokenInterface;
import me.itut.lanitium.internal.carpet.TokenTypeInterface;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

@Mixin(value = Tokenizer.class, remap = false)
public abstract class TokenizerMixin {
    @Shadow private int pos;
    @Shadow @Final private String input;
    @Shadow private Token previousToken;
    @Shadow private int linepos;
    @Shadow private int lineno;
    @Shadow protected abstract char peekNextChar();
    @Shadow @Final private Expression expression;
    @Shadow @Final private Context context;
    @Shadow @Final private boolean comments;
    @Shadow @Final private boolean newLinesMarkers;
    @Shadow public abstract Token next();

    @Unique private static final int STATE_NORMAL = 0;
    @Unique private static final int STATE_STRING_INTERPOLATION = 1;

    @Unique private int state;
    @Unique private int brackets;
    @Unique private final Stack<Token> tokenQueue = new Stack<>();

    @Shadow
    private static boolean isSemicolon(Token tok) {
        return false;
    }

    @Inject(method = "hasNext", at = @At("HEAD"), cancellable = true)
    private void hasNext(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(pos < input.length() || !tokenQueue.isEmpty());
    }

    @Unique
    private boolean escapeCode(StringBuilder builder) {
        char nextChar = peekNextChar();
        S: switch (nextChar) {
            case '(' -> {
                pos++;
                linepos++;
                return true;
            }
            case 'n' -> builder.append('\n');
            case 't' -> builder.append('\t');
            case 'r' -> builder.append('\r');
            case 'f' -> builder.append('\f');
            case 'b' -> builder.append('\b');
            case 's' -> builder.append(' ');
            default -> {
                switch (nextChar) {
                    case 'x' -> {
                        if (pos + 3 < input.length() && input.charAt(pos + 2) != '+') try {
                            builder.append((char)Integer.parseUnsignedInt(input, pos + 2, pos + 4, 16));
                            pos += 2;
                            linepos += 2;
                            break S;
                        } catch (NumberFormatException ignored) {}
                    }
                    case 'u' -> {
                        if (pos + 5 < input.length() && input.charAt(pos + 2) != '+') try {
                            builder.append((char)Integer.parseUnsignedInt(input, pos + 2, pos + 6, 16));
                            pos += 4;
                            linepos += 4;
                            break S;
                        } catch (NumberFormatException ignored) {}
                    }
                    case 'U' -> {
                        if (pos + 9 < input.length() && input.charAt(pos + 2) != '+') try {
                            builder.append(Character.toString(Integer.parseUnsignedInt(input, pos + 2, pos + 10, 16)));
                            pos += 8;
                            linepos += 8;
                            break S;
                        } catch (NumberFormatException ignored) {}
                    }
                    case 'N' -> {
                        N: if (pos + 4 < input.length() && input.charAt(pos + 2) == '{') try {
                            int start = pos + 3, end;
                            for (int i = start; ; i++) {
                                if (i == input.length()) break N;
                                char c = input.charAt(i);
                                if (c == '}') {
                                    end = i;
                                    break;
                                }
                                if (c != ' ' && c != '_' && c != '-' && !Character.isDigit(c) && !Character.isLetter(c))
                                    break N;
                            }
                            builder.append(Character.toString(Character.codePointOf(input.substring(start, end))));
                            linepos += end - pos;
                            pos = end;
                            return false;
                        } catch (IllegalArgumentException ignored) {}
                    }
                }
                builder.append(nextChar);
            }
        }
        pos++;
        linepos++;
        return false;
    }

    @Inject(method = "next()Lcarpet/script/Token;", at = @At("HEAD"), cancellable = true)
    private void customSyntax(CallbackInfoReturnable<Token> cir) {
        if (!tokenQueue.isEmpty()) {
            cir.setReturnValue(tokenQueue.pop());
            return;
        }

        if (pos >= input.length()) {
            cir.setReturnValue(previousToken = null);
            return;
        }
        char ch = input.charAt(pos);
        while (Character.isWhitespace(ch) && pos < input.length()) {
            linepos++;
            if (ch == '\n') {
                lineno++;
                linepos = 0;
            }
            ch = input.charAt(++pos);
        }

        Token token = new Token();
        token.pos = pos;
        token.lineno = lineno;
        token.linepos = linepos;

        if (Character.isDigit(ch) || (ch == '.' && Character.isDigit(peekNextChar()))) {
            boolean hex = false, bin = false, dec = false;
            if (ch == '0')
                if (peekNextChar() == 'x' || peekNextChar() == 'X') hex = true;
                else if (peekNextChar() == 'b' || peekNextChar() == 'B') bin = true;
                else dec = true;
            else dec = true;

            while (ch == '_' || Character.isDigit(ch)
                || hex && (ch >= 'a' && ch <= 'f' || ch >= 'A' && ch <= 'F' || ch == 'x' || ch == 'X')
                || bin && (ch == 'b' || ch == 'B')
                || dec && (ch == '.' || ch == 'e' || ch == 'E' ||
                    (ch == '-' || ch == '+') && token.length() > 0 &&
                    ('e' == token.charAt(token.length() - 1) || 'E' == token.charAt(token.length() - 1))
                )
            ) {
                if (input.charAt(pos++) != '_') token.append(input.charAt(pos - 1));
                linepos++;
                ch = pos == input.length() ? 0 : input.charAt(pos);
            }
            if (bin) try {
                token.surface = new BigInteger(token.surface.substring(2), 2).toString();
            } catch (NumberFormatException ignored) {}

            TokenInterface.setType(token, hex ? TokenTypeInterface.HEX_LITERAL : TokenTypeInterface.LITERAL);
        } else if (ch == '\'') parseString(token);
        else if (ch == '#') {
            int count = 0;
            while (ch == '#') {
                count++;
                if (++pos == input.length())
                    throw new ExpressionException(context, expression, token, "Program truncated");

                linepos++;
                ch = input.charAt(pos);
            }

            if (ch != '\'')
                throw new ExpressionException(context, expression, token, "Only a string can be raw");
            int start = pos + 1;

            s: while (true) {
                if (++pos == input.length())
                    throw new ExpressionException(context, expression, token, "Program truncated");

                ch = input.charAt(pos);
                if (ch == '\n') {
                    lineno++;
                    linepos = 0;
                } else linepos++;
                if (ch == '\'') {
                    if (pos + count >= input.length())
                        throw new ExpressionException(context, expression, token, "Program truncated");

                    for (int i = 1; i <= count; i++)
                        if (input.charAt(pos + i) != '#')
                            continue s;
                    break;
                }
            }
            token.surface = input.substring(start, pos);
            pos += count + 1;
            linepos += count + 1;
        } else if (Character.isLetter(ch) || ch == '_') {
            while ((Character.isLetter(ch) || Character.isDigit(ch) || ch == '_') && pos < input.length()) {
                token.append(input.charAt(pos++));
                linepos++;
                ch = pos == input.length() ? 0 : input.charAt(pos);
            }
            // Remove optional white spaces after function or variable name
            if (Character.isWhitespace(ch)) {
                while (Character.isWhitespace(ch) && pos < input.length()) {
                    ch = input.charAt(pos++);
                    linepos++;
                    if (ch == '\n') {
                        lineno++;
                        linepos = 0;
                    }
                }
                pos--;
                linepos--;
            }
            TokenInterface.setType(token, ch == '(' || ch == '{' || ch == '\'' || ch == '#' ? TokenTypeInterface.FUNCTION : TokenTypeInterface.VARIABLE);
        } else if (ch == '(' || ch == ')' || ch == ',' || ch == '{' || ch == '}' || ch == '[' || ch == ']') {
            if (state == STATE_STRING_INTERPOLATION) switch (ch) {
                case '(', '[', '{' -> brackets++;
                case ')', ']', '}' -> {
                    if (--brackets < 0) {
                        cir.setReturnValue(null);
                        return;
                    }
                }
            }

            TokenInterface.setType(token, switch (ch) {
                case '(' -> TokenTypeInterface.OPEN_PAREN;
                case ')' -> TokenTypeInterface.CLOSE_PAREN;
                case ',' -> {
                    token.disguiseAs(", ", null);
                    yield TokenTypeInterface.COMMA;
                }
                default -> TokenTypeInterface.MARKER;
            });
            token.append(ch);
            pos++;
            linepos++;

            if (expression != null && context != null && previousToken != null && (
                ((TokenInterface)previousToken).lanitium$type() == TokenTypeInterface.OPERATOR ||
                ((TokenInterface)previousToken).lanitium$type() == TokenTypeInterface.UNARY_OPERATOR)
             && (ch == ')' || ch == ',' || ch == ']' || ch == '}')
             && !previousToken.surface.equals(";"))
                throw new ExpressionException(context, expression, previousToken, "Can't have operator " + previousToken.surface + " at the end of a subexpression");
        } else if (ch == '.' && peekNextChar() != '.') {
            token.surface = ".";
            TokenInterface.setType(token, TokenTypeInterface.OPERATOR);

            TokenTypeInterface prevType = previousToken != null ? ((TokenInterface)previousToken).lanitium$type() : null;
            if (prevType == null
             || prevType == TokenTypeInterface.OPERATOR
             || prevType == TokenTypeInterface.UNARY_OPERATOR
             || prevType == TokenTypeInterface.OPEN_PAREN
             || prevType == TokenTypeInterface.COMMA
             || prevType == TokenTypeInterface.MARKER && (previousToken.surface.equals("{") || previousToken.surface.equals("[")))
                throw new ExpressionException(context, expression, token, "Member access must come after a value");

            pos++;
            linepos++;
        } else {
            StringBuilder greedyMatch = new StringBuilder();
            int initialPos = pos;
            int initialLinePos = linepos;
            int validOperatorSeenUntil = -1;
            while (!Character.isLetter(ch) && !Character.isDigit(ch) && ch != '_'
                && !Character.isWhitespace(ch) && ch != ','
                && ch != '(' && ch != ')' && ch != '[' && ch != ']' && ch != '{' && ch != '}'
                && pos < input.length()) {
                greedyMatch.append(ch);
                if (comments && "/*".contentEquals(greedyMatch)) {
                    while (pos < input.length()) {
                        ch = input.charAt(pos++);
                        if (ch == '*' && pos < input.length() && input.charAt(pos) == '/') {
                            pos++;
                            linepos += 2;
                            break;
                        } else if (ch == '\n') {
                            lineno++;
                            linepos = 0;
                            continue;
                        }
                        linepos++;
                    }
                    token.append(input.substring(initialPos, pos));
                    TokenInterface.setType(token, TokenTypeInterface.MARKER);
                    cir.setReturnValue(token);
                    return;
                } else if (comments && "//".contentEquals(greedyMatch)) {
                    while (ch != '\n' && pos < input.length()) {
                        ch = input.charAt(pos++);
                        linepos++;
                    }
                    if (ch == '\n') {
                        lineno++;
                        linepos = 0;
                    }
                    token.append(input.substring(initialPos, pos));
                    TokenInterface.setType(token, TokenTypeInterface.MARKER);
                    cir.setReturnValue(token); // skipping setting previous
                    return;
                }
                pos++;
                linepos++;
                if ((expression != null ? expression : Expression.none).isAnOperator(greedyMatch.toString()))
                    validOperatorSeenUntil = pos;
                ch = pos == input.length() ? 0 : input.charAt(pos);
            }
            if (newLinesMarkers && "$".contentEquals(greedyMatch)) {
                lineno++;
                linepos = 0;
                TokenInterface.setType(token, TokenTypeInterface.MARKER);
                token.append('$');
                cir.setReturnValue(token); // skipping previous token lookback
                return;
            }
            if (validOperatorSeenUntil != -1) {
                token.append(input.substring(initialPos, validOperatorSeenUntil));
                pos = validOperatorSeenUntil;
                linepos = initialLinePos + validOperatorSeenUntil - initialPos;
            } else {
                token.append(greedyMatch.toString());
            }

            TokenTypeInterface prevType = previousToken != null ? ((TokenInterface)previousToken).lanitium$type() : null;
            if (prevType == null
             || prevType == TokenTypeInterface.OPERATOR
             || prevType == TokenTypeInterface.UNARY_OPERATOR
             || prevType == TokenTypeInterface.OPEN_PAREN
             || prevType == TokenTypeInterface.COMMA
             || (prevType == TokenTypeInterface.MARKER && (previousToken.surface.equals("{") || previousToken.surface.equals("[")))
            ) {
                token.disguiseAs(token.surface, null);
                token.surface += "u";
                TokenInterface.setType(token, TokenTypeInterface.UNARY_OPERATOR);
            } else {
                token.disguiseAs(token.surface.equals(";") ? "; " : ' ' + token.surface + ' ', null);
                TokenInterface.setType(token, TokenTypeInterface.OPERATOR);
            }
        }

        if (expression != null && context != null && previousToken != null) {
            TokenTypeInterface type = ((TokenInterface)token).lanitium$type();
            TokenTypeInterface prevType = ((TokenInterface)previousToken).lanitium$type();

            boolean currentTypeCheck =
                   type == TokenTypeInterface.LITERAL
                || type == TokenTypeInterface.HEX_LITERAL
                || type == TokenTypeInterface.VARIABLE
                || type == TokenTypeInterface.STRINGPARAM
                || type == TokenTypeInterface.FUNCTION
                || type == TokenTypeInterface.OPEN_PAREN
                || type == TokenTypeInterface.MARKER && token.surface.equals("{");

            boolean invalidType = currentTypeCheck && (
                   prevType == TokenTypeInterface.LITERAL
                || prevType == TokenTypeInterface.HEX_LITERAL
                || prevType == TokenTypeInterface.VARIABLE
                || prevType == TokenTypeInterface.STRINGPARAM
                || prevType == TokenTypeInterface.CLOSE_PAREN
                || prevType == TokenTypeInterface.MARKER && (previousToken.surface.equals("}") || previousToken.surface.equals("]"))
            );

            if (invalidType
                || type != TokenTypeInterface.VARIABLE
                && type != TokenTypeInterface.FUNCTION
                && prevType == TokenTypeInterface.OPERATOR
                && previousToken.surface.equals("."))
                throw new ExpressionException(context, expression, previousToken, '\'' + token.surface + "' is not allowed after '" + previousToken.surface + '\'');
        }

        cir.setReturnValue(previousToken = token);
    }

    @Unique
    private void parseString(Token token) {
        pos++;
        linepos++;
        TokenInterface.setType(token, TokenTypeInterface.STRINGPARAM);
        if (pos == input.length())
            throw new ExpressionException(context, expression, token, "Program truncated");

        char ch = input.charAt(pos);
        m: if (ch == '\'' && pos + 1 < input.length() && input.charAt(pos + 1) == '\'') {
            pos += 2;
            linepos += 2;
            if (pos == input.length())
                throw new ExpressionException(context, expression, token, "Program truncated");

            ch = input.charAt(pos);
            w: while (ch != '\n') {
                if (++pos == input.length())
                    throw new ExpressionException(context, expression, token, "Program truncated");

                linepos++;
                ch = input.charAt(pos);
                if (ch == '\'' && pos + 2 < input.length() && input.charAt(pos + 1) == '\'' && input.charAt(pos + 2) == '\'') {
                    pos += 2;
                    linepos += 2;
                    break m;
                } else if (comments && ch == '/' && pos + 1 < input.length()) {
                    switch (input.charAt(pos + 1)) {
                        case '/' -> {
                            pos += 2;
                            if (pos == input.length())
                                throw new ExpressionException(context, expression, token, "Program truncated");
                            ch = input.charAt(pos);
                            while (ch != '\n') {
                                if (++pos == input.length())
                                    throw new ExpressionException(context, expression, token, "Program truncated");
                                ch = input.charAt(pos);
                            }
                            break w;
                        }
                        case '*' -> {
                            if (++pos == input.length())
                                throw new ExpressionException(context, expression, token, "Program truncated");
                            linepos++;
                            while (true) {
                                if (++pos == input.length())
                                    throw new ExpressionException(context, expression, token, "Program truncated");
                                ch = input.charAt(pos);
                                if (ch == '\n') {
                                    lineno++;
                                    linepos = 0;
                                } else if (ch == '*' && pos + 1 < input.length() && input.charAt(pos + 1) == '/') {
                                    pos += 2;
                                    linepos += 2;
                                    break;
                                } else linepos++;
                            }
                            continue;
                        }
                    }
                } else if (Character.isWhitespace(ch)) continue;

                throw new ExpressionException(context, expression, token, "Only comments are allowed on the starting line of multiline strings");
            }

            boolean interpolate = false;
            List<InterpolatedString> lines = new ArrayList<>();
            int indentStart;
            w: while (true) {
                if (++pos == input.length())
                    throw new ExpressionException(context, expression, token, "Program truncated");

                lineno++;
                linepos = 0;
                int start = pos;
                ch = input.charAt(pos);
                while (Character.isWhitespace(ch)) {
                    if (ch == '\n') {
                        lines.add(new InterpolatedString(List.of(), List.of(), new StringBuilder(input.substring(start, pos))));
                        continue w;
                    }
                    if (++pos == input.length())
                        throw new ExpressionException(context, expression, token, "Program truncated");
                    linepos++;
                    ch = input.charAt(pos);
                }

                if (ch == '\'' && pos + 2 < input.length() && input.charAt(pos + 1) == '\'' && input.charAt(pos + 2) == '\'') {
                    indentStart = start;
                    break;
                }

                StringBuilder next = new StringBuilder(input.substring(start, pos));
                InterpolatedString line = new InterpolatedString(new ArrayList<>(), new ArrayList<>(), next);
                lines.add(line);
                while (pos < input.length()) {
                    ch = input.charAt(pos);
                    if (ch == '\\') {
                        if (escapeCode(next)) {
                            interpolate = true;
                            readInterpolation(line);
                        }
                    } else if (ch == '\n') continue w;
                    else next.append(ch);
                    pos++;
                    linepos++;
                }

                throw new ExpressionException(context, expression, token, "Program truncated");
            }

            String indent = input.substring(indentStart, pos);
            for (InterpolatedString line : lines) if (!line.dedent(indent))
                throw new ExpressionException(context, expression, token, "Lines in a multiline string cannot be indented less than the closing triple-quote");

            if (!interpolate)
                token.surface = lines.stream().map(v -> v.next().toString()).collect(Collectors.joining("\n"));
            else {
                int capacity = lines.stream().mapToInt(v -> v.substrings().size()).sum();
                StringBuilder next = new StringBuilder();
                InterpolatedString joined = new InterpolatedString(new ArrayList<>(capacity), new ArrayList<>(capacity), next);

                for (InterpolatedString line : lines) {
                    joined.append(line);
                    next.append('\n');
                }
                next.setLength(next.length() - 1);
                outputInterpolatedString(token, joined);
            }

            pos += 2;
            linepos += 2;
        } else {
            StringBuilder next = new StringBuilder();
            InterpolatedString str = new InterpolatedString(new ArrayList<>(), new ArrayList<>(), next);
            while (ch != '\'') {
                if (ch == '\\') {
                    if (escapeCode(next)) readInterpolation(str);
                    linepos++;
                } else {
                    next.append(ch);
                    if (ch == '\n') {
                        lineno++;
                        linepos = 0;
                    } else linepos++;
                }

                if (++pos == input.length())
                    throw new ExpressionException(context, expression, token, "Program truncated");
                ch = input.charAt(pos);
            }

            if (str.substrings().isEmpty()) token.surface = next.toString();
            else outputInterpolatedString(token, str);
        }
        pos++;
        linepos++;
        token.disguiseAs('\'' + token.surface + '\'', null);
    }

    @Unique
    private void readInterpolation(InterpolatedString str) {
        pos++;
        linepos++;

        int prevState = state;
        int prevBrackets = brackets;
        Token prevToken = previousToken;

        state = STATE_STRING_INTERPOLATION;
        brackets = 0;
        previousToken = null;

        List<Token> interpolation = new ArrayList<>();
        Token interpolated;
        while ((interpolated = next()) != null) interpolation.add(interpolated);

        state = prevState;
        brackets = prevBrackets;
        previousToken = prevToken;

        str.nextInterpolation(interpolation);
    }

    @Unique
    private void outputInterpolatedString(Token token, InterpolatedString str) {
        tokenQueue.push(((TokenInterface)token).lanitium$morphedInto(TokenTypeInterface.MARKER, InterpolatedString.END));
        tokenQueue.push(((TokenInterface)token).lanitium$morphedInto(TokenTypeInterface.STRINGPARAM, str.next().toString()));

        for (int i = str.interpolations().size() - 1; i > 0; i--) {
            tokenQueue.addAll(str.interpolations().get(i).reversed());
            tokenQueue.push(((TokenInterface)token).lanitium$morphedInto(TokenTypeInterface.MARKER, InterpolatedString.NEXT));
            tokenQueue.push(((TokenInterface)token).lanitium$morphedInto(TokenTypeInterface.STRINGPARAM, str.substrings().get(i)));
        }

        tokenQueue.addAll(str.interpolations().getFirst().reversed());
        tokenQueue.push(((TokenInterface)token).lanitium$morphedInto(TokenTypeInterface.MARKER, InterpolatedString.FIRST));
        token.surface = str.substrings().getFirst();
    }

    @Inject(method = "postProcess", at = @At("HEAD"), cancellable = true)
    private static void postProcess(List<Token> originalTokens, CallbackInfoReturnable<List<Token>> cir) {
        List<Token> cleanedTokens = new ArrayList<>();
        Token last = null;
        Stack<Integer> bracketStack = new Stack<>();
        for (int i = originalTokens.size() - 1; i >= 0; i--) {
            Token current = originalTokens.get(i);
            TokenTypeInterface currentType = ((TokenInterface)current).lanitium$type();
            if (currentType == TokenTypeInterface.MARKER && current.surface.charAt(0) == '/')
                continue;
            // skipping comments
            TokenTypeInterface lastType = last != null ? ((TokenInterface)last).lanitium$type() : null;
            if (!isSemicolon(current)
                || (last != null && lastType != TokenTypeInterface.CLOSE_PAREN && lastType != TokenTypeInterface.COMMA && !isSemicolon(last))) {
                if (isSemicolon(current)) {
                    current.surface = ";";
                    ((TokenInterface)current).lanitium$setType(TokenTypeInterface.OPERATOR);
                }

                if (currentType == TokenTypeInterface.OPEN_PAREN) {
                    if (!bracketStack.isEmpty()) bracketStack.pop();
                } else if (currentType == TokenTypeInterface.CLOSE_PAREN) {
                    bracketStack.push(cleanedTokens.size());
                } else if (currentType == TokenTypeInterface.MARKER) switch (current.surface) {
                    case "{" -> {
                        cleanedTokens.add(((TokenInterface)current).lanitium$morphedInto(TokenTypeInterface.OPEN_PAREN, "("));
                        ((TokenInterface)current).lanitium$morph(TokenTypeInterface.FUNCTION, "m");

                        if (bracketStack.isEmpty()) break;
                        int bracket = bracketStack.pop();

                        if (i > 0 && ((TokenInterface)originalTokens.get(i - 1)).lanitium$type() == TokenTypeInterface.FUNCTION) {
                            cleanedTokens.add(bracket + 1, cleanedTokens.get(bracket));
                            cleanedTokens.add(current);
                            cleanedTokens.add(((TokenInterface)current).lanitium$morphedInto(TokenTypeInterface.OPEN_PAREN, "("));
                            last = current;
                            continue;
                        }
                    }
                    case "[" -> {
                        cleanedTokens.add(((TokenInterface)current).lanitium$morphedInto(TokenTypeInterface.OPEN_PAREN, "("));

                        if (!bracketStack.isEmpty()) bracketStack.pop();

                        if (i > 0) {
                            Token prev = originalTokens.get(i - 1);
                            TokenTypeInterface prevType = ((TokenInterface)prev).lanitium$type();
                            if (prevType == TokenTypeInterface.VARIABLE
                             || prevType == TokenTypeInterface.LITERAL
                             || prevType == TokenTypeInterface.HEX_LITERAL
                             || prevType == TokenTypeInterface.STRINGPARAM
                             || prevType == TokenTypeInterface.CLOSE_PAREN
                             || prevType == TokenTypeInterface.MARKER
                             && prev.surface.equals("]") || prev.surface.equals("}")) {
                                ((TokenInterface)current).lanitium$morph(TokenTypeInterface.OPERATOR, ":");
                                break;
                            }
                        }
                        ((TokenInterface)current).lanitium$morph(TokenTypeInterface.FUNCTION, "l");
                    }
                    case "}", "]" -> {
                        ((TokenInterface)current).lanitium$morph(TokenTypeInterface.CLOSE_PAREN, ")");
                        bracketStack.push(cleanedTokens.size());
                    }
                    case InterpolatedString.END, InterpolatedString.NEXT -> {
                        if (current.surface.equals(InterpolatedString.END))
                            ((TokenInterface)current).lanitium$morph(TokenTypeInterface.CLOSE_PAREN, ")");
                        else {
                            ((TokenInterface)current).lanitium$morph(TokenTypeInterface.COMMA, ",");
                            current.disguiseAs(", ", null);
                        }
                        cleanedTokens.add(current);
                        cleanedTokens.add(current = originalTokens.get(--i));
                        current = ((TokenInterface)current).lanitium$morphedInto(TokenTypeInterface.COMMA, ",");
                        current.disguiseAs(", ", null);
                    }
                    case InterpolatedString.FIRST -> {
                        ((TokenInterface)current).lanitium$morph(TokenTypeInterface.COMMA, ",");
                        current.disguiseAs(", ", null);
                        cleanedTokens.add(current);
                        cleanedTokens.add(current = originalTokens.get(--i));
                        current = ((TokenInterface)current).lanitium$morphedInto(TokenTypeInterface.OPEN_PAREN, "(");
                        if (i == 0 || ((TokenInterface)originalTokens.get(i - 1)).lanitium$type() != TokenTypeInterface.FUNCTION) {
                            cleanedTokens.add(current);
                            current = ((TokenInterface)current).lanitium$morphedInto(TokenTypeInterface.FUNCTION, "sum");
                        }
                    }
                } else if (currentType == TokenTypeInterface.STRINGPARAM && i > 0 && ((TokenInterface)originalTokens.get(i - 1)).lanitium$type() == TokenTypeInterface.FUNCTION) {
                    cleanedTokens.add(((TokenInterface)current).lanitium$morphedInto(TokenTypeInterface.CLOSE_PAREN, ")"));
                    cleanedTokens.add(current);
                    cleanedTokens.add(((TokenInterface)current).lanitium$morphedInto(TokenTypeInterface.OPEN_PAREN, "("));
                    last = current;
                    continue;
                } else if (i > 0 && ((TokenInterface)originalTokens.get(i - 1)).lanitium$type() == TokenTypeInterface.OPERATOR && originalTokens.get(i - 1).surface.equals(".")) {
                    if (currentType == TokenTypeInterface.VARIABLE) {
                        originalTokens.get(i - 1).surface = ":";
                        TokenInterface.setType(current, TokenTypeInterface.STRINGPARAM);
                    } else current.surface = '.' + current.surface;
                }
                cleanedTokens.add(current);
            }
            if (currentType != TokenTypeInterface.MARKER || !current.surface.equals("$"))
                last = current;
        }
        Collections.reverse(cleanedTokens);
        cir.setReturnValue(cleanedTokens);
    }
}

package me.itut.lanitium.mixin.carpet;

import carpet.script.Context;
import carpet.script.Expression;
import carpet.script.Tokenizer;
import carpet.script.exception.ExpressionException;
import com.mojang.brigadier.context.StringRange;
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
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Mixin(value = Tokenizer.class, remap = false)
public abstract class TokenizerMixin {
    @Shadow private int pos;
    @Shadow @Final private String input;
    @Shadow private Tokenizer.Token previousToken;
    @Shadow private int linepos;
    @Shadow private int lineno;
    @Shadow protected abstract char peekNextChar();
    @Shadow @Final private Expression expression;
    @Shadow @Final private Context context;
    @Shadow @Final private boolean comments;
    @Shadow @Final private boolean newLinesMarkers;

    @Shadow
    private static boolean isSemicolon(Tokenizer.Token tok) {
        return false;
    }

    @Unique
    private void escapeCode(Tokenizer.Token token) {
        char nextChar = peekNextChar();
        switch (nextChar) {
            case 'n' -> token.append('\n');
            case 't' -> token.append('\t');
            case 'r' -> token.append('\r');
            case 'f' -> token.append('\f');
            case 'b' -> token.append('\b');
            case 's' -> token.append(' ');
            default -> {
                if (nextChar == 'x' && pos + 3 < input.length() && input.charAt(pos + 2) != '+') try {
                    token.append((char)Integer.parseUnsignedInt(input, pos + 2, pos + 4, 16));
                    pos += 2;
                    linepos += 2;
                    break;
                } catch (NumberFormatException ignored) {}
                else if (nextChar == 'u' && pos + 5 < input.length() && input.charAt(pos + 2) != '+') try {
                    token.append((char)Integer.parseUnsignedInt(input, pos + 2, pos + 6, 16));
                    pos += 4;
                    linepos += 4;
                    break;
                } catch (NumberFormatException ignored) {}
                else if (nextChar == 'U' && pos + 9 < input.length() && input.charAt(pos + 2) != '+') try {
                    token.append(Character.toString(Integer.parseUnsignedInt(input, pos + 2, pos + 10, 16)));
                    pos += 8;
                    linepos += 8;
                    break;
                } catch (NumberFormatException ignored) {}
                else N: if (nextChar == 'N' && pos + 4 < input.length() && input.charAt(pos + 2) == '{') try {
                    int start = pos + 3, end;
                    for (int i = start ;; i++) {
                        if (i == input.length()) break N;
                        char c = input.charAt(i);
                        if (c == '}') {
                            end = i;
                            break;
                        }
                        if (c != ' ' && c != '_' && c != '-' && !Character.isDigit(c) && !Character.isLetter(c)) break N;
                    }
                    token.append(Character.toString(Character.codePointOf(input.substring(start, end))));
                    linepos += end - pos;
                    pos = end;
                    return;
                } catch (IllegalArgumentException ignored) {}
                token.append(nextChar);
            }
        }
        pos++;
        linepos++;
    }

    @Unique
    private int dedent(String input, int start, int end, String indent) {
        int len = Math.min(end - start, indent.length());
        for (int i = 0; i < len; i++)
            if (input.charAt(start + i) != indent.charAt(i))
                return i;
        return len;
    }

    @Inject(method = "next()Lcarpet/script/Tokenizer$Token;", at = @At("HEAD"), cancellable = true)
    private void customSyntax(CallbackInfoReturnable<Tokenizer.Token> cir) {
        Tokenizer.Token token = new Tokenizer.Token();

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
                || dec && (ch == '.' || ch == 'e' || ch == 'E'
                        || (ch == '-' || ch == '+') && token.length() > 0
                        && ('e' == token.charAt(token.length() - 1) || 'E' == token.charAt(token.length() - 1))
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
        } else if (ch == '\'') {
            pos++;
            linepos++;
            TokenInterface.setType(token, TokenTypeInterface.STRINGPARAM);
            if (pos == input.length() && expression != null && context != null) {
                throw new ExpressionException(context, expression, token, "Program truncated");
            }
            ch = input.charAt(pos);
            m: if (ch == '\'' && pos + 1 < input.length() && input.charAt(pos + 1) == '\'') {
                pos += 2;
                linepos += 2;
                if (pos == input.length() && expression != null && context != null) {
                    throw new ExpressionException(context, expression, token, "Program truncated");
                }
                ch = input.charAt(pos);
                w: while (ch != '\n') {
                    if (++pos == input.length() && expression != null && context != null) {
                        throw new ExpressionException(context, expression, token, "Program truncated");
                    }
                    linepos++;
                    ch = input.charAt(pos);
                    if (ch == '\'' && pos + 2 < input.length() && input.charAt(pos + 1) == '\'' && input.charAt(pos + 2) == '\'') {
                        pos += 2;
                        linepos += 2;
                        break m;
                    } else if (comments && ch == '/' && pos + 1 < input.length()) {
                        switch (input.charAt(pos + 1)) {
                            case '/':
                                pos += 2;
                                if (pos == input.length() && expression != null && context != null) {
                                    throw new ExpressionException(context, expression, token, "Program truncated");
                                }
                                ch = input.charAt(pos);
                                while (ch != '\n') {
                                    if (++pos == input.length() && expression != null && context != null) {
                                        throw new ExpressionException(context, expression, token, "Program truncated");
                                    }
                                    ch = input.charAt(pos);
                                }
                                break w;
                            case '*':
                                if (++pos == input.length() && expression != null && context != null) {
                                    throw new ExpressionException(context, expression, token, "Program truncated");
                                }
                                linepos++;
                                while (true) {
                                    if (++pos == input.length() && expression != null && context != null) {
                                        throw new ExpressionException(context, expression, token, "Program truncated");
                                    }
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
                    } else if (Character.isWhitespace(ch)) continue;
                    if (expression != null && context != null) {
                        throw new ExpressionException(context, expression, token, "Only comments are allowed on the starting line of multiline strings");
                    }
                }

                List<StringRange> lines = new ArrayList<>();
                int indentStart;
                w: while (true) {
                    if (++pos == input.length() && expression != null && context != null) {
                        throw new ExpressionException(context, expression, token, "Program truncated");
                    }
                    lineno++;
                    linepos = 0;
                    int start = pos;
                    ch = input.charAt(pos);
                    while (Character.isWhitespace(ch)) {
                        if (ch == '\n') {
                            lines.add(StringRange.between(start, pos));
                            continue w;
                        }
                        if (++pos == input.length() && expression != null && context != null) {
                            throw new ExpressionException(context, expression, token, "Program truncated");
                        }
                        linepos++;
                        ch = input.charAt(pos);
                    }

                    if (ch == '\'' && pos + 2 < input.length() && input.charAt(pos + 1) == '\'' && input.charAt(pos + 2) == '\'') {
                        indentStart = start;
                        break;
                    }

                    while (pos < input.length()) {
                        ch = input.charAt(pos);
                        if (ch == '\n') {
                            lines.add(StringRange.between(start, pos));
                            break;
                        } else if (ch == '\\') escapeCode(token);
                        pos++;
                        linepos++;
                    }

                    if (expression != null && context != null) {
                        throw new ExpressionException(context, expression, token, "Program truncated");
                    }
                }

                String indent = input.substring(indentStart, pos);
                for (Iterator<StringRange> it = lines.iterator(); it.hasNext();) {
                    StringRange line = it.next();
                    token.append(input.substring(line.getStart() + dedent(input, line.getStart(), line.getEnd(), indent)));
                    if (it.hasNext()) token.append('\n');
                }
                pos += 2;
                linepos += 2;
            } else while (ch != '\'') {
                if (ch == '\\') escapeCode(token); else {
                    token.append(ch);
                    if (ch == '\n') {
                        lineno++;
                        linepos = 0;
                    } else linepos++;
                }
                if (++pos == input.length() && expression != null && context != null) {
                    throw new ExpressionException(context, expression, token, "Program truncated");
                }
                ch = input.charAt(pos);
            }
            pos++;
            linepos++;
        } else if (ch == '#') {
            int count = 0;
            while (ch == '#') {
                count++;
                if (++pos == input.length() && expression != null && context != null) {
                    throw new ExpressionException(context, expression, token, "Program truncated");
                }
                linepos++;
                ch = input.charAt(pos);
            }

            if (ch != '\'' && expression != null && context != null) {
                throw new ExpressionException(context, expression, token, "Only a string can be raw");
            }

            s: while (true) {
                if (++pos == input.length() && expression != null && context != null) {
                    throw new ExpressionException(context, expression, token, "Program truncated");
                }
                ch = input.charAt(pos);
                token.append(ch);
                if (ch == '\n') {
                    lineno++;
                    linepos = 0;
                } else linepos++;
                if (ch == '\'')
                    if (pos + count >= input.length() && expression != null && context != null) {
                        throw new ExpressionException(context, expression, token, "Program truncated");
                    } else {
                        for (int i = 1; i <= count; i++)
                            if (input.charAt(pos + i) != '#')
                                continue s;
                        break;
                    }
            }
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
            TokenInterface.setType(token, switch (ch) {
                case '(' -> TokenTypeInterface.OPEN_PAREN;
                case ')' -> TokenTypeInterface.CLOSE_PAREN;
                case ',' -> TokenTypeInterface.COMMA;
                default -> TokenTypeInterface.MARKER;
            });
            token.append(ch);
            pos++;
            linepos++;

            if (expression != null && context != null && previousToken != null &&
                ((TokenInterface)previousToken).lanitium$type() == TokenTypeInterface.OPERATOR &&
                (ch == ')' || ch == ',' || ch == ']' || ch == '}') &&
                !previousToken.surface.equals(";")
            ) {
                throw new ExpressionException(context, expression, previousToken,
                    "Can't have operator " + previousToken.surface + " at the end of a subexpression");
            }
        } else if (ch == '.') {
            token.surface = ".";
            TokenInterface.setType(token, TokenTypeInterface.OPERATOR);
            TokenTypeInterface prevType = previousToken != null ? ((TokenInterface)previousToken).lanitium$type() : null;
            if (prevType == null || prevType == TokenTypeInterface.OPERATOR
                || prevType == TokenTypeInterface.OPEN_PAREN || prevType == TokenTypeInterface.COMMA
                || prevType == TokenTypeInterface.MARKER && (previousToken.surface.equals("{") || previousToken.surface.equals("["))) {
                throw new ExpressionException(context, expression, token, "Member access must come after a value");
            }
            pos++;
            linepos++;
        } else {
            StringBuilder greedyMatch = new StringBuilder();
            int initialPos = pos;
            int initialLinePos = linepos;
            ch = input.charAt(pos);
            int validOperatorSeenUntil = -1;
            while (!Character.isLetter(ch) && !Character.isDigit(ch) && ch != '_'
                && !Character.isWhitespace(ch) && ch != ','
                && ch != '(' && ch != ')' && ch != '[' && ch != ']' && ch != '{' && ch != '}'
                && pos < input.length()) {
                greedyMatch.append(ch);
                if (comments && "/*".contentEquals(greedyMatch)) {
                    while (pos < input.length()) {
                        ch = input.charAt(pos++);
                        greedyMatch.append(ch);
                        if (ch == '*' && pos < input.length() && input.charAt(pos) == '/') {
                            pos++;
                            linepos += 2;
                            greedyMatch.append('/');
                            break;
                        } else if (ch == '\n') {
                            lineno++;
                            linepos = 0;
                            continue;
                        }
                        linepos++;
                    }
                    token.append(greedyMatch.toString());
                    TokenInterface.setType(token, TokenTypeInterface.MARKER);
                    cir.setReturnValue(token);
                    return;
                } else if (comments && "//".contentEquals(greedyMatch)) {
                    while (ch != '\n' && pos < input.length()) {
                        ch = input.charAt(pos++);
                        linepos++;
                        greedyMatch.append(ch);
                    }
                    if (ch == '\n') {
                        lineno++;
                        linepos = 0;
                    }
                    token.append(greedyMatch.toString());
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
            if (prevType == null || prevType == TokenTypeInterface.OPERATOR
                || prevType == TokenTypeInterface.OPEN_PAREN || prevType == TokenTypeInterface.COMMA
                || (prevType == TokenTypeInterface.MARKER && (previousToken.surface.equals("{") || previousToken.surface.equals("[")))
            ) {
                token.surface += "u";
                TokenInterface.setType(token, TokenTypeInterface.UNARY_OPERATOR);
            } else {
                TokenInterface.setType(token, TokenTypeInterface.OPERATOR);
            }
        }
        TokenTypeInterface type = ((TokenInterface)token).lanitium$type(),
                           prevType = previousToken != null
                               ? ((TokenInterface)previousToken).lanitium$type()
                               : null;
        if (expression != null && context != null && previousToken != null && (
            (
                type == TokenTypeInterface.LITERAL ||
                type == TokenTypeInterface.HEX_LITERAL ||
                type == TokenTypeInterface.VARIABLE ||
                type == TokenTypeInterface.STRINGPARAM ||
                type == TokenTypeInterface.FUNCTION ||
                type == TokenTypeInterface.OPEN_PAREN ||
                type == TokenTypeInterface.MARKER && token.surface.equals("{")
            ) && (
                prevType == TokenTypeInterface.LITERAL ||
                prevType == TokenTypeInterface.HEX_LITERAL ||
                prevType == TokenTypeInterface.VARIABLE ||
                prevType == TokenTypeInterface.STRINGPARAM ||
                prevType == TokenTypeInterface.CLOSE_PAREN ||
                prevType == TokenTypeInterface.MARKER && (previousToken.surface.equals("}") || previousToken.surface.equals("]"))
            ) || (
                type != TokenTypeInterface.VARIABLE &&
                type != TokenTypeInterface.FUNCTION &&
                prevType == TokenTypeInterface.OPERATOR &&
                previousToken.surface.equals(".")
            )
        )) {
            throw new ExpressionException(context, expression, previousToken, '\'' + token.surface + "' is not allowed after '" + previousToken.surface + '\'');
        }
        cir.setReturnValue(previousToken = token);
    }

    @Inject(method = "postProcess", at = @At("HEAD"), cancellable = true)
    private void postProcess(CallbackInfoReturnable<List<Tokenizer.Token>> cir) {
        Iterable<Tokenizer.Token> iterable = () -> (Tokenizer)(Object)this;
        List<Tokenizer.Token> originalTokens = StreamSupport.stream(iterable.spliterator(), false).collect(Collectors.toList());
        List<Tokenizer.Token> cleanedTokens = new ArrayList<>();
        Tokenizer.Token last = null;
        Stack<Integer> bracketStack = new Stack<>();
        while (!originalTokens.isEmpty()) {
            Tokenizer.Token current = originalTokens.removeLast();
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

                        if (!originalTokens.isEmpty() && ((TokenInterface)originalTokens.getLast()).lanitium$type() == TokenTypeInterface.FUNCTION) {
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

                        if (!originalTokens.isEmpty()) {
                            Tokenizer.Token prev = originalTokens.getLast();
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
                } else if (currentType == TokenTypeInterface.STRINGPARAM && !originalTokens.isEmpty() && ((TokenInterface)originalTokens.getLast()).lanitium$type() == TokenTypeInterface.FUNCTION) {
                    cleanedTokens.add(((TokenInterface)current).lanitium$morphedInto(TokenTypeInterface.CLOSE_PAREN, ")"));
                    cleanedTokens.add(current);
                    cleanedTokens.add(((TokenInterface)current).lanitium$morphedInto(TokenTypeInterface.OPEN_PAREN, "("));
                    last = current;
                    continue;
                } else if (!originalTokens.isEmpty() && ((TokenInterface)originalTokens.getLast()).lanitium$type() == TokenTypeInterface.OPERATOR && originalTokens.getLast().surface.equals(".")) {
                    if (currentType == TokenTypeInterface.VARIABLE) {
                        originalTokens.getLast().surface = ":";
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

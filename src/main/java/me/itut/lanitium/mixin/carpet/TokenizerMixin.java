package me.itut.lanitium.mixin.carpet;

import carpet.script.Context;
import carpet.script.Expression;
import carpet.script.Tokenizer;
import carpet.script.exception.ExpressionException;
import me.itut.lanitium.internal.carpet.TokenInterface;
import me.itut.lanitium.internal.carpet.TokenTypeInterface;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.math.BigInteger;

@Mixin(value = Tokenizer.class, remap = false)
public abstract class TokenizerMixin {
    @Shadow private int pos;
    @Shadow @Final private String input;
    @Shadow private Tokenizer.Token previousToken;
    @Shadow private int linepos;
    @Shadow private int lineno;
    @Shadow protected abstract char peekNextChar();
    @Shadow protected abstract boolean isHexDigit(char ch);
    @Shadow @Final private Expression expression;
    @Shadow @Final private Context context;
    @Shadow @Final private boolean comments;
    @Shadow @Final private boolean newLinesMarkers;

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
            boolean hex = false, bin = false;
            if (ch == '0')
                if (peekNextChar() == 'x' || peekNextChar() == 'X') hex = true;
                else if (peekNextChar() == 'b' || peekNextChar() == 'B') bin = true;

            while (ch == '_' || ch >= '0' && ch <= '9' || hex && (ch >= 'a' && ch <= 'f' || ch >= 'A' && ch <= 'F' || ch == 'x' || ch == 'X') || bin && (ch == 'b' || ch == 'B') || (
                Character.isDigit(ch) || ch == '.' || ch == 'e' || ch == 'E' || (ch == '-' || ch == '+') && token.length() > 0 && ('e' == token.charAt(token.length() - 1) || 'E' == token.charAt(token.length() - 1))
            ) && pos < input.length()) {
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
            while (ch != '\'') {
                if (ch == '\\') {
                    char nextChar = peekNextChar();
                    if (nextChar == 'n') {
                        token.append('\n');
                    } else if (nextChar == 't') {
                        token.append('\t');
                    } else if (nextChar == 'r') {
                        token.append('\r');
                    } else if (nextChar == '\\' || nextChar == '\'') {
                        token.append(nextChar);
                    } else {
                        pos--;
                        linepos--;
                    }
                    pos += 2;
                    linepos += 2;
                } else {
                    token.append(input.charAt(pos++));
                    linepos++;
                    if (ch == '\n') {
                        lineno++;
                        linepos = 0;
                    }
                }
                if (pos == input.length() && expression != null && context != null) {
                    throw new ExpressionException(context, this.expression, token, "Program truncated");
                }
                ch = input.charAt(pos);
            }
            pos++;
            linepos++;
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
            TokenInterface.setType(token, ch == '(' ? TokenTypeInterface.FUNCTION : TokenTypeInterface.VARIABLE);
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
                !previousToken.surface.equalsIgnoreCase(";")
            ) {
                throw new ExpressionException(context, this.expression, previousToken,
                    "Can't have operator " + previousToken.surface + " at the end of a subexpression");
            }
        } else {
            StringBuilder greedyMatch = new StringBuilder();
            int initialPos = pos;
            int initialLinePos = linepos;
            ch = input.charAt(pos);
            int validOperatorSeenUntil = -1;
            while (!Character.isLetter(ch) && !Character.isDigit(ch) && ch != '_'
                && !Character.isWhitespace(ch) && ch != '(' && ch != ')' && ch != ','
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
                    token.append("/" + greedyMatch);
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
        if (expression != null && context != null && previousToken != null &&
            (
                type == TokenTypeInterface.LITERAL ||
                type == TokenTypeInterface.HEX_LITERAL ||
                type == TokenTypeInterface.VARIABLE ||
                type == TokenTypeInterface.STRINGPARAM ||
                type == TokenTypeInterface.MARKER && (previousToken.surface.equals("{") || previousToken.surface.equals("[")) ||
                type == TokenTypeInterface.FUNCTION
            ) && (
                prevType == TokenTypeInterface.VARIABLE ||
                prevType == TokenTypeInterface.FUNCTION ||
                prevType == TokenTypeInterface.LITERAL ||
                prevType == TokenTypeInterface.CLOSE_PAREN ||
                prevType == TokenTypeInterface.MARKER && (previousToken.surface.equals("}") || previousToken.surface.equals("]")) ||
                prevType == TokenTypeInterface.HEX_LITERAL ||
                prevType == TokenTypeInterface.STRINGPARAM
            )
        ) {
            throw new ExpressionException(context, this.expression, previousToken, "'" + token.surface + "' is not allowed after '" + previousToken.surface + "'");
        }
        cir.setReturnValue(previousToken = token);
    }
}

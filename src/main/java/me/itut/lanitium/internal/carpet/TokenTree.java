package me.itut.lanitium.internal.carpet;

import carpet.script.Token;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public sealed interface TokenTree {
    enum BracketType { ROUND, SQUARE, CURLY, INTERPOLATION }

    record Single(Token token) implements TokenTree {}
    record Brackets(BracketType type, Token open, List<TokenTree> contents, Token close) implements TokenTree {}

    record StackFrame(@Nullable BracketType type, Token open, List<TokenTree> contents) {}

    record StringInterpolation(TokenTree interpolation, Token token) {}
    record StringLine(Token start, List<StringInterpolation> interpolations, boolean noNewline) {}
}

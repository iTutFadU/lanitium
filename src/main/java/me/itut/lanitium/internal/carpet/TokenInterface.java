package me.itut.lanitium.internal.carpet;

import carpet.script.Token;

public interface TokenInterface {
    TokenInterface NONE = (TokenInterface)Token.NONE;

    static void setType(Token token, TokenTypeInterface type) {
        ((TokenInterface)token).lanitium$setType(type);
    }

    static TokenTypeInterface type(Token token) {
        return ((TokenInterface)token).lanitium$type();
    }

    static Token morphedInto(Token token, TokenTypeInterface newType, String newSurface) {
        return ((TokenInterface)token).lanitium$morphedInto(newType, newSurface);
    }

    static Token morph(Token token, TokenTypeInterface type, String s) {
        ((TokenInterface)token).lanitium$morph(type, s);
        return token;
    }

    static Token at(int pos, int lineno, int linepos) {
        return relocate(new Token(), pos, lineno, linepos);
    }

    static Token at(Lexer lexer) {
        return at(lexer.pos, lexer.lineno, lexer.linepos);
    }

    static Token relocate(Token token, int pos, int lineno, int linepos) {
        token.pos = pos;
        token.lineno = lineno;
        token.linepos = linepos;
        return token;
    }

    static Token relocate(Token token, Lexer lexer) {
        return relocate(token, lexer.pos, lexer.lineno, lexer.linepos);
    }

    TokenTypeInterface lanitium$byName(String name);
    TokenTypeInterface lanitium$type();
    void lanitium$setType(TokenTypeInterface type);
    Token lanitium$morphedInto(TokenTypeInterface newType, String newSurface);
    void lanitium$morph(TokenTypeInterface type, String s);
}

package me.itut.lanitium.internal.carpet;

import carpet.script.Token;

public interface TokenInterface {
    TokenInterface NONE = (TokenInterface)Token.NONE;
    static void setType(Token token, TokenTypeInterface type) {
        ((TokenInterface)token).lanitium$setType(type);
    }

    TokenTypeInterface lanitium$byName(String name);
    TokenTypeInterface lanitium$type();
    void lanitium$setType(TokenTypeInterface type);
    Token lanitium$morphedInto(TokenTypeInterface newType, String newSurface);
    void lanitium$morph(TokenTypeInterface type, String s);
}

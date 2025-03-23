package me.itut.lanitium.internal.carpet;

import carpet.script.Tokenizer;

public interface TokenInterface {
    TokenInterface NONE = (TokenInterface)Tokenizer.Token.NONE;
    static void setType(Tokenizer.Token token, TokenTypeInterface type) {
        ((TokenInterface)token).lanitium$setType(type);
    }

    TokenTypeInterface lanitium$byName(String name);
    TokenTypeInterface lanitium$type();
    void lanitium$setType(TokenTypeInterface type);
    Tokenizer.Token lanitium$morphedInto(TokenTypeInterface newType, String newSurface);
    void lanitium$morph(TokenTypeInterface type, String s);
}

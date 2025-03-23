package me.itut.lanitium.internal.carpet;

public interface TokenTypeInterface {
    TokenTypeInterface FUNCTION = TokenInterface.NONE.lanitium$byName("FUNCTION");
    TokenTypeInterface OPERATOR = TokenInterface.NONE.lanitium$byName("OPERATOR");
    TokenTypeInterface UNARY_OPERATOR = TokenInterface.NONE.lanitium$byName("UNARY_OPERATOR");
    TokenTypeInterface VARIABLE = TokenInterface.NONE.lanitium$byName("VARIABLE");
    TokenTypeInterface CONSTANT = TokenInterface.NONE.lanitium$byName("CONSTANT");
    TokenTypeInterface LITERAL = TokenInterface.NONE.lanitium$byName("LITERAL");
    TokenTypeInterface HEX_LITERAL = TokenInterface.NONE.lanitium$byName("HEX_LITERAL");
    TokenTypeInterface STRINGPARAM = TokenInterface.NONE.lanitium$byName("STRINGPARAM");
    TokenTypeInterface OPEN_PAREN = TokenInterface.NONE.lanitium$byName("OPEN_PAREN");
    TokenTypeInterface COMMA = TokenInterface.NONE.lanitium$byName("COMMA");
    TokenTypeInterface CLOSE_PAREN = TokenInterface.NONE.lanitium$byName("CLOSE_PAREN");
    TokenTypeInterface MARKER = TokenInterface.NONE.lanitium$byName("MARKER");

    boolean lanitium$functional();
    boolean lanitium$constant();
}

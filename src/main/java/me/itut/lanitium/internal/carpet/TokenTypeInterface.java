package me.itut.lanitium.internal.carpet;

public interface TokenTypeInterface {
    TokenTypeInterface FUNCTION = TokenInterface.NONE.lanitium$byName(TokenTypeName.FUNCTION);
    TokenTypeInterface OPERATOR = TokenInterface.NONE.lanitium$byName(TokenTypeName.OPERATOR);
    TokenTypeInterface UNARY_OPERATOR = TokenInterface.NONE.lanitium$byName(TokenTypeName.UNARY_OPERATOR);
    TokenTypeInterface VARIABLE = TokenInterface.NONE.lanitium$byName(TokenTypeName.VARIABLE);
    TokenTypeInterface CONSTANT = TokenInterface.NONE.lanitium$byName(TokenTypeName.CONSTANT);
    TokenTypeInterface LITERAL = TokenInterface.NONE.lanitium$byName(TokenTypeName.LITERAL);
    TokenTypeInterface HEX_LITERAL = TokenInterface.NONE.lanitium$byName(TokenTypeName.HEX_LITERAL);
    TokenTypeInterface STRINGPARAM = TokenInterface.NONE.lanitium$byName(TokenTypeName.STRINGPARAM);
    TokenTypeInterface OPEN_PAREN = TokenInterface.NONE.lanitium$byName(TokenTypeName.OPEN_PAREN);
    TokenTypeInterface COMMA = TokenInterface.NONE.lanitium$byName(TokenTypeName.COMMA);
    TokenTypeInterface CLOSE_PAREN = TokenInterface.NONE.lanitium$byName(TokenTypeName.CLOSE_PAREN);
    TokenTypeInterface MARKER = TokenInterface.NONE.lanitium$byName(TokenTypeName.MARKER);

    boolean lanitium$functional();
    boolean lanitium$constant();
    String name();
}

package me.itut.lanitium.value.pattern;

import carpet.script.Context;
import carpet.script.Expression;
import carpet.script.Token;
import carpet.script.exception.ExpressionException;
import carpet.script.value.FrameworkValue;
import carpet.script.value.Value;

public class EntryPatternValue extends FrameworkValue {
    public final Expression expression;
    public final Token token;
    public final Value key, pattern;

    public EntryPatternValue(Expression expression, Token token, Value key, Value pattern) {
        this.expression = expression;
        this.token = token;
        this.key = key;
        this.pattern = pattern;
    }

    public static void checkPattern(Context context, Value pattern) throws ExpressionException {
        switch (pattern) {
            case EntryPatternValue entry -> throw new ExpressionException(context, entry.expression, entry.token, "Multiple entry patterns are not allowed");
            case RestPatternValue r -> throw new ExpressionException(context, r.expression, r.token, "Rest pattern cannot be mapped");
            default -> {}
        }
    }
}

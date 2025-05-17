package me.itut.lanitium.value.pattern;

import carpet.script.Context;
import carpet.script.Expression;
import carpet.script.LazyValue;
import carpet.script.Token;
import carpet.script.exception.ExpressionException;
import carpet.script.value.FrameworkValue;
import carpet.script.value.LContainerValue;
import carpet.script.value.Value;

public class DefaultPatternValue extends FrameworkValue {
    public final Expression expression;
    public final Token token;
    public final Value pattern;
    public final LazyValue defaultValue;

    public DefaultPatternValue(Expression expression, Token token, Value pattern, LazyValue defaultValue) {
        this.expression = expression;
        this.token = token;
        this.pattern = pattern;
        this.defaultValue = defaultValue;
    }

    public static void checkPattern(Expression expression, Token token, Context context, Value pattern) throws ExpressionException {
        switch (pattern) {
            case LContainerValue ignored -> {}
            case ConditionPatternValue ignored -> {}
            case DefaultPatternValue defaultPattern -> throw new ExpressionException(context, defaultPattern.expression, defaultPattern.token, "Multiple default patterns are not allowed");
            case EntryPatternValue entry -> throw new ExpressionException(context, entry.expression, entry.token, "Entry pattern is not allowed in a default pattern, use key -> pattern = default instead of (key -> pattern) = default");
            case ListPatternValue ignored -> {}
            case MapPatternValue ignored -> {}
            case RestPatternValue r -> throw new ExpressionException(context, r.expression, r.token, "Rest pattern cannot have a default value");
            default -> {
                if (!pattern.isBound())
                    throw new ExpressionException(context, expression, token, "Literal pattern cannot have a default value");
            }
        }
    }
}

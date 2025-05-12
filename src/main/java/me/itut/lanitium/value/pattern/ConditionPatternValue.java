package me.itut.lanitium.value.pattern;

import carpet.script.*;
import carpet.script.exception.ExpressionException;
import carpet.script.value.FrameworkValue;
import carpet.script.value.LContainerValue;
import carpet.script.value.Value;

public class ConditionPatternValue extends FrameworkValue {
    public final Expression expression;
    public final Token token;
    public final Value pattern;
    public final LazyValue condition;

    public ConditionPatternValue(Expression expression, Token token, Value pattern, LazyValue condition) {
        this.expression = expression;
        this.token = token;
        this.pattern = pattern;
        this.condition = condition;
    }

    public static void checkPattern(Expression expression, Token token, Context context, Value pattern) throws ExpressionException {
        switch (pattern) {
            case LContainerValue ignored -> throw new ExpressionException(context, expression, token, "Container mutation cannot have a condition");
            case ConditionPatternValue condition -> throw new ExpressionException(context, condition.expression, condition.token, "Multiple condition patterns are not allowed, use pattern && (condition1 && condition2) instead of pattern && condition1 && condition2");
            case DefaultPatternValue defaultPattern -> throw new ExpressionException(context, defaultPattern.expression, defaultPattern.token, "Default pattern is not allowed in a condition pattern, use pattern && (condition) = default instead of (pattern = default) && (condition)");
            case EntryPatternValue entry -> throw new ExpressionException(context, entry.expression, entry.token, "Entry pattern is not allowed in a condition pattern, use key -> pattern && (condition) instead of (key -> pattern) && (condition)");
            case ListPatternValue ignored -> {}
            case MapPatternValue ignored -> {}
            case RestPatternValue ignored -> {}
            default -> {
                if (!pattern.isBound())
                    throw new ExpressionException(context, expression, token, "Literal pattern cannot have a condition, use var && (var == literal && condition) instead of literal && (condition)");
            }
        }
    }
}

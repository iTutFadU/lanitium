package me.itut.lanitium.value.pattern;

import carpet.script.Context;
import carpet.script.Expression;
import carpet.script.Token;
import carpet.script.exception.ExpressionException;
import carpet.script.value.FrameworkValue;
import carpet.script.value.LContainerValue;
import carpet.script.value.Value;

public class RestPatternValue extends FrameworkValue {
    public final Expression expression;
    public final Token token;
    public final Value pattern;

    public RestPatternValue(Expression expression, Token token, Value pattern) {
        this.expression = expression;
        this.token = token;
        this.pattern = pattern;
    }

    public static void checkPattern(Expression expression, Token token, Context context, Value pattern) throws ExpressionException {
        switch (pattern) {
            case LContainerValue ignored -> {}
            case ConditionPatternValue condition -> throw new ExpressionException(context, condition.expression, condition.token, "Condition pattern is not allowed in a rest pattern, use ...pattern && (condition) instead of ...(pattern && (condition))");
            case DefaultPatternValue defaultPattern -> throw new ExpressionException(context, defaultPattern.expression, defaultPattern.token, "Default pattern is not allowed in a rest pattern");
            case EntryPatternValue entry -> throw new ExpressionException(context, entry.expression, entry.token, "Entry pattern is not allowed in a rest pattern");
            case ListPatternValue list -> throw new ExpressionException(context, list.expression, list.token, "List pattern is not allowed in a rest pattern, use pattern1, pattern2, pattern3 instead of ...[pattern1, pattern2, pattern3]");
            case MapPatternValue map -> throw new ExpressionException(context, map.expression, map.token, "Map pattern is not allowed in a rest pattern, use pattern1, pattern2, pattern3 instead of ...{pattern1, pattern2, pattern3}");
            case RestPatternValue r -> throw new ExpressionException(context, r.expression, r.token, "Multiple rest patterns are not allowed");
            default -> {
                if (!pattern.isBound())
                    throw new ExpressionException(context, expression, token, "Literal pattern is not allowed in a rest pattern");
            }
        }
    }
}

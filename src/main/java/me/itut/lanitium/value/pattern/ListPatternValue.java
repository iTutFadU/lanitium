package me.itut.lanitium.value.pattern;

import carpet.script.Context;
import carpet.script.Expression;
import carpet.script.Token;
import carpet.script.exception.ExpressionException;
import carpet.script.value.FrameworkValue;
import carpet.script.value.Value;
import org.jetbrains.annotations.Contract;

import java.util.List;

public class ListPatternValue extends FrameworkValue {
    public final Expression expression;
    public final Token token;
    public final List<Value> values;
    public final int rest;

    public ListPatternValue(Expression expression, Token token, List<Value> values, int rest) {
        this.expression = expression;
        this.token = token;
        this.values = values;
        this.rest = rest;
    }

    @Contract(mutates = "param3")
    public static void checkPattern(Context context, Value pattern, int[] rest, int index) throws ExpressionException {
        switch (pattern) {
            case ConditionPatternValue condition -> checkPattern(context, condition.pattern, rest, index);
            case DefaultPatternValue defaultPattern -> throw new ExpressionException(context, defaultPattern.expression, defaultPattern.token, "Default pattern is not allowed in a list pattern");
            case EntryPatternValue entry -> throw new ExpressionException(context, entry.expression, entry.token, "Entry pattern is not allowed in a list pattern");
            case RestPatternValue r -> {
                if (rest[0] >= 0)
                    throw new ExpressionException(context, r.expression, r.token, "Rest pattern is already defined for this list pattern");
                else if (r.pattern instanceof MapPatternValue l)
                    throw new ExpressionException(context, l.expression, l.token, "Map rest pattern is not allowed in a list pattern");
                rest[0] = index;
            }
            default -> {}
        }
    }
}

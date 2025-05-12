package me.itut.lanitium.value.pattern;

import carpet.script.Context;
import carpet.script.Expression;
import carpet.script.Token;
import carpet.script.exception.ExpressionException;
import carpet.script.value.FrameworkValue;
import carpet.script.value.LContainerValue;
import carpet.script.value.Value;
import org.jetbrains.annotations.Contract;

import java.util.List;

public class MapPatternValue extends FrameworkValue {
    public final Expression expression;
    public final Token token;
    public final List<Value> values;
    public final int rest, minSize;

    public MapPatternValue(Expression expression, Token token, List<Value> values, int rest, int minSize) {
        this.expression = expression;
        this.token = token;
        this.values = values;
        this.rest = rest;
        this.minSize = minSize;
    }

    @Contract(mutates = "param5") // vars[0] - rest, vars[1] - minSize
    public static void checkPattern(Expression expression, Token token, Context context, Value pattern, int[] vars, int index, boolean inEntry, boolean optional) throws ExpressionException {
        switch (pattern) {
            case LContainerValue ignored -> {
                if (!inEntry)
                    throw new ExpressionException(context, expression, token, "Container mutation is not allowed in a map pattern, use '->' to create an entry pattern");
            }
            case ConditionPatternValue condition -> checkPattern(condition.expression, condition.token, context, condition.pattern, vars, index, inEntry, optional);
            case DefaultPatternValue defaultPattern -> checkPattern(defaultPattern.expression, defaultPattern.token, context, defaultPattern.pattern, vars, index, inEntry, optional = true);
            case EntryPatternValue entry -> checkPattern(entry.expression, entry.token, context, entry.pattern, vars, index, true, false);
            case ListPatternValue list -> {
                if (!inEntry)
                    throw new ExpressionException(context, list.expression, list.token, "List pattern is not allowed in a map pattern, use '->' to create an entry pattern");
            }
            case MapPatternValue map -> {
                if (!inEntry)
                    throw new ExpressionException(context, map.expression, map.token, "Map pattern is not allowed in a map pattern, use '->' to create an entry pattern");
            }
            case RestPatternValue r -> {
                if (vars[0] >= 0)
                    throw new ExpressionException(context, r.expression, r.token, "Rest pattern is already defined for this map pattern");
                else if (r.pattern instanceof ListPatternValue l)
                    throw new ExpressionException(context, l.expression, l.token, "List rest pattern is not allowed in a map pattern");
                optional = true;
                vars[0] = index;
            }
            default -> {
                if (!inEntry && !pattern.isBound())
                    throw new ExpressionException(context, expression, token, "Literal pattern is not allowed in a map pattern, use '->' to create an entry pattern");
            }
        }
        if (!optional) vars[1]++;
    }
}

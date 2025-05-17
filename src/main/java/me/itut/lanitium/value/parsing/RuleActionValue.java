package me.itut.lanitium.value.parsing;

import carpet.script.Context;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.FunctionValue;
import carpet.script.value.NullValue;
import carpet.script.value.Value;
import com.mojang.brigadier.StringReader;
import me.itut.lanitium.value.ObjectFunctionValue;
import me.itut.lanitium.value.SimpleFunctionValue;
import net.minecraft.util.parsing.packrat.ParseState;
import net.minecraft.util.parsing.packrat.Rule;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class RuleActionValue extends ObjectFunctionValue<Rule.RuleAction<StringReader, Value>> {
    protected RuleActionValue(Rule.RuleAction<StringReader, Value> value) {
        super(value, value instanceof Fn fn ? fn.fn : new SimpleFunctionValue(1, 1, (c, t, e, tok, lv) -> Objects.requireNonNullElse(value.run(StateValue.from(lv.getFirst())), Value.NULL)));
    }

    protected RuleActionValue(RuleActionValue self) {
        super(self.value, self);
    }

    public static Value of(Rule.RuleAction<StringReader, Value> value) {
        return value != null ? new RuleActionValue(value) : Value.NULL;
    }

    public static Rule.RuleAction<StringReader, Value> from(Context c, Value value) {
        return switch (value) {
            case null -> null;
            case NullValue ignored -> null;
            case RuleActionValue action -> action.value;
            case ObjectFunctionValue<?> ignored -> throw new InternalExpressionException("Cannot convert " + value.getTypeString() + " to parsing_rule_action");
            case FunctionValue fn -> new Fn(c, new SimpleFunctionValue(1, 1, (cc, t, e, tok, lv) -> fn.execute(cc, t, e, tok, lv, null).evalValue(cc, t)));
            default -> throw new InternalExpressionException("Cannot convert " + value.getTypeString() + " to parsing_rule_action");
        };
    }

    @Override
    public String getTypeString() {
        return "parsing_rule_action";
    }

    @Override
    protected Value clone() {
        return new RuleActionValue(this);
    }

    private record Fn(Context c, SimpleFunctionValue fn) implements Rule.RuleAction<StringReader, Value> {
        @Override
        public @Nullable Value run(ParseState<StringReader> state) {
            Value ret = fn.callInContext(c, Context.NONE, List.of(StateValue.of(c, state))).evalValue(c);
            return ret.isNull() ? null : ret;
        }
    }
}

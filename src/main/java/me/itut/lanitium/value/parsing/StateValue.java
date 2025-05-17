package me.itut.lanitium.value.parsing;

import carpet.script.Context;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.FunctionValue;
import carpet.script.value.NullValue;
import carpet.script.value.NumericValue;
import carpet.script.value.Value;
import com.mojang.brigadier.StringReader;
import me.itut.lanitium.value.ObjectValue;
import net.minecraft.util.parsing.packrat.ParseState;

import java.util.List;
import java.util.Objects;

public class StateValue extends ObjectValue<ParseState<StringReader>> {
    private final Context context;

    protected StateValue(Context context, ParseState<StringReader> value) {
        super(value);
        this.context = context;
    }

    public static Value of(Context context, ParseState<StringReader> value) {
        return value != null ? new StateValue(context, value) : Value.NULL;
    }

    public static ParseState<StringReader> from(Value value) {
        return switch (value) {
            case null -> null;
            case NullValue ignored -> null;
            case StateValue state -> state.value;
            default -> throw new InternalExpressionException("Cannot convert " + value.getTypeString() + " to parsing_state");
        };
    }

    @Override
    public Value get(String what, Value... more) {
        return switch (what) {
            case "scope" -> {
                checkArguments(what, more, 0);
                yield ScopeValue.of(value.scope());
            }
            case "error_collector" -> {
                checkArguments(what, more, 0);
                yield ErrorCollectorValue.of(context, value.errorCollector());
            }
            case "parse_top_rule" -> {
                checkArguments(what, more, 1);
                yield value.parseTopRule(NamedRuleValue.from(context, more[0])).orElse(Value.NULL);
            }
            case "parse" -> {
                checkArguments(what, more, 1);
                yield Objects.requireNonNullElse(value.parse(NamedRuleValue.from(context, more[0])), Value.NULL);
            }
            case "input" -> {
                checkArguments(what, more, 0);
                yield StringReaderValue.of(value.input());
            }
            case "mark" -> {
                checkArguments(what, more, 0);
                yield NumericValue.of(value.mark());
            }
            case "restore" -> {
                checkArguments(what, more, 1);
                value.restore(NumericValue.asNumber(more[0]).getInt());
                yield Value.NULL;
            }
            case "acquire_control" -> {
                checkArguments(what, more, 0, 1);
                Value control = ControlValue.of(value.acquireControl());
                if (more.length == 0) yield control;
                try {
                    if (!(more[0] instanceof FunctionValue fn))
                        throw new InternalExpressionException("A function must be used to acquire control");
                    yield fn.callInContext(context, Context.NONE, List.of(control)).evalValue(context);
                } finally {
                    value.releaseControl();
                }
            }
            case "release_control" -> {
                checkArguments(what, more, 0);
                value.releaseControl();
                yield Value.NULL;
            }
            case "silent" -> {
                checkArguments(what, more, 0);
                ParseState<StringReader> silent = value.silent();
                yield silent == value ? this : new StateValue(context, silent);
            }
            default -> unknownFeature(what);
        };
    }

    @Override
    public String getTypeString() {
        return "parsing_state";
    }
}

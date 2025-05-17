package me.itut.lanitium.value.parsing;

import carpet.script.Context;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.MapValue;
import carpet.script.value.NullValue;
import carpet.script.value.Value;
import com.mojang.brigadier.StringReader;
import me.itut.lanitium.value.ObjectValue;
import net.minecraft.util.parsing.packrat.Atom;
import net.minecraft.util.parsing.packrat.Dictionary;

public class DictionaryValue extends ObjectValue<Dictionary<StringReader>> {
    private final Context context;

    protected DictionaryValue(Context context, Dictionary<StringReader> value) {
        super(value);
        this.context = context;
    }

    public static Value of(Context context, Dictionary<StringReader> value) {
        return value != null ? new DictionaryValue(context, value) : Value.NULL;
    }

    public static Dictionary<StringReader> from(Context c, Value value) {
        return switch (value) {
            case null -> null;
            case NullValue ignored -> null;
            case DictionaryValue dict -> dict.value;
            case MapValue map -> {
                Dictionary<StringReader> dict = new Dictionary<>();
                map.getMap().forEach((k, v) -> dict.put(AtomValue.from(k), RuleValue.from(c, v)));
                yield dict;
            }
            default -> throw new InternalExpressionException("Cannot convert " + value.getTypeString() + " to parsing_dictionary");
        };
    }

    @Override
    public Value get(String what, Value... more) {
        return switch (what) {
            case "put" -> {
                checkArguments(what, more, 2, 3);
                Atom<Value> atom = AtomValue.from(more[0]);
                yield NamedRuleValue.of(more.length == 3
                    ? value.putComplex(atom, TermValue.from(context, more[1]), RuleActionValue.from(context, more[2]))
                    : value.put(atom, RuleValue.from(context, more[1]))
                );
            }
            case "check_all_bound" -> {
                checkArguments(what, more, 0);
                value.checkAllBound();
                yield Value.NULL;
            }
            case "get" -> {
                checkArguments(what, more, 1);
                yield NamedRuleValue.of(value.getOrThrow(AtomValue.from(more[0])));
            }
            case "forward" -> {
                checkArguments(what, more, 1);
                yield NamedRuleValue.of(value.forward(AtomValue.from(more[0])));
            }
            case "named" -> {
                checkArguments(what, more, 1);
                yield TermValue.of(value.named(AtomValue.from(more[0])));
            }
            case "alias" -> {
                checkArguments(what, more, 2);
                yield TermValue.of(value.namedWithAlias(AtomValue.from(more[0]), AtomValue.from(more[1])));
            }
            default -> unknownFeature(what);
        };
    }

    @Override
    public String getTypeString() {
        return "parsing_dictionary";
    }
}

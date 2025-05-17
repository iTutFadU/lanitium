package me.itut.lanitium.value.parsing;

import carpet.script.Context;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.AbstractListValue;
import carpet.script.value.NullValue;
import carpet.script.value.Value;
import com.mojang.brigadier.StringReader;
import me.itut.lanitium.value.ObjectValue;
import net.minecraft.util.parsing.packrat.Atom;
import net.minecraft.util.parsing.packrat.NamedRule;
import net.minecraft.util.parsing.packrat.Rule;

import java.util.List;

public class NamedRuleValue extends ObjectValue<NamedRule<StringReader, Value>> {
    protected NamedRuleValue(NamedRule<StringReader, Value> value) {
        super(value);
    }

    public static Value of(NamedRule<StringReader, Value> value) {
        return value != null ? new NamedRuleValue(value) : Value.NULL;
    }

    public static NamedRule<StringReader, Value> from(Context c, Value value) {
        return switch (value) {
            case null -> null;
            case NullValue ignored -> null;
            case NamedRuleValue rule -> rule.value;
            case AbstractListValue list -> new NamedRule<>() {
                final Atom<Value> name;
                final Rule<StringReader, Value> value;

                {
                    List<Value> values = list.unpack();
                    if (values.size() != 2)
                        throw new InternalExpressionException("Named rule must be a list with an atom and a rule");
                    name = AtomValue.from(values.getFirst());
                    value = RuleValue.from(c, values.getLast());
                }

                @Override
                public Atom<Value> name() {
                    return name;
                }

                @Override
                public Rule<StringReader, Value> value() {
                    return value;
                }
            };
            default -> throw new InternalExpressionException("Cannot convert " + value.getTypeString() + " to parsing_named_rule");
        };
    }

    @Override
    public Value get(String what, Value... more) {
        return switch (what) {
            case "name" -> AtomValue.of(value.name());
            case "value" -> RuleValue.of(value.value());
            default -> unknownFeature(what);
        };
    }

    @Override
    public String getTypeString() {
        return "parsing_named_rule";
    }
}

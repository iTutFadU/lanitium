package me.itut.lanitium.value.brigadier.context;

import carpet.script.CarpetContext;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.NullValue;
import carpet.script.value.NumericValue;
import carpet.script.value.Value;
import com.mojang.brigadier.context.SuggestionContext;
import me.itut.lanitium.value.ObjectValue;
import me.itut.lanitium.value.brigadier.tree.CommandNodeValue;
import net.minecraft.commands.CommandSourceStack;

public class SuggestionContextValue extends ObjectValue<SuggestionContext<CommandSourceStack>> {
    protected SuggestionContextValue(CarpetContext context, SuggestionContext<CommandSourceStack> value) {
        super(context, value);
    }

    public static Value of(CarpetContext context, SuggestionContext<CommandSourceStack> value) {
        return value != null ? new SuggestionContextValue(context, value) : Value.NULL;
    }

    public static SuggestionContext<CommandSourceStack> from(Value value) {
        return switch (value) {
            case null -> null;
            case NullValue ignored -> null;
            case SuggestionContextValue v -> v.value;
            default -> throw new InternalExpressionException("Cannot convert " + value.getTypeString() + " to suggestion_context");
        };
    }

    @Override
    public Value get(String what, Value... more) {
        return switch (what) {
            case "parent" -> {
                checkArguments(what, more, 0);
                yield CommandNodeValue.of(context, value.parent);
            }
            case "start" -> {
                checkArguments(what, more, 0);
                yield NumericValue.of(value.startPos);
            }
            default -> unknownFeature(what);
        };
    }

    @Override
    public String getTypeString() {
        return "suggestion_context";
    }
}

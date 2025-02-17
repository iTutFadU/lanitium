package me.itut.lanitium.value.brigadier;

import carpet.script.CarpetContext;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.NullValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;

public class RequiredArgumentBuilderValue<T> extends ArgumentBuilderValue<RequiredArgumentBuilder<CommandSourceStack, T>> {
    protected RequiredArgumentBuilderValue(CarpetContext context, RequiredArgumentBuilder<CommandSourceStack, T> value) {
        super(context, value);
    }

    public static Value of(CarpetContext context, RequiredArgumentBuilder<CommandSourceStack, ?> value) {
        return value != null ? new RequiredArgumentBuilderValue<>(context, value) : Value.NULL;
    }

    public static RequiredArgumentBuilder<CommandSourceStack, ?> from(CarpetContext context, Value value) {
        return switch (value) {
            case null -> null;
            case NullValue ignored -> null;
            case RequiredArgumentBuilderValue<?> v -> v.value;
            default -> throw new InternalExpressionException("Cannot convert " + value.getTypeString() + " to required_argument_builder");
        };
    }

    @Override
    public Value get(String what, Value... more) {
        return switch (what) {
            case "suggests" -> {
                value.suggests(SuggestionProviderValue.from(context, more[0]));
                yield this;
            }
            case "suggestion_provider" -> SuggestionProviderValue.of(context, value.getSuggestionsProvider());
            case "type" -> ArgumentTypeValue.of(context, value.getType());
            case "name" -> StringValue.of(value.getName());
            case "build" -> ArgumentCommandNodeValue.of(context, value.build());
            default -> super.get(what, more);
        };
    }

    @Override
    public String getTypeString() {
        return "required_argument_builder";
    }
}

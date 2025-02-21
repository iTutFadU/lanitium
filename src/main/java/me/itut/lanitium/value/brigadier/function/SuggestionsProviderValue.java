package me.itut.lanitium.value.brigadier.function;

import carpet.script.CarpetContext;
import carpet.script.Context;
import carpet.script.exception.InternalExpressionException;
import carpet.script.exception.ProcessedThrowStatement;
import carpet.script.value.FunctionValue;
import carpet.script.value.NullValue;
import carpet.script.value.Value;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import me.itut.lanitium.value.ObjectFunctionValue;
import me.itut.lanitium.value.brigadier.CommandSyntaxError;
import me.itut.lanitium.value.brigadier.context.CommandContextValue;
import me.itut.lanitium.value.brigadier.suggestion.SuggestionsBuilderValue;
import me.itut.lanitium.value.brigadier.suggestion.SuggestionsFuture;
import net.minecraft.commands.CommandSourceStack;

import java.util.List;

public class SuggestionsProviderValue extends ObjectFunctionValue<SuggestionProvider<CommandSourceStack>> {
    protected SuggestionsProviderValue(CarpetContext context, SuggestionProvider<CommandSourceStack> value) {
        super(context, value, (c, t) -> {
            try {
                return SuggestionsFuture.of(context, value.getSuggestions(CommandContextValue.from(c.getVariable("c").evalValue(c, t)), SuggestionsBuilderValue.from(c.getVariable("b").evalValue(c, t))));
            } catch (CommandSyntaxException e) {
                throw CommandSyntaxError.create(context, e);
            }
        }, List.of("c", "b"), null);
    }

    public static Value of(CarpetContext context, SuggestionProvider<CommandSourceStack> value) {
        return value != null ? new SuggestionsProviderValue(context, value) : Value.NULL;
    }

    public static SuggestionProvider<CommandSourceStack> from(CarpetContext context, Value value) {
        return switch (value) {
            case null -> null;
            case NullValue ignored -> null;
            case SuggestionsProviderValue v -> v.value;
            case FunctionValue fn -> (ctx, builder) -> {
                try {
                    CarpetContext copy = (CarpetContext)context.recreate();
                    copy.variables = context.variables;
                    copy.swapSource(ctx.getSource());

                    return SuggestionsFuture.from(fn.callInContext(copy, Context.NONE, List.of(CommandContextValue.of(copy, ctx), SuggestionsBuilderValue.of(copy, builder))).evalValue(copy));
                } catch (ProcessedThrowStatement e) {
                    if (e.thrownExceptionType == CommandSyntaxError.COMMAND_SYNTAX_ERROR && e.data instanceof CommandSyntaxError err) throw err.value;
                    throw e;
                }
            };
            default -> throw new InternalExpressionException("Cannot convert " + value.getTypeString() + " to suggestions_provider");
        };
    }

    @Override
    public String getTypeString() {
        return "suggestions_provider";
    }
}

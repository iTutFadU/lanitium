package me.itut.lanitium.value.brigadier;

import carpet.script.CarpetContext;
import carpet.script.Context;
import carpet.script.exception.InternalExpressionException;
import carpet.script.exception.ProcessedThrowStatement;
import carpet.script.value.FunctionValue;
import carpet.script.value.NullValue;
import carpet.script.value.Value;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import me.itut.lanitium.value.SimpleFunctionValue;
import net.minecraft.commands.CommandSourceStack;

import java.util.List;

public class SuggestionProviderValue extends SimpleFunctionValue {
    public final CarpetContext context;
    public final SuggestionProvider<CommandSourceStack> value;

    protected SuggestionProviderValue(CarpetContext context, SuggestionProvider<CommandSourceStack> value) {
        super((c, t) -> {
            try {
                return SuggestionsFuture.of(context, value.getSuggestions(CommandContextValue.from((CarpetContext)c, c.getVariable("c").evalValue(c, t)), SuggestionsBuilderValue.from((CarpetContext)c, c.getVariable("b").evalValue(c, t))));
            } catch (CommandSyntaxException e) {
                throw CommandSyntaxError.create(context, e);
            }
        }, List.of("c", "b"), null);
        this.context = context;
        this.value = value;
    }

    public static Value of(CarpetContext context, SuggestionProvider<CommandSourceStack> value) {
        return value != null ? new SuggestionProviderValue(context, value) : Value.NULL;
    }

    public static SuggestionProvider<CommandSourceStack> from(CarpetContext context, Value value) {
        return switch (value) {
            case null -> null;
            case NullValue ignored -> null;
            case SuggestionProviderValue v -> v.value;
            case FunctionValue fn -> (ctx, builder) -> {
                try {
                    return SuggestionsFuture.from(context, fn.callInContext(context, Context.NONE, List.of(CommandContextValue.of(context, ctx), SuggestionsBuilderValue.of(context, builder))).evalValue(context));
                } catch (ProcessedThrowStatement e) {
                    if (e.thrownExceptionType == CommandSyntaxError.COMMAND_SYNTAX_ERROR && e.data instanceof CommandSyntaxError err) throw err.value;
                    throw e;
                }
            };
            default -> throw new InternalExpressionException("Cannot convert " + value.getTypeString() + " to suggestion_provider");
        };
    }

    @Override
    public String getTypeString() {
        return "suggestion_provider";
    }
}

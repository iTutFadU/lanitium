package me.itut.lanitium.value.brigadier.function;

import carpet.script.CarpetContext;
import carpet.script.Context;
import carpet.script.exception.InternalExpressionException;
import carpet.script.exception.ProcessedThrowStatement;
import carpet.script.value.FunctionValue;
import carpet.script.value.NullValue;
import carpet.script.value.Value;
import com.mojang.brigadier.SingleRedirectModifier;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.itut.lanitium.value.ContextValue;
import me.itut.lanitium.value.ObjectFunctionValue;
import me.itut.lanitium.value.Util;
import me.itut.lanitium.value.brigadier.CommandSyntaxError;
import me.itut.lanitium.value.brigadier.context.CommandContextValue;
import net.minecraft.commands.CommandSourceStack;

import java.util.List;

public class SingleRedirectModifierValue extends ObjectFunctionValue<SingleRedirectModifier<CommandSourceStack>> {
    protected SingleRedirectModifierValue(CarpetContext context, SingleRedirectModifier<CommandSourceStack> value) {
        super(context, value, (c, t) -> {
            try {
                return Util.source((CarpetContext)c, value.apply(CommandContextValue.from(c.getVariable("c").evalValue(c, t))));
            } catch (CommandSyntaxException e) {
                throw CommandSyntaxError.create(context, e);
            }
        }, List.of("c"), null);
    }

    public static Value of(CarpetContext context, SingleRedirectModifier<CommandSourceStack> value) {
        return value != null ? new SingleRedirectModifierValue(context, value) : Value.NULL;
    }

    public static SingleRedirectModifier<CommandSourceStack> from(CarpetContext context, Value value) {
        return switch (value) {
            case null -> null;
            case NullValue ignored -> null;
            case SingleRedirectModifierValue v -> v.value;
            case FunctionValue fn -> ctx -> {
                try {
                    return ContextValue.from(fn.callInContext(context, Context.NONE, List.of(CommandContextValue.of(context, ctx))).evalValue(context)).source();
                } catch (ProcessedThrowStatement e) {
                    if (e.thrownExceptionType == CommandSyntaxError.COMMAND_SYNTAX_ERROR && e.data instanceof CommandSyntaxError err) throw err.value;
                    throw e;
                }
            };
            default -> throw new InternalExpressionException("Cannot convert " + value.getTypeString() + " to single_redirect_modifier");
        };
    }

    @Override
    public String getTypeString() {
        return "single_redirect_modifier";
    }
}

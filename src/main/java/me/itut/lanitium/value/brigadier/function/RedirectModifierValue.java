package me.itut.lanitium.value.brigadier.function;

import carpet.script.CarpetContext;
import carpet.script.Context;
import carpet.script.exception.InternalExpressionException;
import carpet.script.exception.ProcessedThrowStatement;
import carpet.script.value.FunctionValue;
import carpet.script.value.ListValue;
import carpet.script.value.NullValue;
import carpet.script.value.Value;
import com.mojang.brigadier.RedirectModifier;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.itut.lanitium.value.ContextValue;
import me.itut.lanitium.value.ObjectFunctionValue;
import me.itut.lanitium.value.ValueConversions;
import me.itut.lanitium.value.brigadier.CommandSyntaxError;
import me.itut.lanitium.value.brigadier.context.CommandContextValue;
import net.minecraft.commands.CommandSourceStack;

import java.util.List;

public class RedirectModifierValue extends ObjectFunctionValue<RedirectModifier<CommandSourceStack>> {
    protected RedirectModifierValue(CarpetContext context, RedirectModifier<CommandSourceStack> value) {
        super(context, value, (c, t) -> {
            try {
                return ListValue.wrap(value.apply(CommandContextValue.from(c.getVariable("c").evalValue(c, t))).stream().map(v -> ValueConversions.source((CarpetContext)c, v)));
            } catch (CommandSyntaxException e) {
                throw CommandSyntaxError.create(context, e);
            }
        }, List.of("c"), null);
    }

    public static Value of(CarpetContext context, RedirectModifier<CommandSourceStack> value) {
        return value != null ? new RedirectModifierValue(context, value) : Value.NULL;
    }

    public static RedirectModifier<CommandSourceStack> from(CarpetContext context, Value value) {
        return switch (value) {
            case null -> null;
            case NullValue ignored -> null;
            case RedirectModifierValue v -> v.value;
            case FunctionValue fn -> ctx -> {
                try {
                    CarpetContext copy = (CarpetContext)context.recreate();
                    copy.variables = context.variables;
                    copy.swapSource(ctx.getSource());

                    return ValueConversions.listFrom(fn.callInContext(copy, Context.LIST, List.of(CommandContextValue.of(copy, ctx))).evalValue(copy, Context.LIST)).stream().map(v -> ContextValue.from(v).source()).toList();
                } catch (ProcessedThrowStatement e) {
                    if (e.thrownExceptionType == CommandSyntaxError.COMMAND_SYNTAX_ERROR && e.data instanceof CommandSyntaxError err) throw err.value;
                    throw e;
                }
            };
            default -> throw new InternalExpressionException("Cannot convert " + value.getTypeString() + " to redirect_modifier");
        };
    }

    @Override
    public String getTypeString() {
        return "redirect_modifier";
    }
}

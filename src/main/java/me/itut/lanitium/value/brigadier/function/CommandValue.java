package me.itut.lanitium.value.brigadier.function;

import carpet.script.CarpetContext;
import carpet.script.Context;
import carpet.script.exception.InternalExpressionException;
import carpet.script.exception.ProcessedThrowStatement;
import carpet.script.value.*;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.itut.lanitium.value.ObjectFunctionValue;
import me.itut.lanitium.value.brigadier.CommandSyntaxError;
import me.itut.lanitium.value.brigadier.context.CommandContextValue;
import net.minecraft.commands.CommandSourceStack;

import java.util.List;

public class CommandValue extends ObjectFunctionValue<Command<CommandSourceStack>> {
    protected CommandValue(CarpetContext context, Command<CommandSourceStack> value) {
        super(context, value, (c, t) -> {
            try {
                return NumericValue.of(value.run(CommandContextValue.from(c.getVariable("c").evalValue(c, t))));
            } catch (CommandSyntaxException e) {
                throw CommandSyntaxError.create(context, e);
            }
        }, List.of("c"), null);
    }

    public static Value of(CarpetContext context, Command<CommandSourceStack> value) {
        return value != null ? new CommandValue(context, value) : Value.NULL;
    }

    public static Command<CommandSourceStack> from(CarpetContext context, Value value) {
        return switch (value) {
            case null -> null;
            case NullValue ignored -> null;
            case CommandValue v -> v.value;
            case FunctionValue fn -> ctx -> {
                try {
                    return NumericValue.asNumber(fn.callInContext(context, Context.NUMBER, List.of(CommandContextValue.of(context, ctx))).evalValue(context, Context.NUMBER)).getInt();
                } catch (ProcessedThrowStatement e) {
                    if (e.thrownExceptionType == CommandSyntaxError.COMMAND_SYNTAX_ERROR && e.data instanceof CommandSyntaxError err) throw err.value;
                    throw e;
                }
            };
            default -> throw new InternalExpressionException("Cannot convert " + value.getTypeString() + " to command");
        };
    }

    @Override
    public String getTypeString() {
        return "command";
    }
}

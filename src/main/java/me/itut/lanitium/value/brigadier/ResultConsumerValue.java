package me.itut.lanitium.value.brigadier;

import carpet.script.CarpetContext;
import carpet.script.Context;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.*;
import com.mojang.brigadier.ResultConsumer;
import me.itut.lanitium.value.SimpleFunctionValue;
import net.minecraft.commands.CommandSourceStack;

import java.util.List;

public class ResultConsumerValue extends SimpleFunctionValue {
    public final CarpetContext context;
    public final ResultConsumer<CommandSourceStack> value;

    protected ResultConsumerValue(CarpetContext context, ResultConsumer<CommandSourceStack> value) {
        super((c, t) -> {
            value.onCommandComplete(CommandContextValue.from((CarpetContext)c, c.getVariable("c").evalValue(c, t)), c.getVariable("s").evalValue(c, t).getBoolean(), NumericValue.asNumber(c.getVariable("r").evalValue(c, t)).getInt());
            return Value.NULL;
        }, List.of("c", "s", "r"), null);
        this.context = context;
        this.value = value;
    }

    public static Value of(CarpetContext context, ResultConsumer<CommandSourceStack> value) {
        return value != null ? new ResultConsumerValue(context, value) : Value.NULL;
    }

    public static ResultConsumer<CommandSourceStack> from(CarpetContext context, Value value) {
        return switch (value) {
            case null -> null;
            case NullValue ignored -> null;
            case ResultConsumerValue v -> v.value;
            case FunctionValue fn -> (ctx, success, result) -> fn.callInContext(context, Context.VOID, List.of(CommandContextValue.of(context, ctx), BooleanValue.of(success), NumericValue.of(result))).evalValue(context, Context.VOID);
            default -> throw new InternalExpressionException("Cannot convert " + value.getTypeString() + " to result_consumer");
        };
    }

    @Override
    public String getTypeString() {
        return "result_consumer";
    }
}

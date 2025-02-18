package me.itut.lanitium.value.brigadier;

import carpet.script.CarpetContext;
import carpet.script.Context;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.*;
import com.mojang.brigadier.AmbiguityConsumer;
import me.itut.lanitium.value.ObjectFunctionValue;
import me.itut.lanitium.value.Util;
import net.minecraft.commands.CommandSourceStack;

import java.util.List;

public class AmbiguityConsumerValue extends ObjectFunctionValue<AmbiguityConsumer<CommandSourceStack>> {
    protected AmbiguityConsumerValue(CarpetContext context, AmbiguityConsumer<CommandSourceStack> value) {
        super(context, value, (c, t) -> {
            value.ambiguous(CommandNodeValue.from(c.getVariable("p").evalValue(c, t)), CommandNodeValue.from(c.getVariable("c").evalValue(c, t)), CommandNodeValue.from(c.getVariable("s").evalValue(c, t)), Util.listFrom(c.getVariable("i").evalValue(c, t)).stream().map(Value::getString).toList());
            return Value.NULL;
        }, List.of("p", "c", "s", "i"), null);
    }

    public static Value of(CarpetContext context, AmbiguityConsumer<CommandSourceStack> value) {
        return value != null ? new AmbiguityConsumerValue(context, value) : Value.NULL;
    }

    public static AmbiguityConsumer<CommandSourceStack> from(CarpetContext context, Value value) {
        return switch (value) {
            case null -> null;
            case NullValue ignored -> null;
            case AmbiguityConsumerValue v -> v.value;
            case FunctionValue fn -> (parent, child, sibling, inputs) -> fn.callInContext(context, Context.VOID, List.of(CommandNodeValue.of(context, parent), CommandNodeValue.of(context, child), CommandNodeValue.of(context, sibling), ListValue.wrap(inputs.stream().map(StringValue::of)))).evalValue(context, Context.VOID);
            default -> throw new InternalExpressionException("Cannot convert " + value.getTypeString() + " to ambiguity_consumer");
        };
    }

    @Override
    public String getTypeString() {
        return "ambiguity_consumer";
    }
}

package me.itut.lanitium.value.brigadier.function;

import carpet.script.CarpetContext;
import carpet.script.Context;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.BooleanValue;
import carpet.script.value.FunctionValue;
import carpet.script.value.NullValue;
import carpet.script.value.Value;
import me.itut.lanitium.value.ObjectFunctionValue;
import net.minecraft.commands.CommandSourceStack;

import java.util.List;
import java.util.function.Predicate;

public class RequirementValue extends ObjectFunctionValue<Predicate<CommandSourceStack>> {
    protected RequirementValue(CarpetContext context, Predicate<CommandSourceStack> value) {
        super(context, value, (c, t) -> BooleanValue.of(value.test(((CarpetContext)c).source())), List.of(), null);
    }

    public static Value of(CarpetContext context, Predicate<CommandSourceStack> value) {
        return value != null ? new RequirementValue(context, value) : Value.NULL;
    }

    public static Predicate<CommandSourceStack> from(CarpetContext context, Value value) {
        return switch (value) {
            case null -> null;
            case NullValue ignored -> null;
            case RequirementValue v -> v.value;
            case FunctionValue fn -> s -> {
                CarpetContext copy = context.duplicate();
                copy.swapSource(s);
                return fn.callInContext(copy, Context.BOOLEAN, List.of()).evalValue(copy, Context.BOOLEAN).getBoolean();
            };
            default -> throw new InternalExpressionException("Cannot convert " + value.getTypeString() + " to requirement");
        };
    }

    @Override
    public String getTypeString() {
        return "requirement";
    }
}

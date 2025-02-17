package me.itut.lanitium.value.brigadier;

import carpet.script.CarpetContext;
import carpet.script.exception.ThrowStatement;
import carpet.script.exception.Throwables;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.itut.lanitium.value.ObjectValue;
import org.jetbrains.annotations.CheckReturnValue;

import static carpet.script.exception.Throwables.THROWN_EXCEPTION_TYPE;
import static carpet.script.exception.Throwables.register;

public class CommandSyntaxError extends ObjectValue<CommandSyntaxException> {
    public static final Throwables COMMAND_SYNTAX_ERROR = register("command_syntax_error", THROWN_EXCEPTION_TYPE);

    public CommandSyntaxError(CarpetContext context, CommandSyntaxException value) {
        super(context, value);
    }

    @CheckReturnValue
    public static ThrowStatement create(CarpetContext context, CommandSyntaxException value) {
        return new ThrowStatement(new CommandSyntaxError(context, value), COMMAND_SYNTAX_ERROR);
    }

    @Override
    public Value get(String what, Value... more) {
        return switch (what) {
            case "message" -> checkArguments(what, more, 0, () -> StringValue.of(value.getMessage()));
            case "raw_message" -> checkArguments(what, more, 0, () -> StringValue.of(value.getRawMessage().getString()));
            case "context" -> checkArguments(what, more, 0, () -> StringValue.of(value.getContext()));
            case "input" -> checkArguments(what, more, 0, () -> StringValue.of(value.getInput()));
            case "cursor" -> checkArguments(what, more, 0, () -> NumericValue.of(value.getCursor()));
            default -> unknownFeature(what);
        };
    }

    @Override
    public String getTypeString() {
        return "command_syntax_error";
    }
}

package me.itut.lanitium.internal.carpet;

import carpet.script.exception.ProcessedThrowStatement;
import carpet.script.value.AbstractListValue;
import carpet.script.value.FormattedTextValue;
import carpet.script.value.Value;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import me.itut.lanitium.function.Apply;
import me.itut.lanitium.value.parsing.StringReaderValue;
import net.minecraft.network.chat.Component;

import java.util.List;

public class CommandExpressionException extends RuntimeException {
    private final CommandSyntaxException exception;

    private CommandExpressionException(String message, CommandSyntaxException exception) {
        super(message);
        this.exception = exception;
    }

    public static RuntimeException of(ProcessedThrowStatement e) {
        if (!e.thrownExceptionType.isRelevantFor(Apply.COMMAND_SYNTAX_EXCEPTION.getId())
         || !(e.data instanceof AbstractListValue list)) return e;

        List<Value> values = list.unpack();
        if (values.size() < 2) return e;

        Value msg = values.get(1);
        Message message = msg instanceof FormattedTextValue fmt
            ? fmt.getText()
            : Component.literal(msg.getString());
        SimpleCommandExceptionType type = new SimpleCommandExceptionType(message);

        CommandSyntaxException exception = values.getFirst() instanceof StringReaderValue reader
            ? type.createWithContext(reader.value)
            : type.create();
        return new CommandExpressionException(message.getString(), exception);
    }

    public CommandSyntaxException exception() {
        return exception;
    }
}

package me.itut.lanitium.value;

import carpet.script.CarpetContext;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.NullValue;
import carpet.script.value.Value;
import net.minecraft.commands.CommandSourceStack;

public class SourceValue extends ObjectValue<CommandSourceStack> {
    protected SourceValue(CarpetContext c) {
        super(c.source());
    }

    public static SourceValue of(CarpetContext c) {
        return new SourceValue(c);
    }

    public static CommandSourceStack from(Value value) {
        return switch (value) {
            case null -> null;
            case NullValue ignored -> null;
            case SourceValue source -> source.value;
            default -> throw new InternalExpressionException("Cannot convert " + value.getTypeString() + " to source");
        };
    }

    @Override
    public Value get(String what, Value... more) {
        return unknownFeature(what); // opaque for now
        // So not source~'entity', but with_source(source, system_info('source_entity'))
    }

    @Override
    public String getTypeString() {
        return "source";
    }
}

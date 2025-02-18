package me.itut.lanitium.value.brigadier;

import carpet.script.CarpetContext;
import carpet.script.value.NullValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;

public class LiteralArgumentBuilderValue extends ArgumentBuilderValue<LiteralArgumentBuilder<CommandSourceStack>> {
    protected LiteralArgumentBuilderValue(CarpetContext context, LiteralArgumentBuilder<CommandSourceStack> value) {
        super(context, value);
    }

    public static Value of(CarpetContext context, LiteralArgumentBuilder<CommandSourceStack> value) {
        return value != null ? new LiteralArgumentBuilderValue(context, value) : Value.NULL;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> from(Value value) {
        return switch (value) {
            case null -> null;
            case NullValue ignored -> null;
            case LiteralArgumentBuilderValue v -> v.value;
            default -> LiteralArgumentBuilder.literal(value.getString());
        };
    }

    @Override
    public Value get(String what, Value... more) {
        return switch (what) {
            case "literal" -> {
                checkArguments(what, more, 0);
                yield StringValue.of(value.getLiteral());
            }
            default -> super.get(what, more);
        };
    }

    @Override
    public String getTypeString() {
        return "literal_argument_builder";
    }
}

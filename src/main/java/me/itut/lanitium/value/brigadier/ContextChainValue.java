package me.itut.lanitium.value.brigadier;

import carpet.script.CarpetContext;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.NullValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.itut.lanitium.value.ContextValue;
import me.itut.lanitium.value.ObjectValue;
import net.minecraft.commands.CommandSourceStack;

public class ContextChainValue extends ObjectValue<ContextChain<CommandSourceStack>> {
    protected ContextChainValue(CarpetContext context, ContextChain<CommandSourceStack> value) {
        super(context, value);
    }

    public static Value of(CarpetContext context, ContextChain<CommandSourceStack> value) {
        return value != null ? new ContextChainValue(context, value) : Value.NULL;
    }

    public static ContextChain<CommandSourceStack> from(Value value) {
        return switch (value) {
            case null -> null;
            case NullValue ignored -> null;
            case ContextChainValue v -> v.value;
            default -> throw new InternalExpressionException("Cannot convert " + value.getTypeString() + " to context_chain");
        };
    }

    private static final Value MODIFY = StringValue.of("modify"), EXECUTE = StringValue.of("execute");

    @Override
    public Value get(String what, Value... more) {
        return switch (what) {
            case "execute_all" -> {
                checkArguments(what, more, 2);
                try {
                    yield NumericValue.of(value.executeAll(ContextValue.from(more[0]).source(), ResultConsumerValue.from(context, more[1])));
                } catch (CommandSyntaxException e) {
                    throw CommandSyntaxError.create(context, e);
                }
            }
            case "stage" -> {
                checkArguments(what, more, 0);
                yield switch (value.getStage()) {
                    case MODIFY -> MODIFY;
                    case EXECUTE -> EXECUTE;
                };
            }
            case "top_context" -> {
                checkArguments(what, more, 0);
                yield CommandContextValue.of(context, value.getTopContext());
            }
            case "next_stage" -> {
                checkArguments(what, more, 0);
                yield of(context, value.nextStage());
            }
            default -> unknownFeature(what);
        };
    }

    @Override
    public String getTypeString() {
        return "context_chain";
    }
}

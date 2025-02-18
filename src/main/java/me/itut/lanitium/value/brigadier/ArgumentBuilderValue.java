package me.itut.lanitium.value.brigadier;

import carpet.script.CarpetContext;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.BooleanValue;
import carpet.script.value.ListValue;
import carpet.script.value.NullValue;
import carpet.script.value.Value;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import me.itut.lanitium.value.ObjectValue;
import net.minecraft.commands.CommandSourceStack;

public abstract class ArgumentBuilderValue<T extends ArgumentBuilder<CommandSourceStack, T>> extends ObjectValue<T> {
    protected ArgumentBuilderValue(CarpetContext context, T value) {
        super(context, value);
    }

    @SuppressWarnings("unchecked")
    public static Value of(CarpetContext context, ArgumentBuilder<CommandSourceStack, ?> value) {
        return switch (value) {
            case null -> null;
            case LiteralArgumentBuilder<?> literal -> LiteralArgumentBuilderValue.of(context, (LiteralArgumentBuilder<CommandSourceStack>)literal);
            case RequiredArgumentBuilder<?, ?> argument -> RequiredArgumentBuilderValue.of(context, (RequiredArgumentBuilder<CommandSourceStack, ?>)argument);
            default -> throw new InternalExpressionException("Unknown argument builder class: " + value.getClass().getSimpleName() + " (how did you get here?)");
        };
    }

    public static ArgumentBuilder<CommandSourceStack, ?> from(Value value) {
        return switch (value) {
            case null -> null;
            case NullValue ignored -> null;
            case ArgumentBuilderValue<?> v -> v.value;
            default -> LiteralArgumentBuilder.literal(value.getString());
        };
    }

    @Override
    public Value get(String what, Value... more) {
        return switch (what) {
            case "then" -> {
                checkArguments(what, more, 1);
                if (more[0] instanceof ArgumentBuilderValue<?> builder) value.then(builder.value);
                else value.then(CommandNodeValue.from(more[0]));
                yield this;
            }
            case "arguments" -> {
                checkArguments(what, more, 0);
                yield ListValue.wrap(value.getArguments().stream().map(v -> CommandNodeValue.of(context, v)));
            }
            case "executes" -> {
                checkArguments(what, more, 1);
                value.executes(CommandValue.from(context, more[0]));
                yield this;
            }
            case "command" -> {
                checkArguments(what, more, 0);
                yield CommandValue.of(context, value.getCommand());
            }
            case "requires" -> {
                checkArguments(what, more, 1);
                value.requires(RequirementValue.from(context, more[0]));
                yield this;
            }
            case "requirement" -> {
                checkArguments(what, more, 0);
                yield RequirementValue.of(context, value.getRequirement());
            }
            case "redirect" -> {
                checkArguments(what, more, 1, 2);
                if (more.length > 1) value.redirect(CommandNodeValue.from(more[0]), SingleRedirectModifierValue.from(context, more[1]));
                else value.redirect(CommandNodeValue.from(more[0]));
                yield this;
            }
            case "fork" -> {
                checkArguments(what, more, 2);
                value.fork(CommandNodeValue.from(more[0]), RedirectModifierValue.from(context, more[1]));
                yield this;
            }
            case "forward" -> {
                checkArguments(what, more, 3);
                value.forward(CommandNodeValue.from(more[0]), RedirectModifierValue.from(context, more[1]), more[2].getBoolean());
                yield this;
            }
            case "target" -> {
                checkArguments(what, more, 0);
                yield CommandNodeValue.of(context, value.getRedirect());
            }
            case "redirect_modifier" -> {
                checkArguments(what, more, 0);
                yield RedirectModifierValue.of(context, value.getRedirectModifier());
            }
            case "forks" -> {
                checkArguments(what, more, 0);
                yield BooleanValue.of(value.isFork());
            }
            case "build" -> {
                checkArguments(what, more, 0);
                yield CommandNodeValue.of(context, value.build());
            }
            default -> unknownFeature(what);
        };
    }
}

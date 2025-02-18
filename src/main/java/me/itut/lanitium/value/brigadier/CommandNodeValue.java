package me.itut.lanitium.value.brigadier;

import carpet.fakes.CommandNodeInterface;
import carpet.script.CarpetContext;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.*;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import me.itut.lanitium.value.ContextValue;
import me.itut.lanitium.value.ObjectValue;
import net.minecraft.commands.CommandSourceStack;

public abstract class CommandNodeValue extends ObjectValue<CommandNode<CommandSourceStack>> {
    protected CommandNodeValue(CarpetContext context, CommandNode<CommandSourceStack> value) {
        super(context, value);
    }

    public static Value of(CarpetContext context, CommandNode<CommandSourceStack> value) {
        return switch (value) {
            case null -> Value.NULL;
            case LiteralCommandNode<CommandSourceStack> literal -> new LiteralCommandNodeValue(context, literal);
            case ArgumentCommandNode<CommandSourceStack, ?> argument -> new ArgumentCommandNodeValue<>(context, argument);
            case RootCommandNode<CommandSourceStack> root -> new RootCommandNodeValue(context, root);
            default -> throw new InternalExpressionException("Unknown command node class: " + value.getClass().getSimpleName() + " (how did you get there?)");
        };
    }

    public static CommandNode<CommandSourceStack> from(Value value) {
        return switch (value) {
            case null -> null;
            case NullValue ignored -> null;
            case CommandNodeValue v -> v.value;
            default -> throw new InternalExpressionException("Cannot convert " + value.getTypeString() + " to command_node");
        };
    }

    @Override
    public Value get(String what, Value... more) {
        return switch (what) {
            case "command" -> {
                checkArguments(what, more, 0);
                yield CommandValue.of(context, value.getCommand());
            }
            case "children" -> {
                checkArguments(what, more, 0);
                yield ListValue.wrap(value.getChildren().stream().map(v -> CommandNodeValue.of(context, v)));
            }
            case "child" -> {
                checkArguments(what, more, 1);
                yield of(context, value.getChild(more[0].getString()));
            }
            case "target" -> {
                checkArguments(what, more, 0);
                yield of(context, value.getRedirect());
            }
            case "redirect_modifier" -> {
                checkArguments(what, more, 0);
                yield RedirectModifierValue.of(context, value.getRedirectModifier());
            }
            case "can_use" -> {
                checkArguments(what, more, 1);
                yield BooleanValue.of(value.canUse(ContextValue.fromOrCurrent(context, more[0]).source()));
            }
            case "add_child" -> {
                checkArguments(what, more, 1);
                value.addChild(CommandNodeValue.from(more[0]));
                yield this;
            }
            case "find_ambiguities" -> {
                checkArguments(what, more, 1);
                value.findAmbiguities(AmbiguityConsumerValue.from(context, more[0]));
                yield Value.NULL;
            }
            case "requirement" -> {
                checkArguments(what, more, 0);
                yield RequirementValue.of(context, value.getRequirement());
            }
            case "name" -> {
                checkArguments(what, more, 0);
                yield StringValue.of(value.getName());
            }
            case "usage_text" -> {
                checkArguments(what, more, 0);
                yield StringValue.of(value.getUsageText());
            }
            case "parse" -> {
                checkArguments(what, more, 2);
                try {
                    value.parse(new StringReader(more[0].getString()), CommandContextBuilderValue.from(more[1]));
                    yield Value.NULL;
                } catch (CommandSyntaxException e) {
                    throw CommandSyntaxError.create(context, e);
                }
            }
            case "list_suggestions" -> {
                checkArguments(what, more, 2);
                try {
                    yield SuggestionsFuture.of(context, value.listSuggestions(CommandContextValue.from(more[0]), SuggestionsBuilderValue.from(more[1])));
                } catch (CommandSyntaxException e) {
                    throw CommandSyntaxError.create(context, e);
                }
            }
            case "create_builder" -> {
                checkArguments(what, more, 0);
                yield ArgumentBuilderValue.of(context, value.createBuilder());
            }
            case "remove_child" -> {
                checkArguments(what, more, 1);
                ((CommandNodeInterface)value).carpet$removeChild(more[0].getString());
                yield this;
            }
            default -> unknownFeature(what);
        };
    }

    @Override
    public String getTypeString() {
        return "command_node";
    }
}

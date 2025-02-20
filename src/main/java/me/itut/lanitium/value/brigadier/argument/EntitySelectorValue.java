package me.itut.lanitium.value.brigadier.argument;

import carpet.script.CarpetContext;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.*;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.itut.lanitium.value.ObjectValue;
import me.itut.lanitium.value.brigadier.CommandSyntaxError;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;

import static me.itut.lanitium.internal.carpet.EntityValueSelectorCache.selectorCache;

public class EntitySelectorValue extends ObjectValue<EntitySelector> {
    protected EntitySelectorValue(CarpetContext context, EntitySelector value) {
        super(context, value);
    }

    public static Value of(CarpetContext context, EntitySelector value) {
        return value != null ? new EntitySelectorValue(context, value) : Value.NULL;
    }

    public static EntitySelector from(Value value) {
        return switch (value) {
            case null -> null;
            case NullValue ignored -> null;
            case EntitySelectorValue v -> v.value;
            default -> throw new InternalExpressionException("Cannot convert " + value.getTypeString() + " to entity_selector");
        };
    }

    @Override
    public Value get(String what, Value... more) {
        return switch (what) {
            case "limit" -> {
                checkArguments(what, more, 0);
                yield NumericValue.of(value.getMaxResults());
            }
            case "includes_entities" -> {
                checkArguments(what, more, 0);
                yield BooleanValue.of(value.includesEntities());
            }
            case "self_selector" -> {
                checkArguments(what, more, 0);
                yield BooleanValue.of(value.isSelfSelector());
            }
            case "world_limited" -> {
                checkArguments(what, more, 0);
                yield BooleanValue.of(value.isWorldLimited());
            }
            case "uses_selector" -> {
                checkArguments(what, more, 0);
                yield BooleanValue.of(value.usesSelector());
            }
            case "find_single_entity" -> {
                checkArguments(what, more, 0);
                try {
                    yield EntityValue.of(value.findSingleEntity(context.source()));
                } catch (CommandSyntaxException e) {
                    throw CommandSyntaxError.create(context, e);
                }
            }
            case "find_entities" -> {
                checkArguments(what, more, 0);
                try {
                    yield ListValue.wrap(value.findEntities(context.source()).stream().map(EntityValue::of));
                } catch (CommandSyntaxException e) {
                    throw CommandSyntaxError.create(context, e);
                }
            }
            case "find_single_player" -> {
                checkArguments(what, more, 0);
                try {
                    yield EntityValue.of(value.findSinglePlayer(context.source()));
                } catch (CommandSyntaxException e) {
                    throw CommandSyntaxError.create(context, e);
                }
            }
            case "find_players" -> {
                checkArguments(what, more, 0);
                try {
                    yield ListValue.wrap(value.findPlayers(context.source()).stream().map(EntityValue::of));
                } catch (CommandSyntaxException e) {
                    throw CommandSyntaxError.create(context, e);
                }
            }
            default -> unknownFeature(what);
        };
    }

    @Override
    public String getTypeString() {
        return "entity_selector";
    }
}

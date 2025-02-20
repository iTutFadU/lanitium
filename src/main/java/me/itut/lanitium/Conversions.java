package me.itut.lanitium;

import carpet.script.CarpetContext;
import carpet.script.exception.InternalExpressionException;
import carpet.script.external.Vanilla;
import carpet.script.value.*;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.itut.lanitium.value.SimpleFunctionValue;
import me.itut.lanitium.value.Util;
import me.itut.lanitium.value.brigadier.CommandSyntaxError;
import me.itut.lanitium.value.brigadier.argument.EntitySelectorValue;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.commands.arguments.*;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.commands.arguments.blocks.BlockPredicateArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.item.FunctionArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.ScoreAccess;

import java.util.*;
import java.util.function.Predicate;

public class Conversions {
    public static Value from(final CarpetContext c, final Object object) throws InternalExpressionException {
        return switch (object) {
            case null -> Value.NULL;
            case Value value -> value;
            case Collection<?> list -> ListValue.wrap(list.stream().map(v -> from(c, v)));
            case Map<?, ?> map -> {
                Map<Value, Value> converted = new HashMap<>();
                map.forEach((k, v) -> converted.put(from(c, k), from(c, v)));
                yield MapValue.wrap(converted);
            }
            case String string -> StringValue.of(string);
            case Character character -> StringValue.of(character.toString());
            case Number number -> NumericValue.of(number);
            case Boolean bool -> BooleanValue.of(bool);
            case ResourceLocation id -> StringValue.of(id.toString());
            case Component component -> FormattedTextValue.of(component);
            case Style style -> new SimpleFunctionValue((cc, tt) -> FormattedTextValue.of(FormattedTextValue.getTextByValue(cc.getVariable("c").evalValue(cc, tt)).copy().setStyle(style)), List.of("c"), null);
            case ChatFormatting formatting -> StringValue.of(formatting.getName());
            case Tag tag -> NBTSerializableValue.of(tag);
            case AngleArgument.SingleAngle angle -> NumericValue.of(angle.getAngle(c.source()));
            case EntityAnchorArgument.Anchor anchor -> StringValue.of(anchor == EntityAnchorArgument.Anchor.EYES ? "eyes" : "feet");
            case StringRepresentable string -> StringValue.of(string.getSerializedName().toLowerCase());
            case Entity entity -> EntityValue.of(entity);
            case EntitySelector selector -> EntitySelectorValue.of(c, selector);
            case GameProfileArgument.Result result -> {
                try {
                    yield ListValue.wrap(result.getNames(c.source()).stream().map(v -> StringValue.of(v.getName())));
                } catch (CommandSyntaxException e) {
                    throw CommandSyntaxError.create(c, e);
                }
            }
            case MessageArgument.Message message -> {
                try {
                    yield FormattedTextValue.of(message.toComponent(c.source(), EntitySelectorParser.allowSelectors(c.source())));
                } catch (CommandSyntaxException e) {
                    throw CommandSyntaxError.create(c, e);
                }
            }
            case NbtPathArgument.NbtPath path -> StringValue.of(path.toString());
            case OperationArgument.Operation operation -> {
                final boolean[] swap = new boolean[1];
                final int[] values = {7, 3};
                ScoreAccess access1 = new ScoreAccess() {
                    @Override
                    public int get() {
                        return values[0];
                    }

                    @Override
                    public void set(int i) {
                        values[0] = i;
                    }

                    @Override
                    public boolean locked() {
                        return false;
                    }

                    @Override
                    public void unlock() {}

                    @Override
                    public void lock() {}

                    @Override
                    public Component display() {
                        return null;
                    }

                    @Override
                    public void display(Component component) {}

                    @Override
                    public void numberFormatOverride(NumberFormat numberFormat) {}
                };
                ScoreAccess access2 = new ScoreAccess() {
                    @Override
                    public int get() {
                        return values[1];
                    }

                    @Override
                    public void set(int i) {
                        values[1] = i;
                        swap[0] = true;
                    }

                    @Override
                    public boolean locked() {
                        return false;
                    }

                    @Override
                    public void unlock() {}

                    @Override
                    public void lock() {}

                    @Override
                    public Component display() {
                        return null;
                    }

                    @Override
                    public void display(Component component) {}

                    @Override
                    public void numberFormatOverride(NumberFormat numberFormat) {}
                };
                try {
                    operation.apply(access1, access2);
                } catch (CommandSyntaxException ignored) {} // impossible
                if (swap[0]) yield StringValue.of("><");
                yield StringValue.of(switch (values[0]) {
                    case 7 + 3 -> "+="; // 10
                    case 7 - 3 -> "-="; // 4
                    case 7 * 3 -> "*="; // 21
                    case 7 / 3 -> "/="; // 2
                    case 7 % 3 -> "%="; // 1
                    case 7 -> ">";
                    default -> {
                        values[0] = 0;
                        try {
                            operation.apply(access1, access2);
                        } catch (CommandSyntaxException ignored) {} // impossible
                        yield values[0] == 0 ? "<" : "=";
                    }
                });
            }
            case ParticleOptions particle -> StringValue.of(ParticleTypes.CODEC.encodeStart(c.registryAccess().createSerializationContext(NbtOps.INSTANCE), particle).toString());
            case MinMaxBounds<?> range -> ListValue.of(range.min().map(NumericValue::of).orElse(Value.NULL), range.max().map(NumericValue::of).orElse(Value.NULL));
            case ScoreHolderArgument.Result result -> {
                try {
                    yield ListValue.wrap(result.getNames(c.source(), () -> c.source().getEntity() instanceof Entity entity ? Collections.singletonList(entity) : Collections.emptyList()).stream().map(v -> StringValue.of(v.getScoreboardName())));
                } catch (CommandSyntaxException e) {
                    throw CommandSyntaxError.create(c, e);
                }
            }
            case UUID uuid -> StringValue.of(uuid.toString());
            case BlockInput block -> new BlockValue(block.getState(), c.source().getLevel(), Vanilla.BlockInput_getTag(block));
            case Coordinates location -> {
                Vec3 pos = location.getPosition(c.source());
                Vec2 rot = location.getRotation(c.source());
                yield ListValue.of(NumericValue.of(pos.x), NumericValue.of(pos.y), NumericValue.of(pos.z), NumericValue.of(rot.y), NumericValue.of(rot.x));
            }
            case BlockPredicateArgument.Result predicate -> {
                Vanilla.BlockPredicatePayload payload = Vanilla.BlockPredicatePayload.of(predicate);
                Registry<Block> blocks = c.registryAccess().lookupOrThrow(Registries.BLOCK);
                yield ListValue.of(payload.state() != null ? from(c, blocks.getKey(payload.state().getBlock())) : Value.NULL, payload.tagKey() != null ? from(c, blocks.get(payload.tagKey()).get().key()) : Value.NULL, MapValue.wrap(payload.properties()), NBTSerializableValue.of(payload.tag()));
            }
            case ItemInput item -> {
                try {
                    ItemStack stack = item.createItemStack(1, false);
                    yield from(c, stack);
                } catch (CommandSyntaxException e) {
                    throw CommandSyntaxError.create(c, e);
                }
            }
            case ItemStack stack -> ListValue.of(StringValue.of(stack.getItem().toString()), NumericValue.of(stack.getCount()), NBTSerializableValue.fromStack(stack, c.registryAccess()));
            default -> ValueConversions.guess(c.level(), object);
        };
    }
}

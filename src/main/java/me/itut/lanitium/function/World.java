package me.itut.lanitium.function;

import carpet.script.CarpetContext;
import carpet.script.CarpetScriptServer;
import carpet.script.Context;
import carpet.script.annotation.Locator;
import carpet.script.annotation.ScarpetFunction;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.*;
import com.mojang.brigadier.StringReader;
import me.itut.lanitium.value.CollisionContextValue;
import me.itut.lanitium.value.VoxelShapeValue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.*;
import java.util.stream.Stream;

public class World {
    private static <T extends Comparable<T>> BlockState setProperty(Property<T> property, String name, String value, BlockState bs) {
        Optional<T> optional = property.getValue(value);
        if (optional.isEmpty())
            throw new InternalExpressionException(value + " is not a valid value for property " + name);
        return bs.setValue(property, optional.get());
    }

    private static int locateUpdateFlags(List<Value> values) {
        if (values.isEmpty()) return Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS;

        int flags = 0;
        for (Value value : values) {
            String str = value.getString();
            if (str.isEmpty() || str.charAt(0) != '+' && str.charAt(0) != '-')
                throw new InternalExpressionException("Block update flag must start with a plus or a minus: " + str);

            int change = switch (str.substring(1)) {
                case "neighbors" -> Block.UPDATE_NEIGHBORS;
                case "clients" -> Block.UPDATE_CLIENTS;
                case "invisible" -> Block.UPDATE_INVISIBLE;
                case "immediate" -> Block.UPDATE_IMMEDIATE;
                case "known_shape" -> Block.UPDATE_KNOWN_SHAPE;
                case "suppress_drops" -> Block.UPDATE_SUPPRESS_DROPS;
                case "move_by_piston" -> Block.UPDATE_MOVE_BY_PISTON;
                case "skip_shape_update_on_wire" -> Block.UPDATE_SKIP_SHAPE_UPDATE_ON_WIRE;
                case "skip_block_entity_side_effects" -> Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS;
                case "skip_on_place" -> Block.UPDATE_SKIP_ON_PLACE;
                case "none" -> Block.UPDATE_NONE;
                case "all" -> Block.UPDATE_ALL;
                case "all_immediate" -> Block.UPDATE_ALL_IMMEDIATE;
                case "skip_all_side_effects" -> Block.UPDATE_SKIP_ALL_SIDEEFFECTS;
                default -> throw new InternalExpressionException("Unknown block update flag: " + str.substring(1));
            };
            if (str.charAt(0) == '+') flags |= change;
            else flags &= ~change;
        }

        return flags;
    }

    @ScarpetFunction(maxParams = -1)
    public Value set_with_updates(Context c, @Locator.Block BlockValue target, @Locator.Block(acceptString = true) BlockValue source, Value... more) {
        CarpetContext cc = (CarpetContext) c;
        ServerLevel world = cc.level();
        BlockState sourceState = source.getBlockState(), targetState = target.getBlockState();
        CompoundTag data = null;
        int offset = 0;

        if (offset < more.length) {
            List<Value> args = null;
            if (more[offset] instanceof MapValue map) {
                args = map.getMap().entrySet().stream().flatMap(e -> Stream.of(e.getKey(), e.getValue())).toList();
                offset++;
            } else if (more[offset] instanceof AbstractListValue list) {
                args = list.unpack();
                offset++;
            } else for (; offset < more.length - 1; offset += 2) {
                Value value = more[offset];
                if (value instanceof NBTSerializableValue) break;
                String str = value.getString();
                if (str.isEmpty() || !StringReader.isAllowedInUnquotedString(str.charAt(0))) break;
                if (args == null) args = new ArrayList<>();
                args.add(value);
                args.add(more[offset + 1]);
            }

            if (args != null) {
                StateDefinition<Block, BlockState> states = sourceState.getBlock().getStateDefinition();
                for (int i = 0; i < args.size() - 1; i += 2) {
                    String name = args.get(i).getString();
                    Property<?> property = states.getProperty(name);
                    if (property == null)
                        throw new InternalExpressionException("Property " + name + " doesn't apply to " + source.getString());
                    String value = args.get(i + 1).getString();
                    sourceState = setProperty(property, name, value, sourceState);
                }
            }
        }

        maybeNbt: if (offset < more.length) {
            Value value = more[offset];
            if (value instanceof NBTSerializableValue nbt) {
                data = nbt.getCompoundTag();
            } else {
                String str = value.getString();
                if (!str.isEmpty() && (str.charAt(0) == '+' || str.charAt(0) == '-')) break maybeNbt;
                data = NBTSerializableValue.parseStringOrFail(str).getCompoundTag();
            }
            offset++;
        }

        int flags = locateUpdateFlags(Arrays.asList(more).subList(offset, more.length));
        if (sourceState == targetState && data == null) return Value.FALSE;

        BlockPos pos = target.getPos();
        BlockState finalState = sourceState;
        CompoundTag finalData = data;
        boolean[] result = {true};
        cc.server().executeBlocking(() -> {
            result[0] = world.setBlock(pos, finalState, flags);
            if (finalData == null) return;
            BlockEntity be = world.getBlockEntity(pos);
            if (be == null) return;

            CompoundTag destination = finalData.copy();
            destination.putInt("x", pos.getX());
            destination.putInt("y", pos.getY());
            destination.putInt("z", pos.getZ());
            try (final ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(be.problemPath(), CarpetScriptServer.LOG)) {
                be.loadWithComponents(TagValueInput.create(reporter, world.registryAccess(), destination));
            }

            be.setChanged();
            result[0] = true;
        });

        return result[0] ? new BlockValue(finalState, world, pos) : Value.FALSE;
    }

    @ScarpetFunction(maxParams = -1)
    public void shape_update(Context c, String direction, @Locator.Block BlockPos pos, @Locator.Block BlockPos neighborPos, @Locator.Block BlockState neighborState, Value... more) {
        Direction dir = switch (direction.toLowerCase(Locale.ROOT)) {
            case "down" -> Direction.DOWN;
            case "up" -> Direction.UP;
            case "north" -> Direction.NORTH;
            case "south" -> Direction.SOUTH;
            case "west" -> Direction.WEST;
            case "east" -> Direction.EAST;
            default -> throw new InternalExpressionException("Invalid direction: " + direction);
        };

        int updateLimit = 512;
        List<Value> args = Arrays.asList(more);
        if (more.length > 0 && more[0] instanceof NumericValue num) {
            updateLimit = num.getInt();
            args = args.subList(1, more.length);
        }
        int flags = locateUpdateFlags(args);

        ((CarpetContext)c).level().neighborShapeChanged(dir, pos, neighborPos, neighborState, flags, updateLimit);
    }

    @ScarpetFunction(maxParams = -1)
    public Value friction(@Locator.Block(acceptString = true) BlockState state) {
        return NumericValue.of(state.getBlock().getFriction());
    }

    @ScarpetFunction(maxParams = -1)
    public Value speed_factor(@Locator.Block(acceptString = true) BlockState state) {
        return NumericValue.of(state.getBlock().getSpeedFactor());
    }

    @ScarpetFunction(maxParams = -1)
    public Value jump_factor(@Locator.Block(acceptString = true) BlockState state) {
        return NumericValue.of(state.getBlock().getJumpFactor());
    }

    @ScarpetFunction(maxParams = -1)
    public Value block_description_id(@Locator.Block(acceptString = true) BlockState state) {
        return StringValue.of(state.getBlock().getDescriptionId());
    }

    @ScarpetFunction(maxParams = -1)
    public Value rotate_block(@Locator.Block(acceptString = true) BlockState state, int angle) {
        return new BlockValue(state.rotate(switch (angle % 360) {
            case 0 -> Rotation.NONE;
            case 90, -270 -> Rotation.CLOCKWISE_90;
            case 180, -180 -> Rotation.CLOCKWISE_180;
            case 270, -90 -> Rotation.COUNTERCLOCKWISE_90;
            default -> throw new InternalExpressionException("Block rotation must be a multiple of 90");
        }));
    }

    @ScarpetFunction(maxParams = -1)
    public Value mirror_block(@Locator.Block(acceptString = true) BlockState state, String mirror) {
        return new BlockValue(state.mirror(switch (mirror.toLowerCase(Locale.ROOT)) {
            case "none", "null", "" -> Mirror.NONE;
            case "left_right", "invert_z", "z" -> Mirror.LEFT_RIGHT;
            case "front_back", "invert_x", "x" -> Mirror.FRONT_BACK;
            case "up_down", "invert_y", "y" -> throw new InternalExpressionException("Blocks cannot be mirrored along the Y axis");
            default -> throw new InternalExpressionException("Invalid block mirror: " + mirror);
        }));
    }

    @ScarpetFunction(maxParams = 2)
    public Value collision_context(Context c, Optional<Value> entity, Optional<Boolean> standOnFluid) {
        return standOnFluid
            .map(s -> {
                Value v = entity.orElseThrow();
                if (!(v instanceof EntityValue e))
                    throw new InternalExpressionException("When calling collision_context with 2 arguments, the first one must be an entity");
                return CollisionContextValue.of((CarpetContext)c, CollisionContext.of(e.getEntity(), s));
            })
            .orElseGet(() -> CollisionContextValue.of((CarpetContext)c, CollisionContextValue.from(entity.orElse(Value.NULL))));
    }

    @ScarpetFunction(maxParams = 1)
    public Value collision_context_placement(Context c, Optional<ServerPlayer> player) {
        return CollisionContextValue.of((CarpetContext)c, CollisionContext.placementContext(player.orElse(null)));
    }

    @ScarpetFunction(maxParams = 2)
    public Value collision_context_with_position(Context c, double bottom, Optional<EntityValue> entity) {
        return CollisionContextValue.of((CarpetContext)c, CollisionContext.withPosition(entity.map(EntityValue::getEntity).orElse(null), bottom));
    }

    @ScarpetFunction(maxParams = 4)
    public Value block_occlusion_shape(Context c, @Locator.Block(acceptString = true) BlockState state, Optional<String> direction) {
        if (direction.isEmpty()) return VoxelShapeValue.of((CarpetContext)c, state.getOcclusionShape());
        Direction dir = switch (direction.get().toLowerCase(Locale.ROOT)) {
            case "down" -> Direction.DOWN;
            case "up" -> Direction.UP;
            case "north" -> Direction.NORTH;
            case "south" -> Direction.SOUTH;
            case "west" -> Direction.WEST;
            case "east" -> Direction.EAST;
            default -> throw new InternalExpressionException("Invalid direction: " + direction.get());
        };
        return VoxelShapeValue.of((CarpetContext)c, state.getFaceOcclusionShape(dir));
    }

    @ScarpetFunction(maxParams = 4)
    public Value block_shape(Context c, @Locator.Block BlockValue block, Optional<CollisionContext> collisionContext) {
        return VoxelShapeValue.of((CarpetContext)c, collisionContext
            .map(cc -> block.getBlockState().getShape(((CarpetContext)c).level(), block.getPos(), cc))
            .orElseGet(() -> block.getBlockState().getShape(((CarpetContext)c).level(), block.getPos())));
    }

    @ScarpetFunction(maxParams = 4)
    public Value collision_shape(Context c, @Locator.Block BlockValue block, Optional<CollisionContext> collisionContext) {
        return VoxelShapeValue.of((CarpetContext)c, collisionContext
            .map(cc -> block.getBlockState().getCollisionShape(((CarpetContext)c).level(), block.getPos(), cc))
            .orElseGet(() -> block.getBlockState().getCollisionShape(((CarpetContext)c).level(), block.getPos())));
    }

    @ScarpetFunction(maxParams = 4)
    public Value entity_inside_collision_shape(Context c, @Locator.Block BlockValue block, EntityValue entity) {
        return VoxelShapeValue.of((CarpetContext)c, block.getBlockState().getEntityInsideCollisionShape(((CarpetContext)c).level(), block.getPos(), entity.getEntity()));
    }

    @ScarpetFunction(maxParams = 3)
    public Value block_support_shape(Context c, @Locator.Block BlockValue block) {
        return VoxelShapeValue.of((CarpetContext)c, block.getBlockState().getBlockSupportShape(((CarpetContext)c).level(), block.getPos()));
    }

    @ScarpetFunction(maxParams = 4)
    public Value visual_shape(Context c, @Locator.Block BlockValue block, CollisionContext collisionContext) {
        return VoxelShapeValue.of((CarpetContext)c, block.getBlockState().getVisualShape(((CarpetContext)c).level(), block.getPos(), collisionContext));
    }

    @ScarpetFunction(maxParams = 3)
    public Value interaction_shape(Context c, @Locator.Block BlockValue block) {
        return VoxelShapeValue.of((CarpetContext)c, block.getBlockState().getInteractionShape(((CarpetContext)c).level(), block.getPos()));
    }
}

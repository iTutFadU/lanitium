package me.itut.lanitium.value;

import carpet.script.CarpetContext;
import carpet.script.Context;
import carpet.script.argument.BlockArgument;
import carpet.script.argument.Vector3Argument;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.*;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class VoxelShapeValue extends ObjectValue<VoxelShape> {
    private final CarpetContext context;

    protected VoxelShapeValue(CarpetContext context, VoxelShape value) {
        super(value);
        this.context = context;
    }

    public static Value of(CarpetContext context, VoxelShape value) {
        return value != null ? new VoxelShapeValue(context, value) : Value.NULL;
    }

    public static VoxelShape from(Value value) {
        return switch (value) {
            case null -> null;
            case NullValue ignored -> null;
            case VoxelShapeValue shape -> shape.value;
            default -> throw new InternalExpressionException("Cannot convert " + value.getTypeString() + " to voxel_shape");
        };
    }

    @Override
    public Value get(String what, Value... more) {
        return switch (what) {
            case "min" -> {
                checkArguments(what, more, 1, 3);
                String str = more[0].getString();
                Direction.Axis axis = switch (str.toLowerCase(Locale.ROOT)) {
                    case "x" -> Direction.Axis.X;
                    case "y" -> Direction.Axis.Y;
                    case "z" -> Direction.Axis.Z;
                    default -> throw new InternalExpressionException("Invalid axis: " + str);
                };
                yield NumericValue.of(more.length == 1
                    ? value.min(axis)
                    : value.min(axis, NumericValue.asNumber(more[1]).getDouble(), NumericValue.asNumber(more[more.length - 1]).getDouble())
                );
            }
            case "max" -> {
                checkArguments(what, more, 1, 3);
                String str = more[0].getString();
                Direction.Axis axis = switch (str.toLowerCase(Locale.ROOT)) {
                    case "x" -> Direction.Axis.X;
                    case "y" -> Direction.Axis.Y;
                    case "z" -> Direction.Axis.Z;
                    default -> throw new InternalExpressionException("Invalid axis: " + str);
                };
                yield NumericValue.of(more.length == 1
                    ? value.max(axis)
                    : value.max(axis, NumericValue.asNumber(more[1]).getDouble(), NumericValue.asNumber(more[more.length - 1]).getDouble())
                );
            }
            case "bounds" -> {
                checkArguments(what, more, 0);
                yield value.isEmpty()
                    ? Value.NULL
                    : ValueConversions.aabb(value.bounds());
            }
            case "empty" -> {
                checkArguments(what, more, 0);
                yield BooleanValue.of(value.isEmpty());
            }
            case "for_edges" -> {
                checkArguments(what, more, 1);
                if (!(more[0] instanceof FunctionValue callback))
                    throw new InternalExpressionException("voxel_shape~'" + what + "' expects a function as an argument");
                value.forAllEdges((x1, y1, z1, x2, y2, z2) -> callback.callInContext(context, Context.VOID, List.of(
                    NumericValue.of(x1), NumericValue.of(y1), NumericValue.of(z1),
                    NumericValue.of(x2), NumericValue.of(y2), NumericValue.of(z2)
                )));
                yield Value.NULL;
            }
            case "for_boxes" -> {
                checkArguments(what, more, 1);
                if (!(more[0] instanceof FunctionValue callback))
                    throw new InternalExpressionException("voxel_shape~'" + what + "' expects a function as an argument");
                value.forAllBoxes((x1, y1, z1, x2, y2, z2) -> callback.callInContext(context, Context.VOID, List.of(
                    NumericValue.of(x1), NumericValue.of(y1), NumericValue.of(z1),
                    NumericValue.of(x2), NumericValue.of(y2), NumericValue.of(z2)
                )));
                yield Value.NULL;
            }
            case "aabbs" -> {
                checkArguments(what, more, 0);
                yield ListValue.wrap(value.toAabbs().stream().map(ValueConversions::aabb));
            }
            case "clip" -> {
                checkArguments(what, more, 3, 9);
                List<Value> lv = Arrays.asList(more);
                Vector3Argument start = Vector3Argument.findIn(lv, 0),
                                finish = Vector3Argument.findIn(lv, start.offset);
                BlockArgument block = BlockArgument.findIn(context, lv, finish.offset);
                yield ValueConversions.hitResult(value.clip(start.vec, finish.vec, block.block.getPos()));
            }
            case "closest_point_to" -> {
                checkArguments(what, more, 1, 3);
                yield value.closestPointTo(Vector3Argument.findIn(Arrays.asList(more), 0).vec).map(v -> ListValue.fromTriple(v.x, v.y, v.z)).orElse(Value.NULL);
            }
            case "face_shape" -> {
                checkArguments(what, more, 1);
                String str = more[0].getString();
                yield of(context, value.getFaceShape(switch (str.toLowerCase(Locale.ROOT)) {
                    case "down" -> Direction.DOWN;
                    case "up" -> Direction.UP;
                    case "north" -> Direction.NORTH;
                    case "south" -> Direction.SOUTH;
                    case "west" -> Direction.WEST;
                    case "east" -> Direction.EAST;
                    default -> throw new InternalExpressionException("Invalid direction: " + str);
                }));
            }
            case "collide" -> {
                checkArguments(what, more, 3);
                String str = more[0].getString();
                Direction.Axis axis = switch (str.toLowerCase(Locale.ROOT)) {
                    case "x" -> Direction.Axis.X;
                    case "y" -> Direction.Axis.Y;
                    case "z" -> Direction.Axis.Z;
                    default -> throw new InternalExpressionException("Invalid axis: " + str);
                };
                yield NumericValue.of(value.collide(axis, ValueConversions.toAabb(more[1]), NumericValue.asNumber(more[2]).getDouble()));
            }
            default -> unknownFeature(what);
        };
    }

    @Override
    public String getTypeString() {
        return "voxel_shape";
    }
}

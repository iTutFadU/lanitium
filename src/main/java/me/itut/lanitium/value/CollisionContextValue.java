package me.itut.lanitium.value;

import carpet.script.CarpetContext;
import carpet.script.argument.BlockArgument;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.*;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Arrays;
import java.util.List;

public class CollisionContextValue extends ObjectValue<CollisionContext> {
    private final CarpetContext context;

    protected CollisionContextValue(CarpetContext context, CollisionContext value) {
        super(value);
        this.context = context;
    }

    public static Value of(CarpetContext context, CollisionContext value) {
        return value != null ? new CollisionContextValue(context, value) : Value.NULL;
    }

    public static CollisionContext from(Value value) {
        return switch (value) {
            case null -> null;
            case NullValue ignored -> CollisionContext.empty();
            case EntityValue entity -> CollisionContext.of(entity.getEntity());
            default -> throw new InternalExpressionException("Cannot convert " + value.getTypeString() + " to collision_context");
        };
    }

    @Override
    public Value get(String what, Value... more) {
        return switch (what) {
            case "descending" -> {
                checkArguments(what, more, 0);
                yield BooleanValue.of(value.isDescending());
            }
            case "above" -> {
                checkArguments(what, more, 3, 5);
                VoxelShape shape = VoxelShapeValue.from(more[0]);
                BlockArgument pos = BlockArgument.findIn(context, Arrays.asList(more), 1);
                boolean aBoolean = more[pos.offset].getBoolean(); // ArrayIndexOutOfBoundsException for ~['above', shape, x, y, z]
                yield BooleanValue.of(value.isAbove(shape, pos.block.getPos(), aBoolean));
            }
            case "holding_item" -> {
                checkArguments(what, more, 1);
                yield BooleanValue.of(value.isHoldingItem(NBTSerializableValue.parseItem(more[0].getString(), context.registryAccess()).getItem()));
            }
            case "can_stand_on_fluid" -> {
                checkArguments(what, more, 2, 6);
                List<Value> lv = Arrays.asList(more);
                BlockArgument lower = BlockArgument.findIn(context, lv, 0, true),
                              on = BlockArgument.findIn(context, lv, lower.offset, true);
                yield BooleanValue.of(value.canStandOnFluid(lower.block.getBlockState().getFluidState(), on.block.getBlockState().getFluidState()));
            }
            case "placement" -> {
                checkArguments(what, more, 0);
                yield BooleanValue.of(value.isPlacement());
            }
            default -> unknownFeature(what);
        };
    }

    @Override
    public String getTypeString() {
        return "collision_context";
    }
}

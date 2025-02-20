package me.itut.lanitium.value.brigadier.argument;

import carpet.script.CarpetContext;
import carpet.script.Context;
import carpet.script.annotation.ScarpetFunction;
import carpet.script.value.Value;
import com.mojang.brigadier.arguments.*;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.arguments.*;
import net.minecraft.commands.arguments.blocks.BlockPredicateArgument;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.commands.arguments.coordinates.*;
import net.minecraft.commands.arguments.item.ItemArgument;

import java.util.Optional;

public class ArgumentFunctions {
    @ScarpetFunction
    public Value argument_type_bool(Context c) {
        return ArgumentTypeValue.of((CarpetContext)c, BoolArgumentType.bool());
    }

    @ScarpetFunction(maxParams = 2)
    public Value argument_type_double(Context c, Optional<Double> min, Optional<Double> max) {
        return ArgumentTypeValue.of((CarpetContext)c, DoubleArgumentType.doubleArg(min.orElse(-Double.MAX_VALUE), max.orElse(Double.MAX_VALUE)));
    }

    @ScarpetFunction(maxParams = 2)
    public Value argument_type_float(Context c, Optional<Double> min, Optional<Double> max) {
        return ArgumentTypeValue.of((CarpetContext)c, FloatArgumentType.floatArg(min.map(v -> (float)(double)v).orElse(-Float.MAX_VALUE), max.map(v -> (float)(double)v).orElse(Float.MAX_VALUE)));
    }

    @ScarpetFunction(maxParams = 2)
    public Value argument_type_integer(Context c, Optional<Integer> min, Optional<Integer> max) {
        return ArgumentTypeValue.of((CarpetContext)c, IntegerArgumentType.integer(min.orElse(Integer.MIN_VALUE), max.orElse(Integer.MAX_VALUE)));
    }

    @ScarpetFunction(maxParams = 2)
    public Value argument_type_long(Context c, Optional<Long> min, Optional<Long> max) {
        return ArgumentTypeValue.of((CarpetContext)c, LongArgumentType.longArg(min.orElse(Long.MIN_VALUE), max.orElse(Long.MAX_VALUE)));
    }

    @ScarpetFunction
    public Value argument_type_word(Context c) {
        return ArgumentTypeValue.of((CarpetContext)c, StringArgumentType.word());
    }

    @ScarpetFunction
    public Value argument_type_string(Context c) {
        return ArgumentTypeValue.of((CarpetContext)c, StringArgumentType.string());
    }

    @ScarpetFunction
    public Value argument_type_greedy_string(Context c) {
        return ArgumentTypeValue.of((CarpetContext)c, StringArgumentType.greedyString());
    }

    @ScarpetFunction
    public Value argument_type_angle(Context c) {
        return ArgumentTypeValue.of((CarpetContext)c, AngleArgument.angle());
    }

    @ScarpetFunction
    public Value argument_type_color(Context c) {
        return ArgumentTypeValue.of((CarpetContext)c, ColorArgument.color());
    }

    @ScarpetFunction
    public Value argument_type_component(Context c) {
        return ArgumentTypeValue.of((CarpetContext)c, ComponentArgument.textComponent(CommandBuildContext.simple(((CarpetContext)c).registryAccess(), ((CarpetContext)c).source().enabledFeatures())));
    }

    @ScarpetFunction
    public Value argument_type_compound(Context c) {
        return ArgumentTypeValue.of((CarpetContext)c, CompoundTagArgument.compoundTag());
    }

    @ScarpetFunction
    public Value argument_type_dimension(Context c) {
        return ArgumentTypeValue.of((CarpetContext)c, DimensionArgument.dimension());
    }

    @ScarpetFunction
    public Value argument_type_anchor(Context c) {
        return ArgumentTypeValue.of((CarpetContext)c, EntityAnchorArgument.anchor());
    }

    @ScarpetFunction
    public Value argument_type_entities(Context c) {
        return ArgumentTypeValue.of((CarpetContext)c, EntityArgument.entities());
    }

    @ScarpetFunction
    public Value argument_type_entity(Context c) {
        return ArgumentTypeValue.of((CarpetContext)c, EntityArgument.entity());
    }

    @ScarpetFunction
    public Value argument_type_players(Context c) {
        return ArgumentTypeValue.of((CarpetContext)c, EntityArgument.players());
    }

    @ScarpetFunction
    public Value argument_type_player(Context c) {
        return ArgumentTypeValue.of((CarpetContext)c, EntityArgument.player());
    }

    @ScarpetFunction
    public Value argument_type_gamemode(Context c) {
        return ArgumentTypeValue.of((CarpetContext)c, GameModeArgument.gameMode());
    }

    @ScarpetFunction
    public Value argument_type_player_name(Context c) {
        return ArgumentTypeValue.of((CarpetContext)c, GameProfileArgument.gameProfile());
    }

    @ScarpetFunction
    public Value argument_type_heightmap(Context c) {
        return ArgumentTypeValue.of((CarpetContext)c, HeightmapTypeArgument.heightmap());
    }

    @ScarpetFunction
    public Value argument_type_message(Context c) {
        return ArgumentTypeValue.of((CarpetContext)c, MessageArgument.message());
    }

    @ScarpetFunction
    public Value argument_type_nbt_path(Context c) {
        return ArgumentTypeValue.of((CarpetContext)c, NbtPathArgument.nbtPath());
    }

    @ScarpetFunction
    public Value argument_type_nbt(Context c) {
        return ArgumentTypeValue.of((CarpetContext)c, NbtTagArgument.nbtTag());
    }

    @ScarpetFunction
    public Value argument_type_objective(Context c) {
        return ArgumentTypeValue.of((CarpetContext)c, ObjectiveArgument.objective());
    }

    @ScarpetFunction
    public Value argument_type_objective_criteria(Context c) {
        return ArgumentTypeValue.of((CarpetContext)c, ObjectiveCriteriaArgument.criteria());
    }

    @ScarpetFunction
    public Value argument_type_scoreboard_operation(Context c) {
        return ArgumentTypeValue.of((CarpetContext)c, OperationArgument.operation());
    }

    @ScarpetFunction
    public Value argument_type_particle(Context c) {
        return ArgumentTypeValue.of((CarpetContext)c, ParticleArgument.particle(CommandBuildContext.simple(((CarpetContext)c).registryAccess(), ((CarpetContext)c).source().enabledFeatures())));
    }

    @ScarpetFunction
    public Value argument_type_int_range(Context c) {
        return ArgumentTypeValue.of((CarpetContext)c, RangeArgument.intRange());
    }

    @ScarpetFunction
    public Value argument_type_float_range(Context c) {
        return ArgumentTypeValue.of((CarpetContext)c, RangeArgument.floatRange());
    }

    @ScarpetFunction
    public Value argument_type_identifier(Context c) {
        return ArgumentTypeValue.of((CarpetContext)c, ResourceLocationArgument.id());
    }

    @ScarpetFunction
    public Value argument_type_scoreboard_slot(Context c) {
        return ArgumentTypeValue.of((CarpetContext)c, ScoreboardSlotArgument.displaySlot());
    }

    @ScarpetFunction
    public Value argument_type_score_holders(Context c) {
        return ArgumentTypeValue.of((CarpetContext)c, ScoreHolderArgument.scoreHolders());
    }

    @ScarpetFunction
    public Value argument_type_score_holder(Context c) {
        return ArgumentTypeValue.of((CarpetContext)c, ScoreHolderArgument.scoreHolder());
    }

    @ScarpetFunction
    public Value argument_type_slot(Context c) {
        return ArgumentTypeValue.of((CarpetContext)c, SlotArgument.slot());
    }

    @ScarpetFunction
    public Value argument_type_slots(Context c) {
        return ArgumentTypeValue.of((CarpetContext)c, SlotsArgument.slots());
    }

    @ScarpetFunction
    public Value argument_type_style(Context c) {
        return ArgumentTypeValue.of((CarpetContext)c, StyleArgument.style(CommandBuildContext.simple(((CarpetContext)c).registryAccess(), ((CarpetContext)c).source().enabledFeatures())));
    }

    @ScarpetFunction
    public Value argument_type_team(Context c) {
        return ArgumentTypeValue.of((CarpetContext)c, TeamArgument.team());
    }

    @ScarpetFunction
    public Value argument_type_template_mirror(Context c) {
        return ArgumentTypeValue.of((CarpetContext)c, TemplateMirrorArgument.templateMirror());
    }

    @ScarpetFunction
    public Value argument_type_template_rotation(Context c) {
        return ArgumentTypeValue.of((CarpetContext)c, TemplateRotationArgument.templateRotation());
    }

    @ScarpetFunction(maxParams = 1)
    public Value argument_type_time(Context c, Optional<Integer> min) {
        return ArgumentTypeValue.of((CarpetContext)c, TimeArgument.time(min.orElse(0)));
    }

    @ScarpetFunction
    public Value argument_type_uuid(Context c) {
        return ArgumentTypeValue.of((CarpetContext)c, UuidArgument.uuid());
    }

    @ScarpetFunction
    public Value argument_type_block_predicate(Context c) {
        return ArgumentTypeValue.of((CarpetContext)c, BlockPredicateArgument.blockPredicate(CommandBuildContext.simple(((CarpetContext)c).registryAccess(), ((CarpetContext)c).source().enabledFeatures())));
    }

    @ScarpetFunction
    public Value argument_type_block_state(Context c) {
        return ArgumentTypeValue.of((CarpetContext)c, BlockStateArgument.block(CommandBuildContext.simple(((CarpetContext)c).registryAccess(), ((CarpetContext)c).source().enabledFeatures())));
    }

    @ScarpetFunction
    public Value argument_type_block_pos(Context c) {
        return ArgumentTypeValue.of((CarpetContext)c, BlockPosArgument.blockPos());
    }

    @ScarpetFunction
    public Value argument_type_column_pos(Context c) {
        return ArgumentTypeValue.of((CarpetContext)c, ColumnPosArgument.columnPos());
    }

    @ScarpetFunction
    public Value argument_type_rotation(Context c) {
        return ArgumentTypeValue.of((CarpetContext)c, RotationArgument.rotation());
    }

    @ScarpetFunction
    public Value argument_type_swizzle(Context c) {
        return ArgumentTypeValue.of((CarpetContext)c, SwizzleArgument.swizzle());
    }

    @ScarpetFunction(maxParams = 1)
    public Value argument_type_vec2(Context c, Optional<Boolean> centered) {
        return ArgumentTypeValue.of((CarpetContext)c, Vec2Argument.vec2(centered.orElse(true)));
    }

    @ScarpetFunction(maxParams = 1)
    public Value argument_type_vec3(Context c, Optional<Boolean> centered) {
        return ArgumentTypeValue.of((CarpetContext)c, Vec3Argument.vec3(centered.orElse(true)));
    }

    @ScarpetFunction
    public Value argument_type_item(Context c) {
        return ArgumentTypeValue.of((CarpetContext)c, ItemArgument.item(CommandBuildContext.simple(((CarpetContext)c).registryAccess(), ((CarpetContext)c).source().enabledFeatures())));
    }

//    @ScarpetFunction
//    public Value argument_type_item_predicate(Context c) {
//        return ArgumentTypeValue.of((CarpetContext)c, ItemPredicateArgument.itemPredicate(CommandBuildContext.simple(((CarpetContext)c).registryAccess(), ((CarpetContext)c).source().enabledFeatures())));
//    }

    // TODO: Add resource arguments
}

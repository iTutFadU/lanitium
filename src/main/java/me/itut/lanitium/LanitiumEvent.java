package me.itut.lanitium;

import carpet.script.CarpetEventServer;
import carpet.script.value.*;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Optional;

public abstract class LanitiumEvent extends CarpetEventServer.Event {
    public LanitiumEvent(String name, int reqArgs, boolean isGlobalOnly) {
        super(name, reqArgs, isGlobalOnly);
    }

    public void onPlayerCustomClick(ServerPlayer player, ResourceLocation id, Optional<Tag> payload) {}

    public void onProjectileHit(Projectile projectile, HitResult hit) {}

    public void onProjectileDeflected(Projectile projectile, @Nullable Entity entity, boolean aim) {}

    public static final LanitiumEvent PLAYER_CUSTOM_CLICK = new LanitiumEvent("player_custom_click", 3, false) {
        @Override
        public void onPlayerCustomClick(ServerPlayer player, ResourceLocation id, Optional<Tag> payload) {
            handler.call(() -> Arrays.asList(
                EntityValue.of(player),
                ValueConversions.of(id),
                payload.map(NBTSerializableValue::of).orElse(Value.NULL)
            ), player::createCommandSourceStack);
        }
    };

    public static final LanitiumEvent PROJECTILE_HIT_BLOCK = new LanitiumEvent("projectile_hit_block", 2, true) {
        @Override
        public void onProjectileHit(Projectile projectile, HitResult hit) {
            handler.call(() -> Arrays.asList(
                EntityValue.of(projectile),
                me.itut.lanitium.value.ValueConversions.hitResult(hit)
            ), () -> projectile.createCommandSourceStackForNameResolution((ServerLevel)projectile.level()));
        }
    };

    public static final LanitiumEvent PROJECTILE_HIT_ENTITY = new LanitiumEvent("projectile_hit_entity", 2, true) {
        @Override
        public void onProjectileHit(Projectile projectile, HitResult hit) {
            handler.call(() -> Arrays.asList(
                EntityValue.of(projectile),
                me.itut.lanitium.value.ValueConversions.hitResult(hit)
            ), () -> projectile.createCommandSourceStackForNameResolution((ServerLevel)projectile.level()));
        }
    };

    public static final LanitiumEvent PROJECTILE_DEFLECTED = new LanitiumEvent("projectile_deflected", 3, true) {
        @Override
        public void onProjectileDeflected(Projectile projectile, @Nullable Entity entity, boolean aim) {
            handler.call(() -> Arrays.asList(
                EntityValue.of(projectile),
                EntityValue.of(entity),
                BooleanValue.of(aim)
            ), () -> projectile.createCommandSourceStackForNameResolution((ServerLevel)projectile.level()));
        }
    };
}

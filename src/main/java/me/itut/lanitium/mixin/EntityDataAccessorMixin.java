package me.itut.lanitium.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.commands.data.EntityDataAccessor;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.TagValueInput;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(value = EntityDataAccessor.class, priority = 1001) // Below Lithium
public abstract class EntityDataAccessorMixin {
    @Shadow @Final private static Logger LOGGER;
    @Shadow @Final private Entity entity;

    @Inject(method = "setData", at = @At("HEAD"), cancellable = true)
    private void setDataYesPlayer(CompoundTag compound, CallbackInfo ci) {
        ci.cancel();
        UUID uUID = entity.getUUID();
        try (ProblemReporter.ScopedCollector scopedCollector = new ProblemReporter.ScopedCollector(this.entity.problemPath(), LOGGER)) {
            this.entity.load(TagValueInput.create(scopedCollector, this.entity.registryAccess(), compound));
            this.entity.setUUID(uUID);
        }
    }
}

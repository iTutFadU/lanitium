package me.itut.lanitium.mixin;

import me.itut.lanitium.LanitiumEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Projectile.class)
public abstract class ProjectileMixin {
    @Inject(method = "onHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/Projectile;onHitBlock(Lnet/minecraft/world/phys/BlockHitResult;)V"))
    void onHitBlockEvent(HitResult hitResult, CallbackInfo ci) {
        if (LanitiumEvent.PROJECTILE_HIT_BLOCK.isNeeded())
            LanitiumEvent.PROJECTILE_HIT_BLOCK.onProjectileHit((Projectile)(Object)this, hitResult);
    }

    @Inject(method = "onHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/Projectile;onHitEntity(Lnet/minecraft/world/phys/EntityHitResult;)V"))
    void onHitEntityEvent(HitResult hitResult, CallbackInfo ci) {
        if (LanitiumEvent.PROJECTILE_HIT_ENTITY.isNeeded())
            LanitiumEvent.PROJECTILE_HIT_ENTITY.onProjectileHit((Projectile)(Object)this, hitResult);
    }

    @Inject(method = "deflect", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/Projectile;onDeflection(Lnet/minecraft/world/entity/Entity;Z)V"))
    void onDeflectedEvent(ProjectileDeflection projectileDeflection, Entity entity, Entity entity2, boolean bl, CallbackInfoReturnable<Boolean> cir) {
        if (LanitiumEvent.PROJECTILE_DEFLECTED.isNeeded())
            LanitiumEvent.PROJECTILE_DEFLECTED.onProjectileDeflected((Projectile)(Object)this, entity, bl);
    }
}

package me.itut.lanitium.mixin;

import me.itut.lanitium.Lanitium;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Inject(method = "getServerModName", at = @At("HEAD"), cancellable = true, remap = false)
    private void customServerBrand(CallbackInfoReturnable<String> cir) {
        if (Lanitium.CONFIG.modName != null) cir.setReturnValue(Lanitium.CONFIG.modName);
    }
}

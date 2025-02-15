package me.itut.lanitium.mixin;

import me.itut.lanitium.Lanitium;
import net.minecraft.server.ServerLinks;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.dedicated.DedicatedServerSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DedicatedServer.class)
public abstract class DedicatedServerMixin {
    @Inject(method = "createServerLinks(Lnet/minecraft/server/dedicated/DedicatedServerSettings;)Lnet/minecraft/server/ServerLinks;", at = @At("HEAD"), cancellable = true)
    private static void customServerLinks(DedicatedServerSettings dedicatedServerSettings, CallbackInfoReturnable<ServerLinks> cir) {
        if (Lanitium.CONFIG.links != null) cir.setReturnValue(new ServerLinks(Lanitium.CONFIG.links));
    }
}

package me.itut.lanitium.mixin;

import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static carpet.script.CarpetEventServer.Event.PLAYER_COMMAND;

@Mixin(value = ServerGamePacketListenerImpl.class, priority = 999)
public abstract class ServerGamePacketListenerImplMixin {
    @Shadow
    public ServerPlayer player;

    @Shadow
    protected abstract void tryHandleChat(String string, Runnable runnable);
    @Shadow
    protected abstract void performUnsignedChatCommand(String string);
    @Shadow
    protected abstract void detectRateSpam();

    @Inject(method = "handleChatCommand(Lnet/minecraft/network/protocol/game/ServerboundChatCommandPacket;)V", at = @At("HEAD"), cancellable = true) // EZ
    private void playerCommandEventFix(ServerboundChatCommandPacket packet, CallbackInfo ci) {
        ci.cancel();
        if (PLAYER_COMMAND.isNeeded() && PLAYER_COMMAND.onPlayerMessage(player, packet.command()) && !packet.command().startsWith("script "))
            return;
        tryHandleChat(packet.command(), () -> {
            performUnsignedChatCommand(packet.command());
            detectRateSpam();
        });
    }
}

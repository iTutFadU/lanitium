package me.itut.lanitium.mixin;

import me.itut.lanitium.Lanitium;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
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

    @Inject(method = "handleChatCommand", at = @At("HEAD"), cancellable = true)
    private void handleChatCommand(ServerboundChatCommandPacket packet, CallbackInfo ci) { // EZ
        ci.cancel();
        if (PLAYER_COMMAND.isNeeded() && PLAYER_COMMAND.onPlayerMessage(player, packet.command()))
            return;
        tryHandleChat(packet.command(), () -> {
            performUnsignedChatCommand(packet.command());
            detectRateSpam();
        });
    }

    @Redirect(method = "removePlayerFromWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"))
    private void removePlayerFromWorldDisableLeaveMessage(PlayerList instance, Component component, boolean bl) {
        if (!Lanitium.CONFIG.disableLeaveMessages) instance.broadcastSystemMessage(component, bl);
    }
}

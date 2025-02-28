package me.itut.lanitium.mixin;

import me.itut.lanitium.Lanitium;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
    @Redirect(method = "placeNewPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"))
    private void placeNewPlayerDisableJoinMessage(PlayerList instance, Component component, boolean bl) {
        if (!Lanitium.CONFIG.disableJoinMessages) instance.broadcastSystemMessage(component, bl);
    }
}

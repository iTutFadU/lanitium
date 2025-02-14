package me.itut.lanitium.mixin;

import com.mojang.authlib.GameProfile;
import me.itut.lanitium.Lanitium;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.status.ServerStatus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

import static me.itut.lanitium.Lanitium.MOTD;

@Mixin(value = ServerStatus.class, priority = 1001) // above Scarpet Additions
public abstract class ServerStatusMixin {
    @Shadow @Final
    private Optional<ServerStatus.Players> players;

    @Inject(method = "description", at = @At("HEAD"), cancellable = true)
    private void descriptionFromScarpet(CallbackInfoReturnable<Component> cir) {
        if (MOTD != null) cir.setReturnValue(MOTD);
    }

    @Inject(method = "players", at = @At("HEAD"), cancellable = true)
    private void playersFromScarpet(CallbackInfoReturnable<Optional<ServerStatus.Players>> cir) {
        if (players.isEmpty() || Lanitium.PLAYERS_ONLINE == null && Lanitium.PLAYERS_MAX == null && Lanitium.PLAYERS_SAMPLE == null) {
            cir.setReturnValue(players);
            return;
        }
        cir.setReturnValue(Optional.of(new ServerStatus.Players(Lanitium.PLAYERS_ONLINE != null ? Lanitium.PLAYERS_ONLINE : players.get().online(), Lanitium.PLAYERS_MAX != null ? Lanitium.PLAYERS_MAX : players.get().max(), Lanitium.PLAYERS_SAMPLE != null ? Lanitium.PLAYERS_SAMPLE : players.get().sample())));
    }
}

package me.itut.lanitium.mixin;

import me.itut.lanitium.Lanitium;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.status.ServerStatus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(value = ServerStatus.class, priority = 999) // _actually_ above Scarpet Additions
public abstract class ServerStatusMixin {
    @Shadow @Final
    private Optional<ServerStatus.Players> players;

    @Inject(method = "description", at = @At("HEAD"), cancellable = true)
    private void descriptionFromScarpet(CallbackInfoReturnable<Component> cir) {
        if (Lanitium.CONFIG.displayMotd != null) cir.setReturnValue(Lanitium.CONFIG.displayMotd);
    }

    /**
     * @author iTut
     * @reason Separate fields can have a custom or default value.
     */
    @Overwrite
    public Optional<ServerStatus.Players> players() {
        if (players.isEmpty() || Lanitium.CONFIG.displayPlayersOnline == null && Lanitium.CONFIG.displayPlayersMax == null && Lanitium.CONFIG.displayPlayersSampleProfiles == null)
            return players;
        return Optional.of(new ServerStatus.Players(Lanitium.CONFIG.displayPlayersMax != null ? Lanitium.CONFIG.displayPlayersMax : players.get().max(), Lanitium.CONFIG.displayPlayersOnline != null ? Lanitium.CONFIG.displayPlayersOnline : players.get().online(), Lanitium.CONFIG.displayPlayersSampleProfiles != null ? Lanitium.CONFIG.displayPlayersSampleProfiles : players.get().sample()));
    }
}

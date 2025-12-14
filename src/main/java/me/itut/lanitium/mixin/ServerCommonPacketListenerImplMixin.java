package me.itut.lanitium.mixin;

import me.itut.lanitium.LanitiumEvent;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class ServerCommonPacketListenerImplMixin {
    // FIXME: refactor to use Fabric API when https://github.com/FabricMC/fabric/pull/4740 gets merged
    @Inject(method = "handleCustomClickAction", at = @At("TAIL"))
    private void waitingForFabricAPI(ServerboundCustomClickActionPacket packet, CallbackInfo ci) {
        ResourceLocation id = packet.id();
        Optional<Tag> payload = packet.payload();

        if ((ServerCommonPacketListenerImpl)(Object)this instanceof ServerGamePacketListenerImpl game && LanitiumEvent.PLAYER_CUSTOM_CLICK.isNeeded())
            LanitiumEvent.PLAYER_CUSTOM_CLICK.onPlayerCustomClick(game.getPlayer(), id, payload);
    }
}

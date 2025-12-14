package me.itut.lanitium;

import carpet.script.CarpetEventServer;
import carpet.script.value.EntityValue;
import carpet.script.value.NBTSerializableValue;
import carpet.script.value.Value;
import carpet.script.value.ValueConversions;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.Optional;

public abstract class LanitiumEvent extends CarpetEventServer.Event {
    public LanitiumEvent(String name, int reqArgs, boolean isGlobalOnly) {
        super(name, reqArgs, isGlobalOnly);
    }

    public void onPlayerCustomClick(ServerPlayer player, ResourceLocation id, Optional<Tag> payload) {}

    public static final LanitiumEvent PLAYER_CUSTOM_CLICK = new LanitiumEvent("player_custom_click", 3, false) {
        @Override
        public void onPlayerCustomClick(ServerPlayer player, ResourceLocation id, Optional<Tag> payload) {
            handler.call(() -> Arrays.asList(
                EntityValue.of(player),
                ValueConversions.of(id),
                payload.map(NBTSerializableValue::of).orElse(Value.NULL)
            ), player::createCommandSourceStack);
        }
    };
}

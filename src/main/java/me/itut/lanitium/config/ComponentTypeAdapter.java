package me.itut.lanitium.config;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

import java.lang.reflect.Type;

public class ComponentTypeAdapter implements JsonSerializer<Component>, JsonDeserializer<Component> {
    @Override
    public JsonElement serialize(Component component, Type type, JsonSerializationContext jsonSerializationContext) {
        return ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, component).getOrThrow();
    }

    @Override
    public Component deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        return ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, jsonElement).getOrThrow(JsonParseException::new);
    }
}

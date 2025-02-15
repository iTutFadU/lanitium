package me.itut.lanitium.config;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.ServerLinks;

import java.lang.reflect.Type;
import java.net.URI;

public class ServerLinksEntryTypeAdapter implements JsonSerializer<ServerLinks.Entry>, JsonDeserializer<ServerLinks.Entry> {
    @Override
    public JsonElement serialize(ServerLinks.Entry src, Type typeOfSrc, JsonSerializationContext context) {
        final JsonObject object = new JsonObject();
        object.addProperty("link", src.link().toString());
        src.type().ifLeft(v -> object.addProperty("type", v.name().toLowerCase())).ifRight(v -> object.add("name", ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, v).getOrThrow()));
        return object;
    }

    @Override
    public ServerLinks.Entry deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        final JsonObject object = jsonElement.getAsJsonObject();
        final URI uri = URI.create(object.get("link").getAsString());
        if (object.has("name"))
            return ServerLinks.Entry.custom(ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, object.get("name")).getOrThrow(JsonParseException::new), uri);
        return ServerLinks.Entry.knownType(ServerLinks.KnownLinkType.valueOf(object.get("type").getAsString().toUpperCase()), uri);
    }
}

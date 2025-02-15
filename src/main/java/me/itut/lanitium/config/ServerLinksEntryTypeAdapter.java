package me.itut.lanitium.config;

import com.google.gson.*;
import me.itut.lanitium.internal.ServerLinksKnownLinkTypeNames;
import net.minecraft.network.chat.Component;
import net.minecraft.server.ServerLinks;

import java.lang.reflect.Type;
import java.net.URI;

public class ServerLinksEntryTypeAdapter implements JsonSerializer<ServerLinks.Entry>, JsonDeserializer<ServerLinks.Entry> {
    @Override
    public JsonElement serialize(ServerLinks.Entry src, Type typeOfSrc, JsonSerializationContext context) {
        final JsonObject object = new JsonObject();
        object.addProperty("link", src.link().toString());
        src.type().ifLeft(v -> object.addProperty("type", ServerLinksKnownLinkTypeNames.name(v))).ifRight(v -> object.add("name", context.serialize(v)));
        return object;
    }

    @Override
    public ServerLinks.Entry deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) throws JsonParseException {
        final JsonObject object = jsonElement.getAsJsonObject();
        final URI uri = URI.create(object.get("link").getAsString());
        if (object.has("name"))
            return ServerLinks.Entry.custom(context.deserialize(object.get("name"), Component.class), uri);
        return ServerLinks.Entry.knownType(ServerLinksKnownLinkTypeNames.from(object.get("type").getAsString()), uri);
    }
}

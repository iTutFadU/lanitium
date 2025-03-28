package me.itut.lanitium.function;

import carpet.script.CarpetContext;
import carpet.script.Context;
import carpet.script.annotation.ScarpetFunction;
import carpet.script.value.*;
import me.itut.lanitium.Lanitium;
import me.itut.lanitium.LanitiumCookie;
import me.itut.lanitium.value.FutureValue;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Protocol {
    // TODO: Revisit cookies
    @ScarpetFunction
    public static Value cookie(Context c, ServerPlayer player, FunctionValue callback) {
        return FutureValue.of((CarpetContext)c, player.getCookie(LanitiumCookie.class).handle((cookie, exception) -> {
            MapValue map = null;

            if (cookie != null) {
                Map<Value, Value> values = new HashMap<>();
                cookie.cookie.forEach((k, v) -> values.put(StringValue.of(k), NBTSerializableValue.of(v)));
                map = MapValue.wrap(values);
            }

            final Value mapValue = map != null ? map : Value.NULL;
            List<Value> args = new ArrayList<>() {{
                add(EntityValue.of(player));
                add(mapValue);
            }};
            boolean set = false;
            try {
                Value out = callback.callInContext(c, Context.STRING, args).evalValue(c, Context.STRING);
                if ("set".equals(out.getString())) set = true;
            } finally {
                if (set)
                    if (map == null)
                        player.setCookie(LanitiumCookie.EMPTY);
                    else {
                        Map<String, Tag> newCookie = new HashMap<>();
                        map.getMap().forEach((k, v) -> newCookie.put(k.getString(), v.toTag(true, ((CarpetContext)c).registryAccess())));
                        player.setCookie(new LanitiumCookie(newCookie));
                    }
            }

            return mapValue;
        }));
    }

    @ScarpetFunction
    public static void cookie_reset(ServerPlayer player) {
        player.setCookie(LanitiumCookie.EMPTY);
    }

    @ScarpetFunction
    public static void cookie_secret(String secret) {
        Lanitium.COOKIE.setSecret(secret);
    }
}

package me.itut.lanitium.value;

import carpet.script.CarpetContext;
import carpet.script.value.BooleanValue;
import carpet.script.value.Value;
import me.itut.lanitium.LanitiumCookie;

import java.util.concurrent.CompletableFuture;

public class LanitiumCookieFuture extends ObjectValue<CompletableFuture<LanitiumCookie>> {
    public LanitiumCookieFuture(CarpetContext context, CompletableFuture<LanitiumCookie> value) {
        super(context, value);
    }

    public Value get(String what, Value... more) {
        return switch (what) {
            case "done" -> checkArguments(what, more, 0, () -> BooleanValue.of(value.isDone()));
            case "cancelled" -> checkArguments(what, more, 0, () -> BooleanValue.of(value.isCancelled()));
            default -> unknownFeature(what);
        };
    }

    @Override
    public String getTypeString() {
        return "lanitium_cookie_future";
    }
}

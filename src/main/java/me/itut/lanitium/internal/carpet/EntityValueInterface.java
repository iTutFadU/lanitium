package me.itut.lanitium.internal.carpet;

import carpet.script.value.Value;
import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.function.BiConsumer;

public interface EntityValueInterface {
    Map<String, BiConsumer<Entity, Value>> lanitium$featureModifiers();
}

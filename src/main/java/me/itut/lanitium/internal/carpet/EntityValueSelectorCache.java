package me.itut.lanitium.internal.carpet;

import carpet.script.value.EntityValue;
import net.minecraft.commands.arguments.selector.EntitySelector;

import java.util.Map;

public interface EntityValueSelectorCache {
    Map<String, EntitySelector> selectorCache = ((EntityValueSelectorCache)new EntityValue(null)).lanitium$selectorCache();

    Map<String, EntitySelector> lanitium$selectorCache();
}

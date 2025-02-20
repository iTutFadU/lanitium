package me.itut.lanitium.mixin.carpet;

import carpet.script.value.EntityValue;
import me.itut.lanitium.internal.carpet.EntityValueSelectorCache;
import net.minecraft.commands.arguments.selector.EntitySelector;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

@Mixin(value = EntityValue.class, remap = false)
public abstract class EntityValueMixin implements EntityValueSelectorCache {
    @Shadow(remap = false) @Final
    private static Map<String, EntitySelector> selectorCache;

    @Override
    public Map<String, EntitySelector> lanitium$selectorCache() {
        return selectorCache;
    }
}

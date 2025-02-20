package me.itut.lanitium.internal;

import carpet.script.value.Value;
import net.minecraft.commands.CommandSourceStack;

import java.util.Map;

public interface CommandSourceStackCustomValues {
    Map<Value, Value> lanitium$customValues();
    void lanitium$setCustomValues(Map<Value, Value> values);
    CommandSourceStack lanitium$withCustomValues(Map<Value, Value> values);
}

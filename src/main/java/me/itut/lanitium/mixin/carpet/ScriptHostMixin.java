package me.itut.lanitium.mixin.carpet;

import carpet.script.Module;
import carpet.script.ScriptHost;
import me.itut.lanitium.internal.carpet.ScriptHostInterface;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

@Mixin(value = ScriptHost.class, remap = false)
public abstract class ScriptHostMixin implements ScriptHostInterface {
    @Shadow @Final private Map<String, Module> modules;
    @Shadow @Final private Map<Module, ScriptHost.ModuleData> moduleData;

    @Override
    public Map<String, Module> lanitium$modules() {
        return modules;
    }

    @Override
    public Map<Module, ScriptHost.ModuleData> lanitium$moduleData() {
        return moduleData;
    }
}

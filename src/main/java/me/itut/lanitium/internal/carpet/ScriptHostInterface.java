package me.itut.lanitium.internal.carpet;

import carpet.script.Module;
import carpet.script.ScriptHost;

import java.util.Map;

public interface ScriptHostInterface {
    Map<String, Module> lanitium$modules();
    Map<Module, ScriptHost.ModuleData> lanitium$moduleData();
}

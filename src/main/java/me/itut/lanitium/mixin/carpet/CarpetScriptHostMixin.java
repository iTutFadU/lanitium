package me.itut.lanitium.mixin.carpet;

import carpet.script.CarpetScriptHost;
import carpet.script.CarpetScriptServer;
import carpet.script.command.CommandArgument;
import carpet.script.value.MapValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import me.itut.lanitium.internal.carpet.CommandParser;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Mixin(value = CarpetScriptHost.class, remap = false)
public abstract class CarpetScriptHostMixin {
    @Shadow
    public Map<Value, Value> appConfig;
    @Shadow
    boolean hasCommand;

    @Shadow
    public abstract CarpetScriptServer scriptServer();

    @Inject(method = "addAppCommands", at = @At(value = "INVOKE", target = "Lcarpet/script/CarpetScriptHost;readCommands(Ljava/util/function/Predicate;)Lcom/mojang/brigadier/builder/LiteralArgumentBuilder;"), cancellable = true)
    private void brokenNeck(Consumer<Component> notifier, CallbackInfoReturnable<Boolean> cir) throws CommandSyntaxException {
        if (!appConfig.getOrDefault(StringValue.of("brigadier"), Value.FALSE).getBoolean()) return;

        cir.setReturnValue(false);
        Value rawBranches = appConfig.get(StringValue.of("commands"));
        if (rawBranches == null)
            return;
        if (!(rawBranches instanceof MapValue m))
            throw CommandArgument.error("'commands' element in config should be a map");
        Map<Value, Value> branchesMap = m.getMap();
        Map<String, Value> branches = new HashMap<>(branchesMap.size());
        branchesMap.forEach((k, v) -> branches.put(k.getString(), v));

        Value rawPermissions = appConfig.get(StringValue.of("command_permission"));
        Map<String, Value> permissions;
        if (rawPermissions == null) permissions = new HashMap<>();
        else if (rawPermissions instanceof MapValue p) {
            Map<Value, Value> permissionsMap = p.getMap();
            permissions = new HashMap<>(permissionsMap.size());
            permissionsMap.forEach((k, v) -> permissions.put(k.getString(), v));
        } else permissions = new HashMap<>(1) {{ put("", rawPermissions); }};

        final CommandNode<CommandSourceStack> command;
        try {
            command = CommandParser.parseCommand((CarpetScriptHost)(Object)this, branches, permissions);
        } catch (CommandSyntaxException e) {
            throw CommandArgument.error(e.getMessage());
        }
        scriptServer().server.getCommands().getDispatcher().getRoot().addChild(command);
        cir.setReturnValue(hasCommand = true);
    }
}

package me.itut.lanitium.mixin.carpet;

import carpet.script.CarpetScriptHost;
import carpet.script.command.CommandArgument;
import carpet.script.external.Carpet;
import carpet.script.value.FunctionValue;
import carpet.script.value.MapValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import me.itut.lanitium.internal.carpet.CommandArgumentInterface;
import me.itut.lanitium.value.ValueConversions;
import net.minecraft.commands.CommandSourceStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Mixin(value = CommandArgument.class, remap = false)
public abstract class CommandArgumentMixin implements CommandArgumentInterface {
    @Unique
    private Value customSuggestions;

    @Shadow
    protected abstract Value getValueFromContext(CommandContext<CommandSourceStack> context, String param) throws CommandSyntaxException;

    @Inject(method = "suggest", at = @At("HEAD"), cancellable = true)
    private void customSuggest(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder, CarpetScriptHost host, CallbackInfoReturnable<CompletableFuture<Suggestions>> cir) throws CommandSyntaxException {
        if (customSuggestions == null) return;
        Runnable currentSection = Carpet.startProfilerSection("Scarpet command");
        try {
            Value suggestions = customSuggestions;
            if (suggestions instanceof FunctionValue fn) {
                Map<Value, Value> params = new HashMap<>();
                for (ParsedCommandNode<CommandSourceStack> parsed : context.getNodes()) {
                    CommandNode<CommandSourceStack> node = parsed.getNode();
                    if (node instanceof ArgumentCommandNode) {
                        params.put(StringValue.of(node.getName()), CommandArgument.getValue(context, node.getName(), host));
                    }
                }
                List<Value> args = List.of(MapValue.wrap(params));
                suggestions = host.handleCommand(context.getSource(), fn, args);
            }
            cir.setReturnValue(CompletableFuture.completedFuture(ValueConversions.toSuggestions(builder, suggestions)));
        } finally { // Resources must be freed, Carpet!
            currentSection.run();
        }
    }

    @Inject(method = "configure", at = @At("HEAD"), cancellable = true)
    private void customConfigure(Map<String, Value> config, CarpetScriptHost host, CallbackInfo ci) {
        if (config.containsKey("suggestions")) {
            customSuggestions = config.get("suggestions");
            ci.cancel(); // No 'suggest' / 'suggester'
        }
    }

    @Override
    public Value lanitium$getValueFromContext(CommandContext<CommandSourceStack> context, String param) throws CommandSyntaxException {
        return getValueFromContext(context, param);
    }
}

package me.itut.lanitium.internal.carpet;

import carpet.script.CarpetScriptHost;
import carpet.script.command.CommandArgument;
import carpet.script.value.Value;
import carpet.script.value.ValueConversions;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;

import java.util.Collections;
import java.util.function.Function;
import java.util.function.Supplier;

public class VanillaArgument extends CommandArgument { // cloned
    @FunctionalInterface
    public interface ValueExtractor {
        Value apply(CommandContext<CommandSourceStack> ctx, String param) throws CommandSyntaxException;
    }

    @FunctionalInterface
    public interface ArgumentProvider {
        ArgumentType<?> get() throws CommandSyntaxException;
    }

    @FunctionalInterface
    public interface ArgumentProviderEx {
        ArgumentType<?> get(CommandBuildContext regAccess) throws CommandSyntaxException;
    }

    private final ArgumentProvider argumentTypeSupplier;
    private final ArgumentProviderEx argumentTypeSupplierEx;
    private final ValueExtractor valueExtractor;
    private final boolean providesExamples;

    public VanillaArgument(
        String suffix,
        ArgumentProvider argumentTypeSupplier,
        ValueExtractor valueExtractor,
        boolean suggestFromExamples
    ) {
        super(suffix, null, suggestFromExamples);
        try {
            this.examples = argumentTypeSupplier.get().getExamples();
        } catch (CommandSyntaxException e) {
            this.examples = Collections.emptyList();
        }
        this.providesExamples = suggestFromExamples;
        this.argumentTypeSupplier = argumentTypeSupplier;
        this.valueExtractor = valueExtractor;
        this.argumentTypeSupplierEx = null;
    }

    public VanillaArgument(
        String suffix,
        ArgumentProvider argumentTypeSupplier,
        ValueExtractor valueExtractor,
        SuggestionProvider<CommandSourceStack> suggester
    ) {
        super(suffix, Collections.emptyList(), false);
        this.suggestionProvider = param -> suggester;
        this.providesExamples = false;
        this.argumentTypeSupplier = argumentTypeSupplier;
        this.valueExtractor = valueExtractor;
        this.argumentTypeSupplierEx = null;
    }

    public VanillaArgument(
        String suffix,
        ArgumentProviderEx argumentTypeSupplier,
        ValueExtractor valueExtractor,
        boolean suggestFromExamples,
        MinecraftServer server
    ) {
        super(suffix, null, suggestFromExamples);
        try {
            this.examples = argumentTypeSupplier.get(CommandBuildContext.simple(server.registryAccess(), server.getWorldData().enabledFeatures())).getExamples();
        } catch (CommandSyntaxException e) {
            this.examples = Collections.emptyList();
        }
        this.providesExamples = suggestFromExamples;
        this.argumentTypeSupplierEx = argumentTypeSupplier;
        this.valueExtractor = valueExtractor;
        this.argumentTypeSupplier = null;
    }

    public VanillaArgument(
        String suffix,
        ArgumentProviderEx argumentTypeSupplier,
        ValueExtractor valueExtractor,
        SuggestionProvider<CommandSourceStack> suggester
    ) {
        super(suffix, Collections.emptyList(), false);
        this.suggestionProvider = param -> suggester;
        this.providesExamples = false;
        this.argumentTypeSupplierEx = argumentTypeSupplier;
        this.valueExtractor = valueExtractor;
        this.argumentTypeSupplier = null;
    }

    public VanillaArgument(
        String suffix,
        ArgumentProviderEx argumentTypeSupplier,
        ValueExtractor valueExtractor,
        Function<String, SuggestionProvider<CommandSourceStack>> suggesterGen
    ) {
        super(suffix, Collections.emptyList(), false);
        this.suggestionProvider = suggesterGen;
        this.providesExamples = false;
        this.argumentTypeSupplierEx = argumentTypeSupplier;
        this.valueExtractor = valueExtractor;
        this.argumentTypeSupplier = null;
    }

    public <T> VanillaArgument(
        String suffix,
        ResourceKey<Registry<T>> registry
    ) {
        this(
            suffix,
            c -> ResourceArgument.resource(c, registry),
            (c, p) -> ValueConversions.of(ResourceArgument.getResource(c, p, registry).key()),
            (c, b) -> SharedSuggestionProvider.suggestResource(c.getSource().getServer().registryAccess().lookupOrThrow(registry).keySet(), b)
        );
    }

    @Override
    protected ArgumentType<?> getArgumentType(CarpetScriptHost host) throws CommandSyntaxException {
        return argumentTypeSupplier != null
            ? argumentTypeSupplier.get()
            : argumentTypeSupplierEx.get(CommandBuildContext.simple(host.scriptServer().server.registryAccess(), host.scriptServer().server.getWorldData().enabledFeatures()));
    }

    @Override
    protected Value getValueFromContext(CommandContext<CommandSourceStack> context, String param) throws CommandSyntaxException {
        return valueExtractor.apply(context, param);
    }

    @Override
    protected Supplier<CommandArgument> factory(MinecraftServer server) {
        return argumentTypeSupplier != null
            ? (() -> new VanillaArgument(getTypeSuffix(), argumentTypeSupplier, valueExtractor, providesExamples))
            : (() -> new VanillaArgument(getTypeSuffix(), argumentTypeSupplierEx, valueExtractor, providesExamples, server));
    }
}

package me.itut.lanitium;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import carpet.script.CarpetExpression;
import carpet.script.annotation.AnnotationParser;
import carpet.script.annotation.ValueCaster;
import me.itut.lanitium.config.Config;
import me.itut.lanitium.config.ConfigManager;
import me.itut.lanitium.value.ByteBufferValue;
import me.itut.lanitium.value.FutureValue;
import me.itut.lanitium.value.Lazy;
import me.mrnavastar.biscuit.api.Biscuit;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Lanitium implements ModInitializer, CarpetExtension {
	public static final Logger LOGGER = LoggerFactory.getLogger("Lanitium");
	public static final ConfigManager CONFIG_MANAGER = new ConfigManager(FabricLoader.getInstance().getConfigDir().resolve("lanitium.json").toFile());
	public static Config CONFIG;
	public static final Biscuit.RegisteredCookie COOKIE = Biscuit.register(ResourceLocation.fromNamespaceAndPath("lanitium", "cookie"), LanitiumCookie.class);

	@Override
	public void onInitialize() {
		CONFIG_MANAGER.load();
		CONFIG = CONFIG_MANAGER.config();

		CarpetServer.manageExtension(this);
		ValueCaster.register(Lazy.class, "lazy");
		ValueCaster.register(FutureValue.class, "future");
		ValueCaster.register(ByteBufferValue.class, "byte_buffer");
        AnnotationParser.parseFunctionClass(LanitiumFunctions.class);

		registerCommand();
        LOGGER.info("Yummy cookies! {}", Emoticons.getRandomEmoticon());
	}

	@Override
	public void scarpetApi(CarpetExpression expression) {
        LanitiumFunctions.apply(expression.getExpr());
	}

	private void registerCommand() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(Commands.literal("lanitium")
			.requires(source -> source.hasPermission(2))
			.then(Commands.literal("reload")
				.executes(ctx -> {
					CONFIG_MANAGER.load();
					CONFIG = CONFIG_MANAGER.config();
					ctx.getSource().sendSuccess(() -> Component.literal("Lanitium configuration reloaded"), true);
					return 1;
				})
			)
		));
	}
}
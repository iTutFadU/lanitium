package me.itut.lanitium;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import carpet.script.CarpetExpression;
import carpet.script.annotation.AnnotationParser;
import carpet.script.annotation.ValueCaster;
import me.itut.lanitium.config.Config;
import me.itut.lanitium.config.ConfigManager;
import me.mrnavastar.biscuit.api.Biscuit;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.loader.api.FabricLoader;
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
		ValueCaster.register(ContextValue.class, "context");
		ValueCaster.register(LanitiumCookieFuture.class, "lanitium_cookie_future");
        AnnotationParser.parseFunctionClass(LanitiumFunctions.class);
		LOGGER.info("Yummy cookies! >u<");
	}

	@Override
	public void scarpetApi(CarpetExpression expression) {
        LanitiumFunctions.apply(expression.getExpr());
	}
}
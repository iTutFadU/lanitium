package me.itut.lanitium;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import carpet.script.CarpetExpression;
import carpet.script.annotation.AnnotationParser;
import carpet.script.annotation.ValueCaster;
import com.mojang.authlib.GameProfile;
import me.mrnavastar.biscuit.api.Biscuit;
import net.fabricmc.api.ModInitializer;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Lanitium implements ModInitializer, CarpetExtension {
	public static final Logger LOGGER = LoggerFactory.getLogger("Lanitium");
	public static final Biscuit.RegisteredCookie COOKIE = Biscuit.register(ResourceLocation.fromNamespaceAndPath("lanitium", "cookie"), LanitiumCookie.class);

    public static Component MOTD = null;
	public static Integer PLAYERS_ONLINE = null, PLAYERS_MAX = null;
	public static List<GameProfile> PLAYERS_SAMPLE = null;

	@Override
	public void onInitialize() {
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
package me.itut.lanitium;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import carpet.script.CarpetExpression;
import carpet.script.annotation.AnnotationParser;
import carpet.script.annotation.SimpleTypeConverter;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.Value;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import me.itut.lanitium.config.Config;
import me.itut.lanitium.config.ConfigManager;
import me.itut.lanitium.function.*;
import me.itut.lanitium.value.ByteBufferValue;
import me.mrnavastar.biscuit.api.Biscuit;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.ServerOpList;
import net.minecraft.server.players.ServerOpListEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Collection;

public class Lanitium implements ModInitializer, CarpetExtension {
	public static final Logger LOGGER = LoggerFactory.getLogger("Lanitium");
	public static final ConfigManager CONFIG_MANAGER = new ConfigManager(FabricLoader.getInstance().getConfigDir().resolve("lanitium.json").toFile());
	public static Config CONFIG;
	public static final Biscuit.RegisteredCookie COOKIE = Biscuit.register(ResourceLocation.fromNamespaceAndPath("lanitium", "cookie"), LanitiumCookie.class);

	@Override
	public void onInitialize() {
		CONFIG = CONFIG_MANAGER.load();

		CarpetServer.manageExtension(this);

		SimpleTypeConverter.registerType(Value.class, ByteBuffer.class, v -> {
			try {
				return ByteBufferValue.from(v);
			} catch (InternalExpressionException ignored) {
				return null;
			}
		}, "byte buffer");

        AnnotationParser.parseFunctionClass(Apply.class);
        AnnotationParser.parseFunctionClass(DataStructures.class);
        AnnotationParser.parseFunctionClass(Encoding.class);
        AnnotationParser.parseFunctionClass(Parsing.class);
        AnnotationParser.parseFunctionClass(Protocol.class);
        AnnotationParser.parseFunctionClass(Server.class);
        AnnotationParser.parseFunctionClass(Symbols.class);

		registerCommands();
        LOGGER.info("Yummy cookies! {}", Emoticons.getRandomEmoticon());
	}

	@Override
	public void scarpetApi(CarpetExpression expr) {
		Apply.apply(expr.getExpr());
        Patterns.apply(expr.getExpr());
	}

	private void registerCommands() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("lanitium")
				.requires(source -> source.hasPermission(2))
				.then(Commands.literal("reload")
					.executes(ctx -> {
						CONFIG = CONFIG_MANAGER.load();
						ctx.getSource().sendSuccess(() -> Component.literal("Lanitium configuration reloaded"), true);
						return 1;
					})
				);

			if (environment.includeDedicated) {
				class PermissionSubCommand { // <_< ...
					private static final SimpleCommandExceptionType
						ERROR_NOT_OP = new SimpleCommandExceptionType(Component.translatable("commands.deop.failed")),
						ERROR_ALREADY_OP = new SimpleCommandExceptionType(Component.translatable("commands.op.failed"));

					private static int execute(CommandContext<CommandSourceStack> ctx, boolean bypassesPlayerLimit) throws CommandSyntaxException {
                        Collection<GameProfile> targets = GameProfileArgument.getGameProfiles(ctx, "targets");
						int level = IntegerArgumentType.getInteger(ctx, "level");

						CommandSourceStack source = ctx.getSource();
						PlayerList playerList = ctx.getSource().getServer().getPlayerList();
						int i = 0;

						if (level == 0 && !bypassesPlayerLimit) {
							for (GameProfile player : targets) {
                                if (!playerList.isOp(player)) continue;
                                playerList.deop(player);
                                i++;
                                source.sendSuccess(() -> Component.translatable("commands.deop.success", player.getName()), true);
                            }

							if (i == 0) throw ERROR_NOT_OP.create();
						} else {
                            ServerOpList ops = playerList.getOps();
							for (GameProfile player : targets) {
								ServerOpListEntry entry = ops.get(player);
								if (entry != null && (entry.getLevel() != level || entry.getBypassesPlayerLimit() != bypassesPlayerLimit)) ops.remove(entry);

								if (entry == null || entry.getLevel() != level || entry.getBypassesPlayerLimit() != bypassesPlayerLimit) {
									ops.add(new ServerOpListEntry(player, level, bypassesPlayerLimit));
									if (entry == null || entry.getLevel() != level) {
                                        ServerPlayer p = playerList.getPlayer(player.getId());
                                        if (p != null) playerList.sendPlayerPermissionLevel(p);
									}
								} else continue;

								i++;
								source.sendSuccess(() -> Component.translatable("commands.op.success", player.getName()).append(" (level " + level + (bypassesPlayerLimit ? ", bypasses player limit)" : ")")), true);
							}

							if (i == 0) throw ERROR_ALREADY_OP.create();
						}

						if (level == 0) source.getServer().kickUnlistedPlayers(source);
						return i;
					}
				}

				command.then(Commands.literal("permission")
					.requires(s -> s.hasPermission(4))
					.then(Commands.argument("targets", GameProfileArgument.gameProfile())
						.suggests((ctx, builder) -> {
							PlayerList playerList = ctx.getSource().getServer().getPlayerList();
							return SharedSuggestionProvider.suggest(playerList.getPlayers().stream().filter(p -> !playerList.isOp(p.getGameProfile())).map(p -> p.getGameProfile().getName()), builder);
						})
						.then(Commands.argument("level", IntegerArgumentType.integer(0, 4))
							.then(Commands.argument("bypasses_player_limit", BoolArgumentType.bool())
								.executes(ctx -> PermissionSubCommand.execute(ctx, BoolArgumentType.getBool(ctx, "bypasses_player_limit")))
							)
							.executes(ctx -> PermissionSubCommand.execute(ctx, false))
						)
					)
				);
			}

			dispatcher.register(command);
		});
	}
}
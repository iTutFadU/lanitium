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
import me.itut.lanitium.value.CollisionContextValue;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.*;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.ServerOpList;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Collection;

public class Lanitium implements ModInitializer, CarpetExtension {
    public static final Logger LOGGER = LoggerFactory.getLogger("Lanitium");
    public static final ConfigManager CONFIG_MANAGER = new ConfigManager(FabricLoader.getInstance().getConfigDir().resolve("lanitium.json").toFile());
    public static Config CONFIG;
//  public static final Biscuit.RegisteredCookie COOKIE = Biscuit.register(ResourceLocation.fromNamespaceAndPath("lanitium", "cookie"), LanitiumCookie.class);

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
        SimpleTypeConverter.registerType(Value.class, CollisionContext.class, v -> {
            try {
                return CollisionContextValue.from(v);
            } catch (InternalExpressionException ignored) {
                return null;
            }
        }, "collision context");

        AnnotationParser.parseFunctionClass(Apply.class);
        AnnotationParser.parseFunctionClass(DataStructures.class);
        AnnotationParser.parseFunctionClass(Encoding.class);
        AnnotationParser.parseFunctionClass(Parsing.class);
//        AnnotationParser.parseFunctionClass(Protocol.class);
        AnnotationParser.parseFunctionClass(Server.class);
        AnnotationParser.parseFunctionClass(Symbols.class);
        AnnotationParser.parseFunctionClass(World.class);

        registerCommands();
        LOGGER.info("Yummy cookies! {}", Emoticons.getRandomEmoticon());
    }

    @Override
    public void scarpetApi(CarpetExpression expr) {
        Apply.apply(expr.getExpr());
        Patterns.apply(expr.getExpr());
    }

    @Override
    public void onGameStarted() {
        LanitiumEvent ignored = LanitiumEvent.PLAYER_CUSTOM_CLICK;
    }

    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("lanitium")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
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
                        Collection<NameAndId> targets = GameProfileArgument.getGameProfiles(ctx, "targets");
                        int level = IntegerArgumentType.getInteger(ctx, "level");
                        LevelBasedPermissionSet permissions = LevelBasedPermissionSet.forLevel(PermissionLevel.byId(level));

                        CommandSourceStack source = ctx.getSource();
                        PlayerList playerList = ctx.getSource().getServer().getPlayerList();
                        int i = 0;

                        if (level == 0 && !bypassesPlayerLimit) {
                            for (NameAndId player : targets) {
                                if (!playerList.isOp(player)) continue;
                                playerList.deop(player);
                                i++;
                                source.sendSuccess(() -> Component.translatable("commands.deop.success", player.name()), true);
                            }

                            if (i == 0) throw ERROR_NOT_OP.create();
                        } else {
                            ServerOpList ops = playerList.getOps();
                            for (NameAndId player : targets) {
                                ServerOpListEntry entry = ops.get(player);
                                if (entry != null && (entry.permissions().level().id() != level || entry.getBypassesPlayerLimit() != bypassesPlayerLimit)) ops.remove(entry);

                                if (entry == null || entry.permissions().level().id() != level || entry.getBypassesPlayerLimit() != bypassesPlayerLimit) {
                                    ops.add(new ServerOpListEntry(player, permissions, bypassesPlayerLimit));
                                    if (entry == null || entry.permissions().level().id() != level) {
                                        ServerPlayer p = playerList.getPlayer(player.id());
                                        if (p != null) playerList.sendPlayerPermissionLevel(p);
                                    }
                                } else continue;

                                i++;
                                source.sendSuccess(() -> Component.translatable("commands.op.success", player.name()).append(" (level " + level + (bypassesPlayerLimit ? ", bypasses player limit)" : ")")), true);
                            }

                            if (i == 0) throw ERROR_ALREADY_OP.create();
                        }

                        if (level == 0) source.getServer().kickUnlistedPlayers();
                        return i;
                    }
                }

                command.then(Commands.literal("permission")
                    .requires(Commands.hasPermission(Commands.LEVEL_OWNERS))
                    .then(Commands.argument("targets", GameProfileArgument.gameProfile())
                        .suggests((ctx, builder) -> {
                            PlayerList playerList = ctx.getSource().getServer().getPlayerList();
                            return SharedSuggestionProvider.suggest(playerList.getPlayers().stream().filter(p -> !playerList.isOp(p.nameAndId())).map(p -> p.nameAndId().name()), builder);
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
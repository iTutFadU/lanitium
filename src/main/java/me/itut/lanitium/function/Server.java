package me.itut.lanitium.function;

import carpet.script.CarpetContext;
import carpet.script.Context;
import carpet.script.annotation.ScarpetFunction;
import carpet.script.value.BooleanValue;
import carpet.script.value.FormattedTextValue;
import carpet.script.value.Value;
import carpet.utils.CommandHelper;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import me.itut.lanitium.Lanitium;
import me.itut.lanitium.value.FutureValue;
import me.itut.lanitium.value.ValueConversions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.stream.Stream;

import static net.minecraft.Util.NIL_UUID;

public class Server {
    @ScarpetFunction(maxParams = 1)
    public static void display_server_motd(Optional<Value> motd) {
        Lanitium.CONFIG.displayMotd = motd.map(FormattedTextValue::getTextByValue).orElse(null);
    }

    @ScarpetFunction(maxParams = 1)
    public static void display_server_players_online(Optional<Integer> current) {
        Lanitium.CONFIG.displayPlayersOnline = current.orElse(null);
    }

    @ScarpetFunction(maxParams = 1)
    public static void display_server_players_max(Optional<Integer> max) {
        Lanitium.CONFIG.displayPlayersMax = max.orElse(null);
    }

    @ScarpetFunction(maxParams = -1)
    public static void display_server_players_sample(String... players) {
        Lanitium.CONFIG.displayPlayersSampleProfiles = Stream.of(players).map(v -> new GameProfile(NIL_UUID, v)).toList();
    }

    @ScarpetFunction
    public static void display_server_players_sample_default() {
        Lanitium.CONFIG.displayPlayersSampleProfiles = null;
    }

    @ScarpetFunction
    public static void set_server_tps(Context c, double tps) {
        ((CarpetContext)c).server().tickRateManager().setTickRate((float)tps);
    }

    @ScarpetFunction
    public static void set_server_frozen(Context c, boolean frozen) {
        ((CarpetContext)c).server().tickRateManager().setFrozen(frozen);
    }

    @ScarpetFunction
    public static void server_sprint(Context c, int ticks) {
        ((CarpetContext)c).server().tickRateManager().requestGameToSprint(ticks);
    }

    @ScarpetFunction(maxParams = 2)
    public static void send_success(Context c, Value message, Optional<Boolean> broadcast) {
        ((CarpetContext)c).source().sendSuccess(() -> FormattedTextValue.getTextByValue(message), broadcast.orElse(false));
    }

    @ScarpetFunction
    public static void send_failure(Context c, Value message) {
        ((CarpetContext)c).source().sendFailure(FormattedTextValue.getTextByValue(message));
    }

    @ScarpetFunction
    public static void send_system_message(Context c, Value message) {
        ((CarpetContext)c).source().sendSystemMessage(FormattedTextValue.getTextByValue(message));
    }

    @ScarpetFunction(maxParams = -1)
    public static void send_commands_update(Context c, ServerPlayer... players) {
        MinecraftServer server = ((CarpetContext)c).server();
        if (players.length == 0) CommandHelper.notifyPlayersCommandsChanged(server);
        else server.schedule(new TickTask(server.getTickCount(), () -> {
            for (ServerPlayer player : players) server.getCommands().sendCommands(player);
        }));
    }

    @ScarpetFunction // system_info('source_permission') >= level
    public static Value has_permission(Context c, int level) {
        return BooleanValue.of(((CarpetContext)c).source().hasPermission(level));
    }

    @ScarpetFunction
    public static Value command_suggestions(Context c, String command) {
        CommandDispatcher<CommandSourceStack> dispatcher = ((CarpetContext)c).server().getCommands().getDispatcher();
        return FutureValue.of((CarpetContext)c, dispatcher.getCompletionSuggestions(dispatcher.parse(command, ((CarpetContext)c).source())).thenApply(ValueConversions::suggestions));
    }
}

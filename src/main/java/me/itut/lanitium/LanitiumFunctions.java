package me.itut.lanitium;

import carpet.script.Module;
import carpet.script.*;
import carpet.script.annotation.Param;
import carpet.script.annotation.ScarpetFunction;
import carpet.script.argument.BlockArgument;
import carpet.script.argument.FunctionArgument;
import carpet.script.exception.*;
import carpet.script.language.Operators;
import carpet.script.value.*;
import carpet.utils.CommandHelper;
import com.google.gson.JsonParseException;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.RootCommandNode;
import me.itut.lanitium.internal.CommandSourceStackInterface;
import me.itut.lanitium.value.*;
import me.itut.lanitium.value.ValueConversions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static me.itut.lanitium.internal.carpet.SystemInfoInterface.options;
import static me.itut.lanitium.value.Lazy.*;
import static me.itut.lanitium.value.ValueConversions.*;
import static net.minecraft.Util.NIL_UUID;

public class LanitiumFunctions {
    static {
        options.put("server_tps", c -> new NumericValue(c.server().tickRateManager().tickrate()));
        options.put("server_frozen", c -> BooleanValue.of(c.server().tickRateManager().isFrozen()));
        options.put("server_sprinting", c -> BooleanValue.of(c.server().tickRateManager().isSprinting()));
        options.put("source_anchor", c -> c.source().getAnchor() == EntityAnchorArgument.Anchor.EYES ? EYES : FEET);
        options.put("source_permission", c -> NumericValue.of(((CommandSourceStackInterface)c.source()).lanitium$permissionLevel()));
        options.put("source_custom_values", c -> {
            Map<Value, Value> map = ((CommandSourceStackInterface)c.source()).lanitium$customValues();
            return map != null ? MapValue.wrap(map) : Value.NULL;
        });
    }

    public static FunctionValue findIn(Context c, Module module, Value functionValue) {
        if (functionValue.isNull()) throw new InternalExpressionException("function argument cannot be null");
        else if (!(functionValue instanceof FunctionValue fun)) {
            String name = functionValue.getString();
            return c.host.getAssertFunction(module, name);
        } else return fun;
    }

    public static void apply(Expression expression) {
        // Carpet nasty nasty
        final Fluff.ILazyFunction call;
        expression.addCustomFunction("call", call = new Fluff.AbstractLazyFunction(-1, "call") {
            @Override
            public LazyValue lazyEval(Context c, Context.Type t, Expression expr, Tokenizer.Token tok, List<LazyValue> lv) {
                if (lv.isEmpty()) {
                    throw new InternalExpressionException("'call' expects at least function name to call");
                } else if (t != Context.SIGNATURE) {
                    FunctionValue fun = findIn(c, expression.module, lv.getFirst().evalValue(c));
                    List<Value> args;
                    if (fun instanceof LazyFunctionValue)
                        args = LazyFunctionValue.wrapArgs(lv.subList(1, lv.size()), c, t);
                    else args = Fluff.AbstractLazyFunction.unpackLazy(lv.subList(1, lv.size()), c, Context.NONE);
                    return fun.callInContext(c, t, args);
                } else {
                    String name = lv.getFirst().evalValue(c, Context.NONE).getString();
                    List<String> args = new ArrayList<>();
                    List<String> globals = new ArrayList<>();
                    String varArgs = null;

                    for (int i = 1; i < lv.size(); ++i) {
                        Value v = lv.get(i).evalValue(c, Context.LOCALIZATION);
                        if (!v.isBound()) {
                            throw new InternalExpressionException("Only variables can be used in function signature, not  " + v.getString());
                        }

                        if (v instanceof FunctionAnnotationValue fav) {
                            if (fav.type == FunctionAnnotationValue.Type.GLOBAL) {
                                globals.add(v.boundVariable);
                            } else {
                                if (varArgs != null) {
                                    throw new InternalExpressionException("Variable argument identifier is already defined as " + varArgs + ", trying to overwrite with " + v.boundVariable);
                                }

                                varArgs = v.boundVariable;
                            }
                        } else {
                            args.add(v.boundVariable);
                        }
                    }

                    Value output = new FunctionSignatureValue(name, args, varArgs, globals);
                    return (cc, tt) -> output;
                }
            }

            @Override
            public boolean pure() {
                return false;
            }

            @Override
            public boolean transitive() {
                return false;
            }

            @Override
            public Context.Type staticType(Context.Type outerType) {
                return outerType == Context.SIGNATURE ? Context.LOCALIZATION : Context.NONE;
            }
        });
        expression.addLazyFunction("z", (c, t, lv) -> {
            ListValue output = ListValue.wrap(lv.stream().map(v -> new Lazy(c, t, v)));
            return (cc, tt) -> output;
        });

        expression.addLazyUnaryOperator("\\", Operators.precedence.get("unary+-!..."), false, false, type -> type, (c, t, lv) -> {
            if (t == Context.SIGNATURE) {
                Value v = lv.evalValue(c, t);
                if (v instanceof FunctionSignatureValue signature) {
                    LazyFunctionSignatureValue lazy = LazyFunctionSignatureValue.of(signature);
                    return (cc, tt) -> lazy;
                }
            }
            Lazy lazy = new Lazy(c, t, lv);
            return (cc, tt) -> lazy;
        });
        expression.addLazyBinaryOperator("\\", Operators.precedence.get("attribute~:"), true, false, type -> type, (c, t, l, r) -> {
            Value left = l.evalValue(c, t);
            if (left instanceof WithValue with)
                return with.with(c, t, r);
            Value right = r.evalValue(c, t);
            if (!(right instanceof ListValue args) || !args.getItems().stream().allMatch(v -> v instanceof Lazy))
                throw new InternalExpressionException("Incorrect list of arguments. To call a function, use func\\z(...args)");
            LazyValue[] values = args.getItems().stream().map(v -> (LazyValue)((Lazy)v)::eval).toArray(LazyValue[]::new), callArgs = new LazyValue[values.length + 1];
            callArgs[0] = (cc, tt) -> left;
            System.arraycopy(values, 0, callArgs, 1, values.length);
            return call.lazyEval(c, t, expression, Tokenizer.Token.NONE, List.of(callArgs));
        });
        expression.addPureLazyFunction("all_then", -1, t -> Context.VOID, (c, t, lv) -> {
            int imax = lv.size() - 1;
            Throwable error = null;

            for (int i = 0; i < imax; i++)
                try {
                    lv.get(i).evalValue(c, Context.VOID);
                } catch (Throwable e) {
                    error = e;
                }

            Value v = lv.get(imax).evalValue(c, t);
            if (error instanceof Error e)
                throw e;
            else if (error instanceof RuntimeException e)
                throw e;
            return (cc, tt) -> v;
        });
        expression.addContextFunction("thread_local", -1, (c, t, lv) -> {
            if (lv.isEmpty())
                throw new InternalExpressionException("'thread_local' requires at least a function to call");
            FunctionValue initial = FunctionArgument.findIn(c, expression.module, lv, 0, true, false).function;
            return new ThreadLocalValue(c, initial);
        });
        expression.addPureLazyFunction("catch_all", 1, type -> type, (c, t, lv) -> {
            try {
                Value output = lv.getFirst().evalValue(c, t);
                return (cc, tt) -> output;
            } catch (Throwable e) {
                return LazyValue.NULL;
            }
        });

        expression.addLazyFunction("as_entity", 2, (c, t, lv) -> {
            final CommandSourceStack source = ((CarpetContext)c).source();
            if (!(lv.getFirst().evalValue(c) instanceof EntityValue entity)) throw new InternalExpressionException("First argument to 'as_entity' must be an entity");
            if (entity.getEntity().equals(source.getEntity())) return lv.get(1);
            final Context ctx = c.recreate();
            ((CarpetContext)ctx).swapSource(source.withEntity(entity.getEntity()));
            ctx.variables = c.variables;
            final Value output = lv.get(1).evalValue(ctx);
            return (cc, tt) -> output;
        });
        expression.addLazyFunction("positioned", 2, (c, t, lv) -> {
            final CommandSourceStack source = ((CarpetContext)c).source();
            if (!(lv.getFirst().evalValue(c) instanceof ListValue b)) throw new InternalExpressionException("First argument to 'positioned' must be a list");
            final List<Value> bl = b.getItems();
            final Vec3 pos = new Vec3(NumericValue.asNumber(bl.get(0)).getDouble(), NumericValue.asNumber(bl.get(1)).getDouble(), NumericValue.asNumber(bl.get(2)).getDouble());
            if (pos.equals(source.getPosition())) return lv.get(1);
            final Context ctx = c.recreate();
            ((CarpetContext)ctx).swapSource(source.withPosition(pos));
            ctx.variables = c.variables;
            final Value output = lv.get(1).evalValue(ctx);
            return (cc, tt) -> output;
        });
        expression.addLazyFunction("rotated", 2, (c, t, lv) -> {
            final CommandSourceStack source = ((CarpetContext)c).source();
            if (!(lv.getFirst().evalValue(c) instanceof ListValue b)) throw new InternalExpressionException("First argument to 'rotated' must be a list");
            final List<Value> bl = b.getItems();
            final Vec2 rot = new Vec2(NumericValue.asNumber(bl.get(0)).getFloat(), NumericValue.asNumber(bl.get(1)).getFloat());
            if (rot.equals(source.getRotation())) return lv.get(1);
            final Context ctx = c.recreate();
            ((CarpetContext)ctx).swapSource(source.withRotation(rot));
            ctx.variables = c.variables;
            final Value output = lv.get(1).evalValue(ctx);
            return (cc, tt) -> output;
        });
        expression.addLazyFunction("anchored", 2, (c, t, lv) -> {
            final CommandSourceStack source = ((CarpetContext)c).source();
            final String name = lv.getFirst().evalValue(c).getString();
            final EntityAnchorArgument.Anchor anchor = switch (name) {
                case "eyes" -> EntityAnchorArgument.Anchor.EYES;
                case "feet" -> EntityAnchorArgument.Anchor.FEET;
                default -> throw new ThrowStatement(name, Throwables.VALUE_EXCEPTION);
            };
            if (source.getAnchor() == anchor) return lv.get(1);
            Context ctx = c.recreate();
            ((CarpetContext)ctx).swapSource(source.withAnchor(anchor));
            ctx.variables = c.variables;
            Value output = lv.get(1).evalValue(ctx);
            return (cc, tt) -> output;
        });
        expression.addLazyFunction("elevated", 2, (c, t, lv) -> {
            final CommandSourceStack source = ((CarpetContext)c).source();
            final int level = NumericValue.asNumber(lv.getFirst().evalValue(c)).getInt();
            if (source.hasPermission(level)) return lv.get(1);
            Context ctx = c.recreate();
            ((CarpetContext)ctx).swapSource(source.withPermission(level));
            ctx.variables = c.variables;
            Value output = lv.get(1).evalValue(ctx);
            return (cc, tt) -> output;
        });
        expression.addLazyFunction("with_permission", 2, (c, t, lv) -> {
            final CommandSourceStack source = ((CarpetContext)c).source();
            final int level = NumericValue.asNumber(lv.getFirst().evalValue(c)).getInt();
            final CommandSourceStack newSource = source.withPermission(level);
            if (source == newSource) return lv.get(1);
            Context ctx = c.recreate();
            ((CarpetContext)ctx).swapSource(newSource);
            ctx.variables = c.variables;
            Value output = lv.get(1).evalValue(ctx);
            return (cc, tt) -> output;
        });
        expression.addLazyFunction("with_custom_values", 2, (c, t, lv) -> {
            final CommandSourceStack source = ((CarpetContext)c).source();
            final MapValue customValues = switch (lv.getFirst().evalValue(c)) {
                case NullValue ignored -> null;
                case MapValue map -> map;
                case AbstractListValue list -> new MapValue(list.unpack());
                default -> throw new InternalExpressionException("Argument 'custom_values' must be a map");
            };
            if (customValues == null || customValues.getMap().isEmpty()) return lv.get(1);
            Context ctx = c.recreate();
            ((CarpetContext)ctx).swapSource(((CommandSourceStackInterface)source).lanitium$withCustomValues(customValues.getMap()));
            ctx.variables = c.variables;
            Value output = lv.get(1).evalValue(ctx);
            return (cc, tt) -> output;
        });
    }

    // TODO: Revisit cookies
    @ScarpetFunction
    public static Value cookie(Context c, ServerPlayer player, FunctionValue callback) {
        return FutureValue.of((CarpetContext)c, player.getCookie(LanitiumCookie.class).handle((cookie, exception) -> {
            MapValue map = null;

            if (cookie != null) {
                Map<Value, Value> values = new HashMap<>();
                cookie.cookie.forEach((k, v) -> values.put(StringValue.of(k), NBTSerializableValue.of(v)));
                map = MapValue.wrap(values);
            }

            final Value mapValue = map != null ? map : Value.NULL;
            List<Value> args = new ArrayList<>() {{
                add(EntityValue.of(player));
                add(mapValue);
            }};
            boolean set = false;
            try {
                Value out = callback.callInContext(c, Context.STRING, args).evalValue(c, Context.STRING);
                if ("set".equals(out.getString())) set = true;
            } finally {
                if (set)
                    if (map == null)
                        player.setCookie(LanitiumCookie.EMPTY);
                    else {
                        Map<String, Tag> newCookie = new HashMap<>();
                        map.getMap().forEach((k, v) -> newCookie.put(k.getString(), v.toTag(true, ((CarpetContext)c).registryAccess())));
                        player.setCookie(new LanitiumCookie(newCookie));
                    }
            }

            return mapValue;
        }));
    }

    @ScarpetFunction
    public static void cookie_reset(ServerPlayer player) {
        player.setCookie(LanitiumCookie.EMPTY);
    }

    @ScarpetFunction
    public static void cookie_secret(String secret) {
        Lanitium.COOKIE.setSecret(secret);
    }

    @SuppressWarnings("ConstantValue")
    @ScarpetFunction(maxParams = 4)
    public static Value lazy_call(Lazy lazy, @Param.KeyValuePairs(allowMultiparam = false) Map<String, Value> vars, Optional<Lazy> c, Optional<String> t) {
        Context context = c.map(values -> values.context).orElseGet(() -> lazy.context);
        Context.Type type;
        try {
            type = t.map(s -> Context.Type.valueOf(s.toUpperCase())).orElseGet(() -> lazy.type);
        } catch (IllegalArgumentException ignored) {
            throw new InternalExpressionException("Unknown context type: " + t.get());
        }

        LazyValue initialLazy = context.getVariable("@");
        context.setVariable("@", (cc, tt) -> lazy.reboundedTo("@"));
        Map<String, LazyValue> originals = new HashMap<>();
        for (Map.Entry<String, Value> entry : vars.entrySet()) {
            String key = entry.getKey();
            originals.put(key, context.getVariable(key));
            Value value = entry.getValue();
            context.setVariable(key, (cc, tt) -> value.reboundedTo(key));
        }

        try {
            return lazy.lazy.evalValue(context, type);
        } catch (BreakStatement e) {
            throw new ThrowStatement(e.retval != null ? e.retval : NULL, BREAK_ERROR);
        } catch (ContinueStatement e) {
            throw new ThrowStatement(e.retval != null ? e.retval : NULL, CONTINUE_ERROR);
        } catch (ReturnStatement e) {
            throw new ThrowStatement(e.retval != null ? e.retval : NULL, RETURN_ERROR);
        } finally {
            for (Map.Entry<String, LazyValue> entry : originals.entrySet())
                if (entry.getValue() != null)
                    context.setVariable(entry.getKey(), entry.getValue());
                else
                    context.delVariable(entry.getKey());
            if (initialLazy != null)
                context.setVariable("@", initialLazy);
            else
                context.delVariable("@");
        }
    }

    @ScarpetFunction
    public static void strict(Context c, boolean value) {
        c.host.strict = value;
    }

    public static class CustomIterator extends LazyListValue {
        private final Context context;
        private final FunctionValue hasNext, next, reset;
        public final Value state;

        public CustomIterator(Context context, FunctionValue hasNext, FunctionValue next, FunctionValue reset, Value state) {
            this.context = context;
            this.hasNext = hasNext;
            this.next = next;
            this.reset = reset;
            this.state = state;
        }

        @Override
        public boolean hasNext() {
            return hasNext.callInContext(context, Context.BOOLEAN, List.of(state)).evalValue(context).getBoolean();
        }

        @Override
        public Value next() {
            try {
                return next.callInContext(context, Context.NONE, List.of(state)).evalValue(context);
            } catch (ProcessedThrowStatement e) {
                throw new NoSuchElementException(e.getMessage());
            }
        }

        @Override
        public void reset() {
            reset.callInContext(context, Context.VOID, List.of(state));
        }

        @Override
        public Value deepcopy() {
            return new CustomIterator(context, hasNext, next, reset, state.deepcopy());
        }
    }

    @ScarpetFunction
    public static Value iterator(Context c, FunctionValue hasNext, FunctionValue next, FunctionValue reset, Value state) {
        return new CustomIterator(c, hasNext, next, reset, state);
    }

    @ScarpetFunction
    public static Value symbol() {
        return new Symbol();
    }

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

    @ScarpetFunction
    public static Value emoticon() {
        return StringValue.of(Emoticons.getRandomEmoticon());
    }

    @ScarpetFunction
    public static Value emoticons_list() {
        return ListValue.wrap(Emoticons.list.stream().map(StringValue::of));
    }

    @ScarpetFunction
    public static Value format_json(Context c, String value) {
        try {
            return FormattedTextValue.deserialize(value, ((CarpetContext)c).registryAccess());
        } catch (JsonParseException e) {
            throw new ThrowStatement(e.getMessage(), Throwables.JSON_ERROR);
        }
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

    @ApiStatus.Experimental
    @ScarpetFunction(maxParams = -1)
    public static void send_empty_commands(Context c, ServerPlayer... players) {
        MinecraftServer server = ((CarpetContext)c).server();
        RootCommandNode<SharedSuggestionProvider> empty = new RootCommandNode<>();
        server.schedule(new TickTask(server.getTickCount(), () -> {
            for (ServerPlayer player : players) player.connection.send(new ClientboundCommandsPacket(empty));
        }));
    }

    @ScarpetFunction
    public static Value has_permission(Context c, int level) {
        return BooleanValue.of(((CarpetContext)c).source().hasPermission(level));
    }

    @ScarpetFunction
    public static Value char_to_int(String str) {
        if (str.isEmpty()) return Value.NULL;
        return NumericValue.of((int)str.charAt(0));
    }

    @ScarpetFunction
    public static Value int_to_char(int i) {
        return StringValue.of(String.valueOf((char)i));
    }

    @ScarpetFunction
    public static Value name_of_code_point(String str) {
        if (str.isEmpty()) return Value.NULL;
        return StringValue.of(Character.getName(str.charAt(0)));
    }

    @ScarpetFunction
    public static Value code_point_of_name(String name) {
        try {
            return StringValue.of(String.valueOf(Character.codePointOf(name)));
        } catch (IllegalArgumentException e) {
            return Value.NULL;
        }
    }

    @ScarpetFunction(maxParams = 2)
    public static Value parse_signed_int(String str, Optional<Integer> radix) {
        try {
            return NumericValue.of(Long.parseLong(str, radix.orElse(10)));
        } catch (NumberFormatException e) {
            return Value.NULL;
        }
    }

    @ScarpetFunction(maxParams = 2)
    public static Value parse_unsigned_int(String str, Optional<Integer> radix) {
        try {
            return NumericValue.of(Long.parseUnsignedLong(str, radix.orElse(10)));
        } catch (NumberFormatException e) {
            return Value.NULL;
        }
    }

    @ScarpetFunction
    public static Value encode_bytes(String data) {
        return ByteBufferValue.of(ByteBuffer.wrap(data.getBytes(StandardCharsets.UTF_8)));
    }

    @ScarpetFunction
    public static Value decode_bytes(ByteBufferValue data) {
        return StringValue.of(new String(data.buffer.array(), StandardCharsets.UTF_8));
    }

    @ScarpetFunction
    public static Value encode_nbt_bytes(Context c, NBTSerializableValue data) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            NbtIo.writeAnyTag(data.getTag(), new DataOutputStream(stream));
        } catch (IOException e) {
            throw new ThrowStatement(e.getMessage(), Throwables.IO_EXCEPTION);
        }
        return ByteBufferValue.of(ByteBuffer.wrap(stream.toByteArray()));
    }

    @ScarpetFunction
    public static Value decode_nbt_bytes(ByteBufferValue data) {
        ByteArrayInputStream stream = new ByteArrayInputStream(data.buffer.array());
        try {
            return NBTSerializableValue.of(NbtIo.readAnyTag(new DataInputStream(stream), NbtAccounter.unlimitedHeap()));
        } catch (IOException e) {
            throw new ThrowStatement(e.getMessage(), Throwables.IO_EXCEPTION);
        }
    }

    @ScarpetFunction
    public static Value encode_compressed_compound(Context c, NBTSerializableValue data) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            NbtIo.writeCompressed(data.getCompoundTag(), new DataOutputStream(stream));
        } catch (IOException e) {
            throw new ThrowStatement(e.getMessage(), Throwables.IO_EXCEPTION);
        }
        return ByteBufferValue.of(ByteBuffer.wrap(stream.toByteArray()));
    }

    @ScarpetFunction
    public static Value decode_compressed_compound(ByteBufferValue data) {
        ByteArrayInputStream stream = new ByteArrayInputStream(data.buffer.array());
        try {
            return NBTSerializableValue.of(NbtIo.readCompressed(new DataInputStream(stream), NbtAccounter.unlimitedHeap()));
        } catch (IOException e) {
            throw new ThrowStatement(e.getMessage(), Throwables.IO_EXCEPTION);
        }
    }

    @ScarpetFunction(maxParams = 2)
    public static Value pretty_nbt(Context c, NBTSerializableValue nbt, Optional<String> indent) {
        return FormattedTextValue.of(new TextComponentTagVisitor(indent.orElse("")).visit(nbt.getTag()));
    }

    @ScarpetFunction
    public static Value nbt_type(NBTSerializableValue nbt) {
        return StringValue.of(nbt.getTag().getType().getName().toLowerCase());
    }

    @ScarpetFunction
    public static Value nbt_list_type(NBTSerializableValue nbt) {
        if (!(nbt.getTag() instanceof ListTag list)) return Value.NULL;
        return StringValue.of(TagTypes.getType(list.getElementType()).getName().toLowerCase());
    }

    @ScarpetFunction(maxParams = -1)
    public static Value byte_buffer(int... values) {
        byte[] arr = new byte[values.length];
        IntStream.range(0, arr.length).forEach(i -> arr[i] = (byte)values[i]);
        return ByteBufferValue.of(ByteBuffer.wrap(arr));
    }

    @ScarpetFunction
    public static Value string_reader(Context c, Value reader) {
        return StringReaderValue.of((CarpetContext)c, StringReaderValue.from(reader));
    }

    @ScarpetFunction
    public static Value command_suggestions(Context c, String command) {
        CommandDispatcher<CommandSourceStack> dispatcher = ((CarpetContext)c).server().getCommands().getDispatcher();
        return FutureValue.of((CarpetContext)c, dispatcher.getCompletionSuggestions(dispatcher.parse(command, ((CarpetContext)c).source())).thenApply(ValueConversions::suggestions));
    }

    @ScarpetFunction(maxParams = 1)
    public static Value future(Context c, Optional<Value> completed) {
        return FutureValue.of((CarpetContext)c, completed.map(CompletableFuture::completedFuture).orElseGet(CompletableFuture::new));
    }

    @ScarpetFunction
    public static Value allocate_list(int capacity) {
        return ListValue.wrap(new ArrayList<>(capacity));
    }

    @ScarpetFunction
    public static Value allocate_map(int capacity) {
        return MapValue.wrap(new HashMap<>(capacity));
    }

    @ScarpetFunction
    public static Value allocate_byte_buffer(int capacity) {
        return ByteBufferValue.of(ByteBuffer.allocate(capacity));
    }

    @ScarpetFunction
    public static Value possible_block_states(Context c, Value value) {
        ServerLevel level = ((CarpetContext)c).level();
        return ListValue.wrap(BlockArgument.findIn((CarpetContext)c, List.of(value), 0, true).block.getBlockState().getBlock().getStateDefinition().getPossibleStates().stream().map(s -> new BlockValue(s, level, (BlockPos)null)));
    }

    @ScarpetFunction
    public static Value block_state_map(Context c, Value value) {
        Collection<Property<?>> properties = BlockArgument.findIn((CarpetContext)c, List.of(value), 0, true).block.getBlockState().getBlock().getStateDefinition().getProperties();
        Map<Value, Value> map = new HashMap<>(properties.size());
        properties.forEach(p -> map.put(StringValue.of(p.getName()), ListValue.wrap(p.getPossibleValues().stream().map(v -> StringValue.of(v instanceof StringRepresentable str ? str.getSerializedName() : v.toString())))));
        return MapValue.wrap(map);
    }

    @ScarpetFunction // https://github.com/gnembon/fabric-carpet/pull/1996
    public static Value item_components(Context c, Value item) {
        ItemStack stack = carpet.script.value.ValueConversions.getItemStackFromValue(item, true, ((CarpetContext)c).registryAccess());
        DataComponentMap components = stack.getComponents().filter(t -> !t.isTransient());
        Map<Value, Value> map = new HashMap<>(components.size());
        components.forEach(v -> map.put(carpet.script.value.ValueConversions.of(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(v.type())), switch (v.value()) {
            case Boolean bool -> BooleanValue.of(bool);
            case Number number -> NumericValue.of(number);
            case Component component -> FormattedTextValue.of(component);
            default -> NBTSerializableValue.of(v.encodeValue(NbtOps.INSTANCE).result().orElse(null));
        }));
        return MapValue.wrap(map);
    }
    
//    @ScarpetFunction
//    public static void send_game_packet(Context c, EntityValue p, String type, Value... values) {
//        CarpetContext context = (CarpetContext)c;
//        ServerPlayer player = EntityValue.getPlayerByValue(context.server(), p);
//        ServerPlayerConnection connection = player.connection;
//        switch (type) {
//            default -> throw new InternalExpressionException("Unknown packet type: " + type);
//        }
//    }
}

package me.itut.lanitium;

import carpet.script.*;
import carpet.script.Module;
import carpet.script.annotation.Param;
import carpet.script.annotation.ScarpetFunction;
import carpet.script.argument.FunctionArgument;
import carpet.script.exception.*;
import carpet.script.language.Operators;
import carpet.script.utils.SystemInfo;
import carpet.script.value.*;
import com.google.gson.JsonParseException;
import com.mojang.authlib.GameProfile;
import me.itut.lanitium.internal.carpet.SystemInfoOptionsGetter;
import me.itut.lanitium.value.*;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static me.itut.lanitium.value.Lazy.*;

@SuppressWarnings({"unused", "OptionalUsedAsFieldOrParameterType"})
public class LanitiumFunctions {
    static {
        Operators.precedence.put("with\\", 100);

        @SuppressWarnings("InstantiationOfUtilityClass")
        final Map<String, Function<CarpetContext, Value>> options = ((SystemInfoOptionsGetter)new SystemInfo()).lanitium$getOptions();
        options.put("server_tps", c -> new NumericValue(c.server().tickRateManager().tickrate()));
        options.put("server_frozen", c -> BooleanValue.of(c.server().tickRateManager().isFrozen()));
        options.put("server_sprinting", c -> BooleanValue.of(c.server().tickRateManager().isSprinting()));
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
                    else args = Fluff.AbstractFunction.unpackLazy(lv.subList(1, lv.size()), c, Context.NONE);
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
        expression.addLazyBinaryOperator("\\", Operators.precedence.get("with\\"), true, false, type -> type, (c, t, l, r) -> {
            Value left = l.evalValue(c, t);
            if (left instanceof WithValue with)
                return with.with(r);
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
            CommandSourceStack source = ((CarpetContext)c).source();
            if (!(lv.getFirst().evalValue(c) instanceof EntityValue entity)) throw new InternalExpressionException("First argument to 'as_entity' must be an entity");
            if (entity.getEntity().equals(source.getEntity())) return lv.get(1);
            Context ctx = c.recreate();
            ((CarpetContext)ctx).swapSource(source.withEntity(entity.getEntity()));
            ctx.variables = c.variables;
            Value output = lv.get(1).evalValue(ctx);
            return (cc, tt) -> output;
        });
        expression.addLazyFunction("positioned", 2, (c, t, lv) -> {
            CommandSourceStack source = ((CarpetContext)c).source();
            if (!(lv.getFirst().evalValue(c) instanceof ListValue b)) throw new InternalExpressionException("First argument to 'positioned' must be a list");
            List<Value> bl = b.getItems();
            Vec3 pos = new Vec3(NumericValue.asNumber(bl.get(0)).getDouble(), NumericValue.asNumber(bl.get(1)).getDouble(), NumericValue.asNumber(bl.get(2)).getDouble());
            if (pos.equals(source.getPosition())) return lv.get(1);
            Context ctx = c.recreate();
            ((CarpetContext)ctx).swapSource(source.withPosition(pos));
            ctx.variables = c.variables;
            Value output = lv.get(1).evalValue(ctx);
            return (cc, tt) -> output;
        });
        expression.addLazyFunction("rotated", 2, (c, t, lv) -> {
            CommandSourceStack source = ((CarpetContext)c).source();
            if (!(lv.getFirst().evalValue(c) instanceof ListValue b)) throw new InternalExpressionException("First argument to 'rotated' must be a list");
            List<Value> bl = b.getItems();
            Vec2 rot = new Vec2(NumericValue.asNumber(bl.get(0)).getFloat(), NumericValue.asNumber(bl.get(1)).getFloat());
            if (rot.equals(source.getRotation())) return lv.get(1);
            Context ctx = c.recreate();
            ((CarpetContext)ctx).swapSource(source.withRotation(rot));
            ctx.variables = c.variables;
            Value output = lv.get(1).evalValue(ctx);
            return (cc, tt) -> output;
        });
    }

    @ScarpetFunction
    public LanitiumCookieFuture cookie(Context c, EntityValue p, FunctionValue callback) {
        CarpetContext context = (CarpetContext)c;
        ServerPlayer player = EntityValue.getPlayerByValue(context.server(), p);
        CarpetScriptServer server = ((CarpetScriptServer)context.host.scriptServer());
        return new LanitiumCookieFuture(context, player.getCookie(LanitiumCookie.class).whenComplete((cookie, exception) -> {
            MapValue map = null;
            boolean set = false;

            if (cookie != null) {
                Map<Value, Value> values = new HashMap<>();
                for (Map.Entry<String, Tag> e : cookie.cookie.entrySet())
                    values.put(StringValue.of(e.getKey()), NBTSerializableValue.of(e.getValue()));
                map = MapValue.wrap(values);
            }

            try {
                List<Value> args = new ArrayList<>();
                args.add(p);
                args.add(map);
                Value out = server.getAppHostByName(context.host.getName()).callNow(callback, args);
                if ("set".equals(out.getString())) set = true;
            } finally {
                if (set)
                    if (map == null)
                        player.setCookie(LanitiumCookie.EMPTY);
                    else {
                        Map<String, Tag> newCookie = new HashMap<>();
                        for (Map.Entry<Value, Value> e : map.getMap().entrySet())
                            newCookie.put(e.getKey().getString(), ((NBTSerializableValue)NBTSerializableValue.fromValue(e.getValue())).getTag());
                        player.setCookie(new LanitiumCookie(newCookie));
                    }
            }
        }));
    }

    @ScarpetFunction
    public void cookie_reset(Context c, EntityValue p) {
        CarpetContext context = (CarpetContext)c;
        ServerPlayer player = EntityValue.getPlayerByValue(context.server(), p);
        player.setCookie(LanitiumCookie.EMPTY);
    }

    @ScarpetFunction
    public void cookie_secret(String secret) {
        Lanitium.COOKIE.setSecret(secret);
    }

    @SuppressWarnings("ConstantValue")
    @ScarpetFunction(maxParams = 4)
    public Value lazy_call(Lazy lazy, @Param.KeyValuePairs(allowMultiparam = false) Map<String, Value> vars, Optional<ContextValue> c, Optional<String> t) {
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
    public void strict(Context c, boolean value) {
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
            return next.callInContext(context, Context.NONE, List.of(state)).evalValue(context);
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
    public Value iterator(Context c, FunctionValue has_next, FunctionValue next, FunctionValue reset, Value state) {
        return new CustomIterator(c, has_next, next, reset, state);
    }

    @ScarpetFunction
    public Value symbol() {
        return new Symbol();
    }

    @ScarpetFunction(maxParams = 1)
    public void display_server_motd(Optional<Value> motd) {
        Lanitium.CONFIG.displayMotd = motd.map(FormattedTextValue::getTextByValue).orElse(null);
    }

    @ScarpetFunction(maxParams = 1)
    public void display_server_players_online(Optional<Integer> current) {
        Lanitium.CONFIG.displayPlayersOnline = current.orElse(null);
    }

    @ScarpetFunction(maxParams = 1)
    public void display_server_players_max(Optional<Integer> max) {
        Lanitium.CONFIG.displayPlayersMax = max.orElse(null);
    }

    @ScarpetFunction(maxParams = -1)
    public void display_server_players_sample(Value... players) {
        Lanitium.CONFIG.displayPlayersSampleProfiles = Stream.of(players).map(v -> new GameProfile(Util.NIL_UUID, v.getString())).toList();
    }

    @ScarpetFunction
    public void display_server_players_sample_default() {
        Lanitium.CONFIG.displayPlayersSampleProfiles = null;
    }

    @ScarpetFunction
    public void set_server_tps(Context c, double tps) {
        ((CarpetContext)c).server().tickRateManager().setTickRate((float)tps);
    }

    @ScarpetFunction
    public void set_server_frozen(Context c, boolean frozen) {
        ((CarpetContext)c).server().tickRateManager().setFrozen(frozen);
    }

    @ScarpetFunction
    public void server_sprint(Context c, int ticks) {
        ((CarpetContext)c).server().tickRateManager().requestGameToSprint(ticks);
    }

    @ScarpetFunction
    public Value emoticon() {
        return StringValue.of(Emoticons.getRandomEmoticon());
    }

    @ScarpetFunction
    public Value emoticons_list() {
        return ListValue.wrap(Emoticons.list.stream().map(StringValue::of));
    }

    @ScarpetFunction
    public Value format_json(Context c, String value) {
        try {
            return FormattedTextValue.deserialize(value, ((CarpetContext)c).registryAccess());
        } catch (JsonParseException e) {
            throw new ThrowStatement(e.getMessage(), Throwables.JSON_ERROR);
        }
    }

    @ScarpetFunction(maxParams = 2)
    public void send_success(Context c, Value message, Optional<Boolean> broadcast) {
        Component component = FormattedTextValue.getTextByValue(message);
        ((CarpetContext)c).source().sendSuccess(() -> component, broadcast.orElse(false));
    }

    @ScarpetFunction
    public void send_failure(Context c, Value message) {
        ((CarpetContext)c).source().sendFailure(FormattedTextValue.getTextByValue(message));
    }

    @ScarpetFunction
    public void send_system_message(Context c, Value message) {
        ((CarpetContext)c).source().sendSystemMessage(FormattedTextValue.getTextByValue(message));
    }

//    @ScarpetFunction
//    public void send_game_packet(Context c, EntityValue p, String type, Value... values) {
//        CarpetContext context = (CarpetContext)c;
//        ServerPlayer player = EntityValue.getPlayerByValue(context.server(), p);
//        ServerPlayerConnection connection = player.connection;
//        switch (type) {
//            default -> throw new InternalExpressionException("Unknown packet type: " + type);
//        }
//    }
}

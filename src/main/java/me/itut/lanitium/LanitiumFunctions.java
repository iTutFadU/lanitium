package me.itut.lanitium;

import carpet.script.*;
import carpet.script.Module;
import carpet.script.annotation.Param;
import carpet.script.annotation.ScarpetFunction;
import carpet.script.argument.Argument;
import carpet.script.argument.FunctionArgument;
import carpet.script.exception.*;
import carpet.script.value.*;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

import static carpet.script.annotation.ScarpetFunction.UNLIMITED_PARAMS;
import static me.itut.lanitium.Lazy.*;

public class LanitiumFunctions {
    public static FunctionValue findIn(Context c, Module module, Value functionValue) {
        if (functionValue.isNull()) throw new InternalExpressionException("function argument cannot be null");
        else if (!(functionValue instanceof FunctionValue fun)) {
            String name = functionValue.getString();
            return c.host.getAssertFunction(module, name);
        } else return fun;
    }

    public static void apply(Expression expression) {
        // Carpet nasty nasty
        expression.addCustomFunction("call", new Fluff.AbstractLazyFunction(-1, "call") {
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
                    List<String> argsx = new ArrayList<>();
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
                            argsx.add(v.boundVariable);
                        }
                    }

                    Value retval = new FunctionSignatureValue(name, argsx, varArgs, globals);
                    return (cc, tt) -> retval;
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

        expression.addLazyFunction("lazy", (c, t, lv) -> {
            if (t == Context.SIGNATURE) {
                Value v = lv.getFirst().evalValue(c, t);
                if (v instanceof FunctionSignatureValue signature) {
                    LazyFunctionSignatureValue lazy = LazyFunctionSignatureValue.of(signature);
                    return (cc, tt) -> lazy;
                }
            }
            Lazy lazy = new Lazy(c, t, lv.isEmpty() ? LazyValue.NULL : lv.getFirst());
            return (cc, tt) -> lazy;
        });
    }

    @ScarpetFunction
    public LanitiumCookieFuture cookie(Context c, EntityValue p, FunctionValue callback) {
        CarpetContext context = (CarpetContext)c;
        ServerPlayer player = EntityValue.getPlayerByValue(context.server(), p);
        CarpetScriptServer server = ((CarpetScriptServer)context.host.scriptServer());
        return new LanitiumCookieFuture(player.getCookie(LanitiumCookie.class).whenComplete((cookie, exception) -> {
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
            return lazy.value.evalValue(context, type);
        } catch (BreakStatement e) {
            throw new ThrowStatement(e.retval != null ? e.retval : NULL, BREAK_ERROR);
        } catch (ContinueStatement e) {
            throw new ThrowStatement(e.retval != null ? e.retval : NULL, CONTINUE_ERROR);
        } catch (ReturnStatement e) {
            throw new ThrowStatement(e.retval != null ? e.retval : NULL, RETURN_ERROR);
        } finally {
            for (Map.Entry<String, LazyValue> entry : originals.entrySet())
                context.setVariable(entry.getKey(), entry.getValue());
            context.setVariable("@", initialLazy);
        }
    }

    @ScarpetFunction
    public void strict(Context c, boolean value) {
        c.host.strict = value;
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

package me.itut.lanitium.mixin.carpet;

import carpet.script.Tokenizer;
import me.itut.lanitium.internal.carpet.TokenInterface;
import me.itut.lanitium.internal.carpet.TokenTypeInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

@Mixin(value = Tokenizer.Token.class, remap = false)
public abstract class TokenMixin implements TokenInterface {
    @Unique
    private static MethodHandle morphedInto, morph, typeValueOf;
    @Unique
    private static VarHandle type;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void init(CallbackInfo ci) {
        if (morphedInto == null) try {
            Class<?> tokenTypeClass = Tokenizer.Token.class.getDeclaredClasses()[0];

            Method method = Tokenizer.Token.class.getDeclaredMethod("morphedInto", tokenTypeClass, String.class);
            method.setAccessible(true);
            morphedInto = MethodHandles.lookup().unreflect(method);

            method = Tokenizer.Token.class.getDeclaredMethod("morph", tokenTypeClass, String.class);
            method.setAccessible(true);
            morph = MethodHandles.lookup().unreflect(method);

            method = tokenTypeClass.getDeclaredMethod("valueOf", String.class);
            method.setAccessible(true);
            typeValueOf = MethodHandles.lookup().unreflect(method);

            Field field = Tokenizer.Token.class.getDeclaredField("type");
            field.setAccessible(true);
            type = MethodHandles.lookup().unreflectVarHandle(field);
        } catch (NoSuchMethodException | IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public TokenTypeInterface lanitium$byName(String name) {
        try {
            return (TokenTypeInterface)typeValueOf.invoke(name);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public TokenTypeInterface lanitium$type() {
        return (TokenTypeInterface)type.get((Tokenizer.Token)(Object)this);
    }

    @Override
    public void lanitium$setType(TokenTypeInterface newType) {
        type.set((Tokenizer.Token)(Object)this, newType);
    }

    @Override
    public Tokenizer.Token lanitium$morphedInto(TokenTypeInterface newType, String newSurface) {
        try {
            return (Tokenizer.Token)morphedInto.invoke((Tokenizer.Token)(Object)this, newType, newSurface);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void lanitium$morph(TokenTypeInterface type, String s) {
        try {
            morph.invoke((Tokenizer.Token)(Object)this, type, s);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}

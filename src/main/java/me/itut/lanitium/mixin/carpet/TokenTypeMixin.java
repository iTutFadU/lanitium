package me.itut.lanitium.mixin.carpet;

import me.itut.lanitium.internal.carpet.TokenTypeInterface;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "carpet/script/Tokenizer$Token$TokenType", remap = false)
public abstract class TokenTypeMixin implements TokenTypeInterface {
    @Shadow @Final boolean functional;
    @Shadow @Final boolean constant;

    @Override
    public boolean lanitium$functional() {
        return functional;
    }

    @Override
    public boolean lanitium$constant() {
        return constant;
    }
}

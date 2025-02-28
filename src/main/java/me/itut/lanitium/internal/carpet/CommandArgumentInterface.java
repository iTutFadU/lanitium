package me.itut.lanitium.internal.carpet;

import carpet.script.value.Value;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;

public interface CommandArgumentInterface {
    Value lanitium$getValueFromContext(CommandContext<CommandSourceStack> context, String param) throws CommandSyntaxException;
}

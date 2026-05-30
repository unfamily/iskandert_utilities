package net.unfamily.iskautils.arcane;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public record ArcaneDictionaryEffectContext(
        ServerPlayer player,
        ItemStack dictionaryStack,
        Identifier traitId,
        int level) {}

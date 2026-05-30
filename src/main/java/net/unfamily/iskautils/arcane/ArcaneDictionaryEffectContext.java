package net.unfamily.iskautils.arcane;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public record ArcaneDictionaryEffectContext(
        ServerPlayer player,
        ItemStack dictionaryStack,
        ResourceLocation traitId,
        int level) {}

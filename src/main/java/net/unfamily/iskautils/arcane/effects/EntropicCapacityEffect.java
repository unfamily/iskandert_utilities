package net.unfamily.iskautils.arcane.effects;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.arcane.ArcaneDictionaryContents;
import net.unfamily.iskautils.arcane.ArcaneDictionaryEffect;
import net.unfamily.iskautils.arcane.ArcaneDictionaryEntryGate;
import net.unfamily.iskautils.arcane.ArcaneDictionaryTraitIds;
import net.unfamily.iskautils.arcane.jei.ArcaneDictionaryJeiContext;
import net.unfamily.iskautils.arcane.jei.ArcaneDictionaryJeiLines;

import java.util.List;

/** Capacity bonus is applied in {@link net.unfamily.iskautils.arcane.ArcaneDictionaryEntropy}. */
public final class EntropicCapacityEffect implements ArcaneDictionaryEffect {
    @Override
    public Identifier id() {
        return ArcaneDictionaryTraitIds.ENTROPIC_CAPACITY;
    }

    @Override
    public int defaultConsumePerLevel() {
        return 3;
    }

    public static int bonusStored(ItemStack dictionary, ServerPlayer player) {
        if (dictionary == null || player == null) {
            return 0;
        }
        int total = 0;
        for (ArcaneDictionaryContents.TraitSlot trait : ArcaneDictionaryContents.getTraits(dictionary)) {
            if (!ArcaneDictionaryTraitIds.ENTROPIC_CAPACITY.equals(trait.id())) {
                continue;
            }
            if (!ArcaneDictionaryEntryGate.traitActive(player, trait.id())) {
                continue;
            }
            total += trait.level() * Config.arcaneEntropicCapacityBonusStoredPerLevel;
        }
        return total;
    }

    @Override
    public void appendJeiDescription(ArcaneDictionaryJeiContext ctx, List<Component> lines) {
        ArcaneDictionaryJeiLines.appendScaled(
                ctx, lines, "capacity", level -> level * Config.arcaneEntropicCapacityBonusStoredPerLevel);
    }
}

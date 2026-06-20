package net.unfamily.iskautils.arcane.effects;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.arcane.ArcaneDictionaryActivation;
import net.unfamily.iskautils.arcane.ArcaneDictionaryContents;
import net.unfamily.iskautils.arcane.ArcaneDictionaryEntryGate;
import net.unfamily.iskautils.arcane.ArcaneDictionaryTraitIds;
import net.unfamily.iskautils.arcane.jei.ArcaneDictionaryJeiContext;
import net.unfamily.iskautils.arcane.jei.ArcaneDictionaryJeiLines;

import java.util.List;

public final class CurseOfUselessEffect {
    private CurseOfUselessEffect() {}

    public static boolean isBlocking(ServerPlayer player) {
        ItemStack dictionary = ArcaneDictionaryActivation.singleCurioDictionary(player);
        if (dictionary == null) {
            return false;
        }
        for (ArcaneDictionaryContents.TraitSlot trait : ArcaneDictionaryContents.getTraits(dictionary)) {
            if (!ArcaneDictionaryTraitIds.CURSE_OF_USELESS.equals(trait.id())) {
                continue;
            }
            if (!ArcaneDictionaryEntryGate.traitActive(player, trait.id())) {
                return false;
            }
            double chance = Math.min(
                    1.0D, trait.level() * Config.arcaneCurseOfUselessDisableChancePerLevel);
            return player.getRandom().nextDouble() < chance;
        }
        return false;
    }

    public static void appendJeiDescription(ArcaneDictionaryJeiContext ctx, List<Component> lines) {
        ArcaneDictionaryJeiLines.appendScaledPercent(
                ctx, lines, "disable", Config.arcaneCurseOfUselessDisableChancePerLevel);
    }
}

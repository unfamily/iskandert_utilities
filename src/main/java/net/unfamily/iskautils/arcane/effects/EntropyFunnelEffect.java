package net.unfamily.iskautils.arcane.effects;

import net.minecraft.network.chat.Component;
import net.unfamily.iskautils.arcane.ArcaneDictionaryEffect;
import net.unfamily.iskautils.arcane.ArcaneDictionaryTraitIds;
import net.unfamily.iskautils.arcane.jei.ArcaneDictionaryJeiContext;
import net.unfamily.iskautils.arcane.jei.ArcaneDictionaryJeiLines;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.integration.apotheosis.ApotheosisCompat;

import java.util.List;

/** Entropy absorb bonus is applied in {@link net.unfamily.iskautils.arcane.ArcaneDictionaryEntropy}. */
public final class EntropyFunnelEffect implements ArcaneDictionaryEffect {

    @Override
    public net.minecraft.resources.ResourceLocation id() {
        return ArcaneDictionaryTraitIds.ENTROPY_FUNNEL;
    }

    @Override
    public int defaultConsumePerLevel() {
        return 2;
    }

    @Override
    public void appendJeiDescription(ArcaneDictionaryJeiContext ctx, List<Component> lines) {
        ArcaneDictionaryJeiLines.appendScaled(
                ctx, lines, "bonus", level -> level * Config.arcaneEntropyFunnelBonusChargesPerLevel);
    }
}

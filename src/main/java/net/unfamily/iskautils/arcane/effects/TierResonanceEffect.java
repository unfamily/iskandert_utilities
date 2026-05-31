package net.unfamily.iskautils.arcane.effects;

import net.minecraft.network.chat.Component;
import net.unfamily.iskautils.arcane.ArcaneDictionaryEffect;
import net.unfamily.iskautils.arcane.ArcaneDictionaryTraitIds;
import net.unfamily.iskautils.arcane.jei.ArcaneDictionaryJeiContext;
import net.unfamily.iskautils.arcane.jei.ArcaneDictionaryJeiLines;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.integration.apotheosis.ApotheosisCompat;

import java.util.List;

/** Consume reduction is applied in {@link net.unfamily.iskautils.arcane.ArcaneDictionaryConsume}. */
public final class TierResonanceEffect implements ArcaneDictionaryEffect {

    @Override
    public net.minecraft.resources.ResourceLocation id() {
        return ArcaneDictionaryTraitIds.TIER_RESONANCE;
    }

    @Override
    public int defaultConsumePerLevel() {
        return 6;
    }

    @Override
    public void appendJeiDescription(ArcaneDictionaryJeiContext ctx, List<Component> lines) {
        if (!ApotheosisCompat.isLoaded()) {
            lines.add(Component.translatable("jei.iska_utils.arcane_trait.iska_utils.tier_resonance.requires_apotheosis"));
            return;
        }
        ArcaneDictionaryJeiLines.appendScaledPercent(
                ctx, lines, "consume", Config.arcaneTierResonanceConsumeReductionPerTierPerLevel * 4.0D);
        lines.add(Component.translatable("jei.iska_utils.arcane_trait.iska_utils.tier_resonance.scales_tier"));
    }
}

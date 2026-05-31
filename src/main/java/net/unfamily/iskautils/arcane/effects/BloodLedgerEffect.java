package net.unfamily.iskautils.arcane.effects;

import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.unfamily.iskautils.arcane.ArcaneDictionaryEffect;
import net.unfamily.iskautils.arcane.ArcaneDictionaryEffectContext;
import net.unfamily.iskautils.arcane.ArcaneDictionaryTraitIds;
import net.unfamily.iskautils.arcane.jei.ArcaneDictionaryJeiContext;
import net.unfamily.iskautils.arcane.jei.ArcaneDictionaryJeiLines;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.util.ArtifactTickIntervals;

import java.util.List;

public final class BloodLedgerEffect implements ArcaneDictionaryEffect {

    @Override
    public net.minecraft.resources.ResourceLocation id() {
        return ArcaneDictionaryTraitIds.BLOOD_LEDGER;
    }

    @Override
    public int defaultConsumePerLevel() {
        return 6;
    }

    @Override
    public void onPlayerTick(ArcaneDictionaryEffectContext ctx) {
        float ratio = ctx.player().getHealth() / Math.max(1.0F, ctx.player().getMaxHealth());
        if (ratio > Config.arcaneBloodLedgerLowHpRatio) {
            return;
        }
        ctx.player().addEffect(new MobEffectInstance(
                MobEffects.REGENERATION, ArtifactTickIntervals.POTION_DURATION_TICKS, 0, true, false, true));
    }

    @Override
    public void appendJeiDescription(ArcaneDictionaryJeiContext ctx, List<Component> lines) {
        ArcaneDictionaryJeiLines.appendTraitLine(
                ctx,
                lines,
                "regen",
                ctx.formatPercent(Config.arcaneBloodLedgerLowHpRatio * 100.0D));
    }
}

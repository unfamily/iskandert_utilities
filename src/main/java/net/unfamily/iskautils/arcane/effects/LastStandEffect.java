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

public final class LastStandEffect implements ArcaneDictionaryEffect {

    @Override
    public net.minecraft.resources.ResourceLocation id() {
        return ArcaneDictionaryTraitIds.LAST_STAND;
    }

    @Override
    public int defaultConsumePerLevel() {
        return 5;
    }

    @Override
    public void onPlayerTick(ArcaneDictionaryEffectContext ctx) {
        float ratio = ctx.player().getHealth() / Math.max(1.0F, ctx.player().getMaxHealth());
        if (ratio > Config.arcaneLastStandHpRatio) {
            return;
        }
        int amp = Math.max(0, ctx.level() - 1);
        ctx.player().addEffect(new MobEffectInstance(
                MobEffects.DAMAGE_RESISTANCE, ArtifactTickIntervals.POTION_DURATION_TICKS, amp, true, false, true));
    }

    @Override
    public void appendJeiDescription(ArcaneDictionaryJeiContext ctx, List<Component> lines) {
        ArcaneDictionaryJeiLines.appendTraitLine(
                ctx,
                lines,
                "resistance",
                ctx.formatPercent(Config.arcaneLastStandHpRatio * 100.0D));
    }
}

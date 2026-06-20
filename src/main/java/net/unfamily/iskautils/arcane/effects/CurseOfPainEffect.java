package net.unfamily.iskautils.arcane.effects;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.arcane.ArcaneDictionaryEffect;
import net.unfamily.iskautils.arcane.ArcaneDictionaryEffectContext;
import net.unfamily.iskautils.arcane.ArcaneDictionaryTraitIds;
import net.unfamily.iskautils.arcane.jei.ArcaneDictionaryJeiContext;
import net.unfamily.iskautils.arcane.jei.ArcaneDictionaryJeiLines;
import net.unfamily.iskautils.effect.ModMobEffects;

import java.util.List;

public final class CurseOfPainEffect implements ArcaneDictionaryEffect {
    @Override
    public Identifier id() {
        return ArcaneDictionaryTraitIds.CURSE_OF_PAIN;
    }

    @Override
    public int defaultConsumePerLevel() {
        return 4;
    }

    @Override
    public void onPlayerTick(ArcaneDictionaryEffectContext ctx) {
        int amp = Math.min(Config.arcaneCurseOfPainMaxAmplifier, Math.max(0, ctx.level() - 1));
        int duration = Config.arcaneCurseOfPainDurationSeconds * 20;
        ctx.player().addEffect(new MobEffectInstance(
                ModMobEffects.CURSE_OF_PAIN.getDelegate(),
                duration,
                amp,
                true,
                true,
                true));
    }

    @Override
    public void appendJeiDescription(ArcaneDictionaryJeiContext ctx, List<Component> lines) {
        ArcaneDictionaryJeiLines.appendTraitLine(
                ctx, lines, "duration", ctx.formatNumber(Config.arcaneCurseOfPainDurationSeconds));
    }
}

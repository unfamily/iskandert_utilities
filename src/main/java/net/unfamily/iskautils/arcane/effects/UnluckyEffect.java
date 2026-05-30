package net.unfamily.iskautils.arcane.effects;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.unfamily.iskautils.arcane.ArcaneDictionaryEffect;
import net.unfamily.iskautils.arcane.ArcaneDictionaryEffectContext;
import net.unfamily.iskautils.arcane.jei.ArcaneDictionaryJeiContext;
import net.unfamily.iskautils.arcane.jei.ArcaneDictionaryJeiLines;
import net.unfamily.iskautils.util.ArtifactTickIntervals;
import net.unfamily.iskautils.util.RomanNumerals;

import java.util.List;

public final class UnluckyEffect implements ArcaneDictionaryEffect {

    private final Identifier id;

    public UnluckyEffect(Identifier id) {
        this.id = id;
    }

    @Override
    public Identifier id() {
        return id;
    }

    @Override
    public void onPlayerTick(ArcaneDictionaryEffectContext ctx) {
        int amp = Math.max(0, ctx.level() - 1);
        ctx.player().addEffect(new MobEffectInstance(
                MobEffects.UNLUCK, ArtifactTickIntervals.POTION_DURATION_TICKS, amp, true, false, true));
    }

    @Override
    public void appendJeiDescription(ArcaneDictionaryJeiContext ctx, List<Component> lines) {
        ArcaneDictionaryJeiLines.appendTraitLine(
                ctx,
                lines,
                "effect",
                RomanNumerals.toRoman(ctx.minLevel()),
                RomanNumerals.toRoman(ctx.maxLevel()));
    }
}

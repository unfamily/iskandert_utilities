package net.unfamily.iskautils.arcane.effects;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.unfamily.iskautils.arcane.ArcaneDictionaryAttributes;
import net.unfamily.iskautils.arcane.ArcaneDictionaryEffect;
import net.unfamily.iskautils.arcane.ArcaneDictionaryEffectContext;
import net.unfamily.iskautils.arcane.ArcaneDictionaryTraitIds;
import net.unfamily.iskautils.arcane.jei.ArcaneDictionaryJeiContext;
import net.unfamily.iskautils.arcane.jei.ArcaneDictionaryJeiLines;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.util.ArtifactTickIntervals;

import java.util.List;

public final class GraveDebtEffect implements ArcaneDictionaryEffect {
    private static final Identifier SPEED_ID =
            Identifier.fromNamespaceAndPath("iska_utils", "arcane/grave_debt_speed");

    @Override
    public Identifier id() {
        return ArcaneDictionaryTraitIds.GRAVE_DEBT;
    }

    @Override
    public int defaultConsumePerLevel() {
        return 0;
    }

    @Override
    public void onPlayerTick(ArcaneDictionaryEffectContext ctx) {
        float ratio = ctx.player().getHealth() / Math.max(1.0F, ctx.player().getMaxHealth());
        if (ratio >= Config.arcaneGraveDebtHighHpRatio) {
            ctx.player().addEffect(new MobEffectInstance(
                    MobEffects.SLOWNESS, ArtifactTickIntervals.POTION_DURATION_TICKS, 0, true, false, true));
            ArcaneDictionaryAttributes.remove(ctx.player().getAttribute(Attributes.MOVEMENT_SPEED), SPEED_ID);
            return;
        }
        if (ratio <= Config.arcaneGraveDebtLowHpRatio) {
            AttributeInstance speed = ctx.player().getAttribute(Attributes.MOVEMENT_SPEED);
            double bonus = Config.arcaneGraveDebtSpeedMultPerLevel * ctx.level();
            ArcaneDictionaryAttributes.applyTransientMultiplied(speed, SPEED_ID, bonus);
            return;
        }
        ArcaneDictionaryAttributes.remove(ctx.player().getAttribute(Attributes.MOVEMENT_SPEED), SPEED_ID);
    }

    @Override
    public void appendJeiDescription(ArcaneDictionaryJeiContext ctx, List<Component> lines) {
        ArcaneDictionaryJeiLines.appendTraitLine(
                ctx,
                lines,
                "slow",
                ctx.formatPercent(Config.arcaneGraveDebtHighHpRatio * 100.0D));
        ArcaneDictionaryJeiLines.appendTraitLine(
                ctx,
                lines,
                "fast",
                ctx.formatPercent(Config.arcaneGraveDebtLowHpRatio * 100.0D));
        ArcaneDictionaryJeiLines.appendScaledPercent(ctx, lines, "speed", Config.arcaneGraveDebtSpeedMultPerLevel);
    }
}

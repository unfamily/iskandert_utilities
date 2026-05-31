package net.unfamily.iskautils.arcane.effects;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.unfamily.iskautils.arcane.ArcaneDictionaryEffect;
import net.unfamily.iskautils.arcane.ArcaneDictionaryEffectContext;
import net.unfamily.iskautils.arcane.ArcaneDictionaryTraitIds;
import net.unfamily.iskautils.arcane.jei.ArcaneDictionaryJeiContext;
import net.unfamily.iskautils.arcane.jei.ArcaneDictionaryJeiLines;
import net.unfamily.iskautils.Config;

import java.util.List;

public final class LifeSiphonEffect implements ArcaneDictionaryEffect {

    @Override
    public net.minecraft.resources.ResourceLocation id() {
        return ArcaneDictionaryTraitIds.LIFE_SIPHON;
    }

    @Override
    public int defaultConsumePerLevel() {
        return 4;
    }

    @Override
    public void onPlayerAttack(ArcaneDictionaryEffectContext ctx, LivingIncomingDamageEvent event) {
        if (event.getAmount() <= 0.0F) {
            return;
        }
        float heal = event.getAmount()
                * (float) (Config.arcaneLifeSiphonHealFractionPerLevel * ctx.level());
        heal = Math.min(heal, (float) Config.arcaneLifeSiphonHealCap);
        if (heal > 0.0F) {
            ctx.player().heal(heal);
        }
    }

    @Override
    public void appendJeiDescription(ArcaneDictionaryJeiContext ctx, List<Component> lines) {
        ArcaneDictionaryJeiLines.appendScaledPercent(ctx, lines, "heal", Config.arcaneLifeSiphonHealFractionPerLevel);
        ArcaneDictionaryJeiLines.appendTraitLine(
                ctx, lines, "cap", ctx.formatNumber(Config.arcaneLifeSiphonHealCap));
    }
}

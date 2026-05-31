package net.unfamily.iskautils.arcane.effects;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.unfamily.iskautils.arcane.ArcaneDictionaryEffect;
import net.unfamily.iskautils.arcane.ArcaneDictionaryEffectContext;
import net.unfamily.iskautils.arcane.ArcaneDictionaryTraitIds;
import net.unfamily.iskautils.arcane.jei.ArcaneDictionaryJeiContext;
import net.unfamily.iskautils.arcane.jei.ArcaneDictionaryJeiLines;
import net.unfamily.iskautils.Config;

import java.util.List;

public final class ExecutionLineEffect implements ArcaneDictionaryEffect {

    @Override
    public net.minecraft.resources.ResourceLocation id() {
        return ArcaneDictionaryTraitIds.EXECUTION_LINE;
    }

    @Override
    public int defaultConsumePerLevel() {
        return 5;
    }

    @Override
    public void onPlayerAttack(ArcaneDictionaryEffectContext ctx, LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }
        float ratio = target.getHealth() / Math.max(1.0F, target.getMaxHealth());
        if (ratio > Config.arcaneExecutionLineHpThreshold) {
            return;
        }
        float bonus = event.getAmount() * (float) Config.arcaneExecutionLineBonusDamageMult * ctx.level();
        event.setAmount(event.getAmount() + bonus);
    }

    @Override
    public void appendJeiDescription(ArcaneDictionaryJeiContext ctx, List<Component> lines) {
        ArcaneDictionaryJeiLines.appendTraitLine(
                ctx,
                lines,
                "threshold",
                ctx.formatPercent(Config.arcaneExecutionLineHpThreshold * 100.0D));
        ArcaneDictionaryJeiLines.appendScaledPercent(ctx, lines, "bonus", Config.arcaneExecutionLineBonusDamageMult);
    }
}

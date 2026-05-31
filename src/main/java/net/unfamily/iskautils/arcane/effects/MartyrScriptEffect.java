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

public final class MartyrScriptEffect implements ArcaneDictionaryEffect {
    @Override
    public net.minecraft.resources.ResourceLocation id() {
        return ArcaneDictionaryTraitIds.MARTYR_SCRIPT;
    }

    @Override
    public int defaultConsumePerLevel() {
        return 7;
    }

    @Override
    public void onPlayerAttack(ArcaneDictionaryEffectContext ctx, LivingIncomingDamageEvent event) {
        float multiplier = 1.0F + (float) Config.arcaneMartyrScriptDealtMultPerLevel * ctx.level();
        event.setAmount(event.getAmount() * multiplier);
    }

    @Override
    public void onVictimDamaged(ArcaneDictionaryEffectContext ctx, LivingIncomingDamageEvent event) {
        float multiplier = 1.0F + (float) Config.arcaneMartyrScriptTakenMultPerLevel * ctx.level();
        event.setAmount(event.getAmount() * multiplier);
    }

    @Override
    public void appendJeiDescription(ArcaneDictionaryJeiContext ctx, List<Component> lines) {
        ArcaneDictionaryJeiLines.appendScaledPercent(ctx, lines, "dealt", Config.arcaneMartyrScriptDealtMultPerLevel);
        ArcaneDictionaryJeiLines.appendScaledPercent(ctx, lines, "taken", Config.arcaneMartyrScriptTakenMultPerLevel);
    }
}

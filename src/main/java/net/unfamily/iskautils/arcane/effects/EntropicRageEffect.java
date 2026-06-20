package net.unfamily.iskautils.arcane.effects;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.arcane.ArcaneDictionaryEffect;
import net.unfamily.iskautils.arcane.ArcaneDictionaryEffectContext;
import net.unfamily.iskautils.arcane.ArcaneDictionaryTraitIds;
import net.unfamily.iskautils.arcane.jei.ArcaneDictionaryJeiContext;
import net.unfamily.iskautils.arcane.jei.ArcaneDictionaryJeiLines;

import java.util.List;

public final class EntropicRageEffect implements ArcaneDictionaryEffect {
    @Override
    public net.minecraft.resources.ResourceLocation id() {
        return ArcaneDictionaryTraitIds.ENTROPIC_RAGE;
    }

    @Override
    public int defaultConsumePerLevel() {
        return 0;
    }

    @Override
    public void onPlayerAttack(ArcaneDictionaryEffectContext ctx, LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof WanderingTrader)) {
            return;
        }
        float mult = 1.0F + (float) (ctx.level() * Config.arcaneEntropicRageDamageMultPerLevel);
        if (mult > 1.0F) {
            event.setAmount(event.getAmount() * mult);
        }
    }

    @Override
    public void appendJeiDescription(ArcaneDictionaryJeiContext ctx, List<Component> lines) {
        ArcaneDictionaryJeiLines.appendScaled(
                ctx,
                lines,
                "trader",
                level -> (1.0D + level * Config.arcaneEntropicRageDamageMultPerLevel) * 100.0D - 100.0D);
        lines.add(Component.translatable("jei.iska_utils.arcane_trait.iska_utils.entropic_rage.wandering_trader"));
    }
}

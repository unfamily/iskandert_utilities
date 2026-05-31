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
import net.unfamily.iskautils.util.DamageArmorBypassUtil;

import java.util.List;

public final class EntropyBladeEffect implements ArcaneDictionaryEffect {
    @Override
    public net.minecraft.resources.Identifier id() {
        return ArcaneDictionaryTraitIds.ENTROPY_BLADE;
    }

    @Override
    public int defaultConsumePerLevel() {
        return 7;
    }

    @Override
    public void onPlayerAttack(ArcaneDictionaryEffectContext ctx, LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }
        float fraction = (float) (Config.arcaneEntropyBladeArmorBypassPerLevel * ctx.level());
        float extra = DamageArmorBypassUtil.computeExtraDamage(
                target, event.getAmount(), event.getSource(), fraction, fraction);
        if (extra > 0.0F) {
            event.setAmount(event.getAmount() + extra);
        }
    }

    @Override
    public void appendJeiDescription(ArcaneDictionaryJeiContext ctx, List<Component> lines) {
        ArcaneDictionaryJeiLines.appendScaledPercent(ctx, lines, "bypass", Config.arcaneEntropyBladeArmorBypassPerLevel);
    }
}

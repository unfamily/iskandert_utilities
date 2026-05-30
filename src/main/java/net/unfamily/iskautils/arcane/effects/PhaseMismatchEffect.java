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

public final class PhaseMismatchEffect implements ArcaneDictionaryEffect {
    @Override
    public net.minecraft.resources.Identifier id() {
        return ArcaneDictionaryTraitIds.PHASE_MISMATCH;
    }

    @Override
    public int defaultUpkeepPerLevel() {
        return 8;
    }

    @Override
    public void onVictimDamaged(ArcaneDictionaryEffectContext ctx, LivingIncomingDamageEvent event) {
        float chance = (float) (Config.arcanePhaseMismatchDodgeChancePerLevel * ctx.level());
        if (ctx.player().getRandom().nextFloat() < chance) {
            event.setAmount(0.0F);
        }
    }

    @Override
    public void appendJeiDescription(ArcaneDictionaryJeiContext ctx, List<Component> lines) {
        ArcaneDictionaryJeiLines.appendScaledPercent(ctx, lines, "dodge", Config.arcanePhaseMismatchDodgeChancePerLevel);
    }
}

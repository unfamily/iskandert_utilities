package net.unfamily.iskautils.arcane.effects;

import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.unfamily.iskautils.arcane.ArcaneDictionaryEffect;
import net.unfamily.iskautils.arcane.ArcaneDictionaryEffectContext;
import net.unfamily.iskautils.arcane.ArcaneDictionaryTraitIds;
import net.unfamily.iskautils.arcane.jei.ArcaneDictionaryJeiContext;
import net.unfamily.iskautils.arcane.jei.ArcaneDictionaryJeiLines;
import net.unfamily.iskautils.Config;

import java.util.List;

public final class VoidThornsEffect implements ArcaneDictionaryEffect {
    @Override
    public net.minecraft.resources.Identifier id() {
        return ArcaneDictionaryTraitIds.VOID_THORNS;
    }

    @Override
    public int defaultUpkeepPerLevel() {
        return 4;
    }

    @Override
    public void onVictimDamaged(ArcaneDictionaryEffectContext ctx, LivingIncomingDamageEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (attacker == null || !attacker.isAlive()) {
            return;
        }
        DamageSource magic = ctx.player().level().damageSources().source(DamageTypes.MAGIC, ctx.player());
        attacker.hurt(magic, (float) (Config.arcaneVoidThornsDamagePerLevel * ctx.level()));
    }

    @Override
    public void appendJeiDescription(ArcaneDictionaryJeiContext ctx, List<Component> lines) {
        ArcaneDictionaryJeiLines.appendScaled(
                ctx, lines, "thorns", level -> level * Config.arcaneVoidThornsDamagePerLevel);
    }
}

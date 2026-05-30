package net.unfamily.iskautils.arcane.effects;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.arcane.ArcaneDictionaryEffect;
import net.unfamily.iskautils.arcane.ArcaneDictionaryEffectContext;
import net.unfamily.iskautils.arcane.jei.ArcaneDictionaryJeiContext;
import net.unfamily.iskautils.arcane.jei.ArcaneDictionaryJeiLines;

import java.util.List;

public final class GlassSkinEffect implements ArcaneDictionaryEffect {
    private final ResourceLocation id;

    public GlassSkinEffect(ResourceLocation id) {
        this.id = id;
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public int defaultUpkeepPerLevel() {
        return 3;
    }

    @Override
    public void onVictimDamaged(ArcaneDictionaryEffectContext ctx, LivingIncomingDamageEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (attacker == null || !attacker.isAlive()) {
            return;
        }
        float reflect = (float) (ctx.level() * Config.arcaneGlassSkinReflectMultPerLevel);
        float self = (float) (ctx.level() * Config.arcaneGlassSkinSelfDamagePerLevel);
        if (reflect > 0.0F) {
            DamageSource magic = ctx.player().level().damageSources().source(DamageTypes.MAGIC, ctx.player());
            attacker.hurt(magic, reflect);
        }
        if (self > 0.0F) {
            event.setAmount(event.getAmount() + self);
        }
    }

    @Override
    public void appendJeiDescription(ArcaneDictionaryJeiContext ctx, List<Component> lines) {
        ArcaneDictionaryJeiLines.appendScaled(
                ctx,
                lines,
                "reflect",
                level -> level * Config.arcaneGlassSkinReflectMultPerLevel);
        ArcaneDictionaryJeiLines.appendScaled(
                ctx,
                lines,
                "self_damage",
                level -> level * Config.arcaneGlassSkinSelfDamagePerLevel);
    }
}

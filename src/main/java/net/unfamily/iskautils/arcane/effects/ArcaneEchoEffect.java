package net.unfamily.iskautils.arcane.effects;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.arcane.ArcaneDictionaryEffect;
import net.unfamily.iskautils.arcane.ArcaneDictionaryEffectContext;
import net.unfamily.iskautils.arcane.ArcaneDictionaryTraitIds;
import net.unfamily.iskautils.arcane.jei.ArcaneDictionaryJeiContext;
import net.unfamily.iskautils.arcane.jei.ArcaneDictionaryJeiLines;
import net.unfamily.iskautils.util.CurioEquipUtil;

import java.util.List;

public final class ArcaneEchoEffect implements ArcaneDictionaryEffect {
    @Override
    public Identifier id() {
        return ArcaneDictionaryTraitIds.ARCANE_ECHO;
    }

    @Override
    public int defaultConsumePerLevel() {
        return 6;
    }

    @Override
    public void onPlayerAttack(ArcaneDictionaryEffectContext ctx, LivingIncomingDamageEvent event) {
        double procChance = Math.min(1.0D, ctx.level() * Config.arcaneEchoProcChancePerLevel);
        if (ctx.player().getRandom().nextDouble() >= procChance) {
            return;
        }
        int artifacts = CurioEquipUtil.countEquippedArcaneArtifacts(ctx.player());
        if (artifacts <= 0) {
            return;
        }
        float bonus = (float) (artifacts * Config.arcaneEchoDamagePerArtifact);
        if (bonus > 0.0F) {
            event.setAmount(event.getAmount() + bonus);
        }
    }

    @Override
    public void appendJeiDescription(ArcaneDictionaryJeiContext ctx, List<Component> lines) {
        ArcaneDictionaryJeiLines.appendScaledPercent(ctx, lines, "proc", Config.arcaneEchoProcChancePerLevel);
        ArcaneDictionaryJeiLines.appendTraitLine(
                ctx, lines, "per_artifact", ctx.formatNumber(Config.arcaneEchoDamagePerArtifact));
    }
}

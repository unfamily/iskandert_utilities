package net.unfamily.iskautils.arcane.effects;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.unfamily.iskautils.arcane.ArcaneDictionaryAttributes;
import net.unfamily.iskautils.arcane.ArcaneDictionaryEffect;
import net.unfamily.iskautils.arcane.ArcaneDictionaryEffectContext;
import net.unfamily.iskautils.arcane.ArcaneDictionaryTraitIds;
import net.unfamily.iskautils.arcane.jei.ArcaneDictionaryJeiContext;
import net.unfamily.iskautils.arcane.jei.ArcaneDictionaryJeiLines;
import net.unfamily.iskautils.Config;

import java.util.List;

public final class QuickHandsEffect implements ArcaneDictionaryEffect {
    private static final Identifier ATTACK_SPEED_ID =
            Identifier.fromNamespaceAndPath("iska_utils", "arcane/quick_hands_attack_speed");

    @Override
    public Identifier id() {
        return ArcaneDictionaryTraitIds.QUICK_HANDS;
    }

    @Override
    public int defaultConsumePerLevel() {
        return 3;
    }

    @Override
    public void onPlayerTick(ArcaneDictionaryEffectContext ctx) {
        AttributeInstance attackSpeed = ctx.player().getAttribute(Attributes.ATTACK_SPEED);
        double bonus = Config.arcaneQuickHandsAttackSpeedPerLevel * ctx.level();
        ArcaneDictionaryAttributes.applyTransientMultiplied(attackSpeed, ATTACK_SPEED_ID, bonus);
    }

    @Override
    public void appendJeiDescription(ArcaneDictionaryJeiContext ctx, List<Component> lines) {
        ArcaneDictionaryJeiLines.appendScaledPercent(
                ctx, lines, "attack_speed", Config.arcaneQuickHandsAttackSpeedPerLevel);
    }
}

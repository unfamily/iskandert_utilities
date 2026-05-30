package net.unfamily.iskautils.arcane.effects;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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

public final class EntropyOverflowEffect implements ArcaneDictionaryEffect {
    private static final ResourceLocation HP_ID =
            ResourceLocation.fromNamespaceAndPath("iska_utils", "arcane/entropy_overflow_hp");

    @Override
    public ResourceLocation id() {
        return ArcaneDictionaryTraitIds.ENTROPY_OVERFLOW;
    }

    @Override
    public void onPlayerTick(ArcaneDictionaryEffectContext ctx) {
        AttributeInstance maxHealth = ctx.player().getAttribute(Attributes.MAX_HEALTH);
        double penalty = -Config.arcaneEntropyOverflowHpPenaltyPerLevel * ctx.level();
        ArcaneDictionaryAttributes.applyTransient(maxHealth, HP_ID, penalty);
        ArcaneDictionaryAttributes.clampHealth(ctx.player());
    }

    @Override
    public void appendJeiDescription(ArcaneDictionaryJeiContext ctx, List<Component> lines) {
        ArcaneDictionaryJeiLines.appendScaledPercent(
                ctx, lines, "other_traits", Config.arcaneEntropyOverflowUpkeepReductionPerLevel);
        ArcaneDictionaryJeiLines.appendScaled(
                ctx, lines, "health", level -> -level * Config.arcaneEntropyOverflowHpPenaltyPerLevel);
    }
}

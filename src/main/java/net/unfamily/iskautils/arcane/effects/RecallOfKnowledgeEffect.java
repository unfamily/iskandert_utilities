package net.unfamily.iskautils.arcane.effects;

import net.minecraft.network.chat.Component;
import net.unfamily.iskautils.arcane.ArcaneDictionaryEffect;
import net.unfamily.iskautils.arcane.ArcaneDictionaryEffectContext;
import net.unfamily.iskautils.arcane.ArcaneDictionaryTraitIds;
import net.unfamily.iskautils.arcane.jei.ArcaneDictionaryJeiContext;
import net.unfamily.iskautils.arcane.jei.ArcaneDictionaryJeiLines;
import net.unfamily.iskautils.Config;

import java.util.List;

public final class RecallOfKnowledgeEffect implements ArcaneDictionaryEffect {
    @Override
    public net.minecraft.resources.ResourceLocation id() {
        return ArcaneDictionaryTraitIds.RECALL_OF_KNOWLEDGE;
    }

    @Override
    public int defaultConsumePerLevel() {
        return 3;
    }

    @Override
    public int adjustExperiencePickup(ArcaneDictionaryEffectContext ctx, int xpAmount) {
        if (xpAmount <= 0) {
            return xpAmount;
        }
        float multiplier = 1.0F + (float) Config.arcaneRecallOfKnowledgeXpMultPerLevel * ctx.level();
        return Math.max(1, Math.round(xpAmount * multiplier));
    }

    @Override
    public void appendJeiDescription(ArcaneDictionaryJeiContext ctx, List<Component> lines) {
        ArcaneDictionaryJeiLines.appendScaledPercent(ctx, lines, "xp", Config.arcaneRecallOfKnowledgeXpMultPerLevel);
    }
}

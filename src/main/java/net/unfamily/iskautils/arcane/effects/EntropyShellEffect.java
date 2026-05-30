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

public final class EntropyShellEffect implements ArcaneDictionaryEffect {
    private static final Identifier TOUGHNESS_ID =
            Identifier.fromNamespaceAndPath("iska_utils", "arcane/entropy_shell_toughness");
    private static final Identifier HP_ID =
            Identifier.fromNamespaceAndPath("iska_utils", "arcane/entropy_shell_hp");

    @Override
    public Identifier id() {
        return ArcaneDictionaryTraitIds.ENTROPY_SHELL;
    }

    @Override
    public int defaultUpkeepPerLevel() {
        return 5;
    }

    @Override
    public void onPlayerTick(ArcaneDictionaryEffectContext ctx) {
        AttributeInstance toughness = ctx.player().getAttribute(Attributes.ARMOR_TOUGHNESS);
        AttributeInstance maxHealth = ctx.player().getAttribute(Attributes.MAX_HEALTH);
        ArcaneDictionaryAttributes.applyTransient(
                toughness, TOUGHNESS_ID, Config.arcaneEntropyShellToughnessPerLevel * ctx.level());
        ArcaneDictionaryAttributes.applyTransient(
                maxHealth, HP_ID, -Config.arcaneEntropyShellHpPenaltyPerLevel * ctx.level());
        ArcaneDictionaryAttributes.clampHealth(ctx.player());
    }

    @Override
    public void appendJeiDescription(ArcaneDictionaryJeiContext ctx, List<Component> lines) {
        ArcaneDictionaryJeiLines.appendScaled(
                ctx, lines, "toughness", level -> level * Config.arcaneEntropyShellToughnessPerLevel);
        ArcaneDictionaryJeiLines.appendScaled(
                ctx, lines, "health", level -> -level * Config.arcaneEntropyShellHpPenaltyPerLevel);
    }
}

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

public final class AgilityEffect implements ArcaneDictionaryEffect {
    private static final Identifier SPEED_ID =
            Identifier.fromNamespaceAndPath("iska_utils", "arcane/agility_speed");

    @Override
    public Identifier id() {
        return ArcaneDictionaryTraitIds.AGILITY;
    }

    @Override
    public int defaultUpkeepPerLevel() {
        return 2;
    }

    @Override
    public void onPlayerTick(ArcaneDictionaryEffectContext ctx) {
        AttributeInstance speed = ctx.player().getAttribute(Attributes.MOVEMENT_SPEED);
        double bonus = Config.arcaneAgilitySpeedMultPerLevel * ctx.level();
        ArcaneDictionaryAttributes.applyTransientMultiplied(speed, SPEED_ID, bonus);
    }

    @Override
    public void appendJeiDescription(ArcaneDictionaryJeiContext ctx, List<Component> lines) {
        ArcaneDictionaryJeiLines.appendScaledPercent(ctx, lines, "speed", Config.arcaneAgilitySpeedMultPerLevel);
    }
}

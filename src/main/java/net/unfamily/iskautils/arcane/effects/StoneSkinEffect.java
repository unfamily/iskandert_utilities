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

public final class StoneSkinEffect implements ArcaneDictionaryEffect {
    private static final Identifier ARMOR_ID =
            Identifier.fromNamespaceAndPath("iska_utils", "arcane/stone_skin_armor");

    @Override
    public Identifier id() {
        return ArcaneDictionaryTraitIds.STONE_SKIN;
    }

    @Override
    public int defaultConsumePerLevel() {
        return 5;
    }

    @Override
    public void onPlayerTick(ArcaneDictionaryEffectContext ctx) {
        AttributeInstance armor = ctx.player().getAttribute(Attributes.ARMOR);
        double bonus = Config.arcaneStoneSkinArmorPerLevel * ctx.level();
        ArcaneDictionaryAttributes.applyTransient(armor, ARMOR_ID, bonus);
    }

    @Override
    public void appendJeiDescription(ArcaneDictionaryJeiContext ctx, List<Component> lines) {
        ArcaneDictionaryJeiLines.appendScaled(ctx, lines, "armor", level -> level * Config.arcaneStoneSkinArmorPerLevel);
    }
}

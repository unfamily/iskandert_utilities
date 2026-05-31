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

public final class IronRootEffect implements ArcaneDictionaryEffect {
    private static final Identifier KNOCKBACK_ID =
            Identifier.fromNamespaceAndPath("iska_utils", "arcane/iron_root_knockback");

    @Override
    public Identifier id() {
        return ArcaneDictionaryTraitIds.IRON_ROOT;
    }

    @Override
    public int defaultConsumePerLevel() {
        return 3;
    }

    @Override
    public void onPlayerTick(ArcaneDictionaryEffectContext ctx) {
        AttributeInstance knockback = ctx.player().getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        double bonus = Config.arcaneIronRootKnockbackResistPerLevel * ctx.level();
        ArcaneDictionaryAttributes.applyTransient(knockback, KNOCKBACK_ID, bonus);
    }

    @Override
    public void appendJeiDescription(ArcaneDictionaryJeiContext ctx, List<Component> lines) {
        ArcaneDictionaryJeiLines.appendScaledPercent(
                ctx, lines, "knockback", Config.arcaneIronRootKnockbackResistPerLevel);
    }
}

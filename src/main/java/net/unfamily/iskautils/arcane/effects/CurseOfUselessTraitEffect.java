package net.unfamily.iskautils.arcane.effects;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.unfamily.iskautils.arcane.ArcaneDictionaryEffect;
import net.unfamily.iskautils.arcane.ArcaneDictionaryTraitIds;
import net.unfamily.iskautils.arcane.jei.ArcaneDictionaryJeiContext;

import java.util.List;

public final class CurseOfUselessTraitEffect implements ArcaneDictionaryEffect {
    @Override
    public Identifier id() {
        return ArcaneDictionaryTraitIds.CURSE_OF_USELESS;
    }

    @Override
    public int defaultConsumePerLevel() {
        return 0;
    }

    @Override
    public void appendJeiDescription(ArcaneDictionaryJeiContext ctx, List<Component> lines) {
        CurseOfUselessEffect.appendJeiDescription(ctx, lines);
    }
}

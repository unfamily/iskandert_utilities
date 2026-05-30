package net.unfamily.iskautils.arcane.jei;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

/** Optional JEI-only descriptions for addon traits without a custom {@link net.unfamily.iskautils.arcane.ArcaneDictionaryEffect}. */
public interface ArcaneDictionaryJeiDescriber {
    Identifier traitId();

    void appendJeiDescription(ArcaneDictionaryJeiContext ctx, List<Component> lines);
}

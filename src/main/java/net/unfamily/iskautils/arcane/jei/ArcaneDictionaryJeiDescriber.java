package net.unfamily.iskautils.arcane.jei;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Optional JEI-only descriptions for addon traits without a custom {@link net.unfamily.iskautils.arcane.ArcaneDictionaryEffect}. */
public interface ArcaneDictionaryJeiDescriber {
    ResourceLocation traitId();

    void appendJeiDescription(ArcaneDictionaryJeiContext ctx, List<Component> lines);
}

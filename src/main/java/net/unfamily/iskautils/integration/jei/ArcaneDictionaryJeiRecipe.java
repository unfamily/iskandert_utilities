package net.unfamily.iskautils.integration.jei;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.unfamily.iskautils.arcane.ArcaneDictionaryDefinition;
import net.unfamily.iskautils.arcane.jei.ArcaneDictionaryJeiLines;

import java.util.List;

public record ArcaneDictionaryJeiRecipe(
        ResourceLocation traitId,
        ArcaneDictionaryDefinition.Entry entry,
        List<FormattedCharSequence> displayLines,
        int catalystRowY,
        int height,
        List<ArcaneDictionaryJeiLines.ResolvedCatalyst> catalysts) {

    public boolean hasCatalysts() {
        return !catalysts.isEmpty();
    }
}

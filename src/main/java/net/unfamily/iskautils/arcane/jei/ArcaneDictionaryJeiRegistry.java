package net.unfamily.iskautils.arcane.jei;

import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ArcaneDictionaryJeiRegistry {
    private static final Map<ResourceLocation, ArcaneDictionaryJeiDescriber> DESCRIBERS = new LinkedHashMap<>();

    private ArcaneDictionaryJeiRegistry() {}

    public static void register(ArcaneDictionaryJeiDescriber describer) {
        if (describer == null || describer.traitId() == null) {
            return;
        }
        DESCRIBERS.put(describer.traitId(), describer);
    }

    public static ArcaneDictionaryJeiDescriber get(ResourceLocation traitId) {
        return traitId == null ? null : DESCRIBERS.get(traitId);
    }

    public static Map<ResourceLocation, ArcaneDictionaryJeiDescriber> all() {
        return Collections.unmodifiableMap(DESCRIBERS);
    }
}

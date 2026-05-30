package net.unfamily.iskautils.arcane.jei;

import net.minecraft.resources.Identifier;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ArcaneDictionaryJeiRegistry {
    private static final Map<Identifier, ArcaneDictionaryJeiDescriber> DESCRIBERS = new LinkedHashMap<>();

    private ArcaneDictionaryJeiRegistry() {}

    public static void register(ArcaneDictionaryJeiDescriber describer) {
        if (describer == null || describer.traitId() == null) {
            return;
        }
        DESCRIBERS.put(describer.traitId(), describer);
    }

    public static ArcaneDictionaryJeiDescriber get(Identifier traitId) {
        return traitId == null ? null : DESCRIBERS.get(traitId);
    }

    public static Map<Identifier, ArcaneDictionaryJeiDescriber> all() {
        return Collections.unmodifiableMap(DESCRIBERS);
    }
}

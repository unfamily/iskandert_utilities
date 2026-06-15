package net.unfamily.iskautils.arcane;

import net.unfamily.iskautils.util.ModLogger;

import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ArcaneDictionaryEffectRegistry {
    private static final ModLogger LOGGER = ModLogger.of(ArcaneDictionaryEffectRegistry.class);
    private static final Map<ResourceLocation, ArcaneDictionaryEffect> EFFECTS = new LinkedHashMap<>();

    private ArcaneDictionaryEffectRegistry() {}

    public static void register(ArcaneDictionaryEffect effect) {
        if (effect == null || effect.id() == null) {
            return;
        }
        EFFECTS.put(effect.id(), effect);
    }

    public static ArcaneDictionaryEffect get(ResourceLocation id) {
        return id == null ? null : EFFECTS.get(id);
    }

    public static Map<ResourceLocation, ArcaneDictionaryEffect> all() {
        return Collections.unmodifiableMap(EFFECTS);
    }

    public static int resolveConsumePerLevel(ResourceLocation enchantId) {
        ArcaneDictionaryDefinition.Entry entry = ArcaneDictionaryLoader.findEntry(enchantId);
        if (entry != null && entry.consumePerLevel() >= 0) {
            return entry.consumePerLevel();
        }
        ArcaneDictionaryEffect effect = get(enchantId);
        return effect != null ? effect.defaultConsumePerLevel() : 0;
    }

    public static void warnUnknown(ResourceLocation id) {
        LOGGER.warn("Arcane dictionary references unknown effect {}", id);
    }
}

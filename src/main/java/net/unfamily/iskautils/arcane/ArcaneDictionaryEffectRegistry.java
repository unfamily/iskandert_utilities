package net.unfamily.iskautils.arcane;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ArcaneDictionaryEffectRegistry {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<Identifier, ArcaneDictionaryEffect> EFFECTS = new LinkedHashMap<>();

    private ArcaneDictionaryEffectRegistry() {}

    public static void register(ArcaneDictionaryEffect effect) {
        if (effect == null || effect.id() == null) {
            return;
        }
        EFFECTS.put(effect.id(), effect);
    }

    public static ArcaneDictionaryEffect get(Identifier id) {
        return id == null ? null : EFFECTS.get(id);
    }

    public static Map<Identifier, ArcaneDictionaryEffect> all() {
        return Collections.unmodifiableMap(EFFECTS);
    }

    public static int resolveConsumePerLevel(Identifier enchantId) {
        ArcaneDictionaryDefinition.Entry entry = ArcaneDictionaryLoader.findEntry(enchantId);
        if (entry != null && entry.consumePerLevel() >= 0) {
            return entry.consumePerLevel();
        }
        ArcaneDictionaryEffect effect = get(enchantId);
        return effect != null ? effect.defaultConsumePerLevel() : 0;
    }

    public static void warnUnknown(Identifier id) {
        LOGGER.warn("Arcane dictionary references unknown effect {}", id);
    }
}

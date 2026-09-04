package net.unfamily.iskautils.shop;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.unfamily.iskautils.IskaUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registry for shop {@code type: other} resources (e.g. RF). Extensible for future ids.
 */
public final class ShopOtherRegistry {
    public static final String RF_ID = "iska_utils:rf";

    public record Definition(String id, ResourceLocation icon, Component displayName) {}

    private static final Map<String, Definition> DEFINITIONS = new LinkedHashMap<>();

    static {
        register(new Definition(
                RF_ID,
                ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/rf_icon.png"),
                Component.translatable("gui.iska_utils.shop.other.rf")));
    }

    private ShopOtherRegistry() {}

    public static void register(Definition definition) {
        if (definition == null || definition.id() == null || definition.id().isBlank()) {
            return;
        }
        DEFINITIONS.put(definition.id().trim(), definition);
    }

    public static boolean isRegistered(@Nullable String id) {
        return id != null && DEFINITIONS.containsKey(id.trim());
    }

    public static boolean isRf(@Nullable String id) {
        return RF_ID.equals(id != null ? id.trim() : null);
    }

    @Nullable
    public static Definition get(@Nullable String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        return DEFINITIONS.get(id.trim());
    }

    public static Component displayName(@Nullable String id) {
        Definition definition = get(id);
        if (definition != null) {
            return definition.displayName();
        }
        return Component.literal(id != null ? id : "");
    }

    public static Collection<Definition> all() {
        return Collections.unmodifiableCollection(DEFINITIONS.values());
    }
}

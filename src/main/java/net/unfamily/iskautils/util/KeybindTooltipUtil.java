package net.unfamily.iskautils.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jspecify.annotations.Nullable;

/**
 * Resolves a player-configured keybind for tooltips.
 * Always applies yellow so the key stays visible when the surrounding line uses lore/tech/gray style.
 */
public final class KeybindTooltipUtil {
    private KeybindTooltipUtil() {}

    public static Component keybindOrTranslation(String translationKey, String clientKeybindingsFieldName) {
        Component resolved = resolveClientKeybinding(clientKeybindingsFieldName);
        Component base = resolved != null ? resolved : Component.translatable(translationKey);
        return withKeybindColor(base);
    }

    /** Explicit yellow so parent line styles (lore/tech/gray) do not wash out the key. */
    public static MutableComponent withKeybindColor(Component keybind) {
        return keybind.copy().withStyle(ChatFormatting.YELLOW);
    }

    private static @Nullable Component resolveClientKeybinding(String clientKeybindingsFieldName) {
        if (!isClientEnvironment()) {
            return null;
        }
        try {
            Class<?> keyBindingsClz = Class.forName("net.unfamily.iskautils.client.KeyBindings");
            Field f = keyBindingsClz.getField(clientKeybindingsFieldName);
            Object keyMapping = f.get(null);
            if (keyMapping == null) {
                return null;
            }
            Method m = keyMapping.getClass().getMethod("getTranslatedKeyMessage");
            Object result = m.invoke(keyMapping);
            if (result instanceof Component c) {
                return c;
            }
            return null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean isClientEnvironment() {
        try {
            Class.forName("net.minecraft.client.Minecraft", false, KeybindTooltipUtil.class.getClassLoader());
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}

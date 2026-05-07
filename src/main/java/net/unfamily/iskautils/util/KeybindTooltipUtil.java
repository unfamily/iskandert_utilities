package net.unfamily.iskautils.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

public final class KeybindTooltipUtil {
    private KeybindTooltipUtil() {}

    public static Component keybindOrTranslation(String translationKey, String clientKeybindingsFieldName) {
        Component resolved = resolveClientKeybinding(clientKeybindingsFieldName);
        return resolved != null ? resolved : Component.translatable(translationKey);
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


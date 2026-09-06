package net.unfamily.iskautils.shop;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.unfamily.iskautils.IskaUtils;

/**
 * Compatibility helpers for RF display (former {@code type: other} / {@code other: iska_utils:rf}).
 * Prefer {@link ShopEntryTypes#RF} and {@link BuiltinShopEntryTypes}.
 */
public final class ShopOtherRegistry {
    public static final String RF_ID = ShopEntryTypes.RF.toString();

    private ShopOtherRegistry() {}

    public static boolean isRf(ShopEntry entry) {
        return ShopEntryTypes.isRf(entry);
    }

    public static boolean isRf(String id) {
        return RF_ID.equals(id != null ? id.trim() : null);
    }

    public static Identifier rfIcon() {
        return BuiltinShopEntryTypes.RF_ICON;
    }

    public static Component rfDisplayName() {
        return Component.translatable("gui.iska_utils.shop.other.rf");
    }

    /** @deprecated RF is a first-class type; kept for older call sites. */
    @Deprecated
    public static boolean isRegistered(String id) {
        return isRf(id);
    }

    /** @deprecated use {@link #rfDisplayName()} */
    @Deprecated
    public static Component displayName(String id) {
        return isRf(id) ? rfDisplayName() : Component.literal(id != null ? id : "");
    }

    /** @deprecated */
    @Deprecated
    public static Definition get(String id) {
        return isRf(id) ? new Definition(RF_ID, rfIcon(), rfDisplayName()) : null;
    }

    /** @deprecated */
    @Deprecated
    public record Definition(String id, Identifier icon, Component displayName) {}
}

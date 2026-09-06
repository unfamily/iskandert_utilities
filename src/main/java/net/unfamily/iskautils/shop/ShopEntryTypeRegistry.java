package net.unfamily.iskautils.shop;

import net.minecraft.resources.Identifier;
import net.unfamily.iskautils.IskaUtils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry of shop entry type handlers. Builtins register on mod init; addons may register more.
 */
public final class ShopEntryTypeRegistry {
    private static final Map<Identifier, ShopEntryTypeHandler> BY_ID = new LinkedHashMap<>();
    private static final List<ShopEntryTypeHandler> ORDERED = new ArrayList<>();
    private static boolean builtinsRegistered;

    private ShopEntryTypeRegistry() {}

    public static synchronized void register(ShopEntryTypeHandler handler) {
        if (handler == null || handler.id() == null) {
            return;
        }
        Identifier id = handler.id();
        ShopEntryTypeHandler previous = BY_ID.put(id, handler);
        if (previous != null) {
            ORDERED.remove(previous);
        }
        ORDERED.add(handler);
    }

    public static synchronized void ensureBuiltins() {
        if (builtinsRegistered) {
            return;
        }
        builtinsRegistered = true;
        BuiltinShopEntryTypes.registerAll();
    }

    @Nullable
    public static ShopEntryTypeHandler get(@Nullable Identifier id) {
        ensureBuiltins();
        if (id == null) {
            return null;
        }
        return BY_ID.get(id);
    }

    @Nullable
    public static ShopEntryTypeHandler get(@Nullable ShopEntry entry) {
        return entry == null ? null : get(entry.typeId);
    }

    public static ShopEntryTypeHandler require(@Nullable ShopEntry entry) {
        ShopEntryTypeHandler handler = get(entry);
        if (handler != null) {
            return handler;
        }
        ensureBuiltins();
        return BY_ID.get(ShopEntryTypes.ITEM);
    }

    public static Collection<ShopEntryTypeHandler> all() {
        ensureBuiltins();
        return Collections.unmodifiableCollection(ORDERED);
    }

    public static List<ShopEntryTypeHandler> availableOrdered() {
        ensureBuiltins();
        List<ShopEntryTypeHandler> out = new ArrayList<>();
        for (ShopEntryTypeHandler handler : ORDERED) {
            if (handler.isAvailable()) {
                out.add(handler);
            }
        }
        return out;
    }

    /** Sync index for menus; unknown → 0. */
    public static int syncIndex(@Nullable Identifier id) {
        ensureBuiltins();
        if (id == null) {
            return 0;
        }
        int i = 0;
        for (ShopEntryTypeHandler handler : ORDERED) {
            if (handler.id().equals(id)) {
                return i;
            }
            i++;
        }
        return 0;
    }

    @Nullable
    public static Identifier idBySyncIndex(int index) {
        ensureBuiltins();
        if (index < 0 || index >= ORDERED.size()) {
            return ShopEntryTypes.ITEM;
        }
        return ORDERED.get(index).id();
    }

    public static Identifier modId(String path) {
        return Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, path);
    }
}

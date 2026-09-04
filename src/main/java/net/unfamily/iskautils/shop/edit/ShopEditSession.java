package net.unfamily.iskautils.shop.edit;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.unfamily.iskautils.shop.ShopCategory;
import net.unfamily.iskautils.shop.ShopCurrency;
import net.unfamily.iskautils.shop.ShopEntry;
import net.unfamily.iskautils.util.ModLogger;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Single-editor lock and in-memory shop edit session.
 */
public final class ShopEditSession {

    private static final ModLogger LOGGER = ModLogger.of(ShopEditSession.class);

    private static UUID holderId;
    private static String holderName;
    private static ShopEditWorkspace.ShopEditData data;

    private ShopEditSession() {}

    public static synchronized boolean tryAcquire(ServerPlayer player) {
        if (holderId != null && !holderId.equals(player.getUUID())) {
            return false;
        }
        try {
            data = ShopEditWorkspace.load(((net.minecraft.server.level.ServerLevel) player.level()).getServer());
        } catch (Exception e) {
            LOGGER.error("Failed to load shop edit workspace: {}", e.toString());
            data = new ShopEditWorkspace.ShopEditData();
        }
        holderId = player.getUUID();
        holderName = player.getName().getString();
        return true;
    }

    public static synchronized void release(UUID playerId) {
        if (holderId != null && holderId.equals(playerId)) {
            holderId = null;
            holderName = null;
            data = null;
        }
    }

    public static synchronized void releaseIfHolder(ServerPlayer player) {
        if (player != null) {
            release(player.getUUID());
        }
    }

    public static synchronized boolean isHolder(ServerPlayer player) {
        return player != null && holderId != null && holderId.equals(player.getUUID());
    }

    @Nullable
    public static synchronized String getHolderName() {
        return holderName;
    }

    @Nullable
    public static synchronized ShopEditWorkspace.ShopEditData getData() {
        return data;
    }

    public static synchronized void autosaveCurrencies(MinecraftServer server) {
        if (data == null) {
            return;
        }
        try {
            ShopEditWorkspace.saveCurrencies(server, data.currencies);
        } catch (Exception e) {
            LOGGER.error("Autosave currencies failed: {}", e.toString());
        }
    }

    public static synchronized void autosaveCategories(MinecraftServer server) {
        if (data == null) {
            return;
        }
        try {
            ShopEditWorkspace.saveCategories(server, data.categories);
        } catch (Exception e) {
            LOGGER.error("Autosave categories failed: {}", e.toString());
        }
    }

    public static synchronized void autosaveEntries(MinecraftServer server) {
        if (data == null) {
            return;
        }
        try {
            ShopEditWorkspace.saveEntries(server, data.entries);
        } catch (Exception e) {
            LOGGER.error("Autosave entries failed: {}", e.toString());
        }
    }

    public static Component occupiedMessage() {
        String name = getHolderName();
        return Component.translatable("gui.iska_utils.shop_edit.occupied", name != null ? name : "?");
    }

    public static ShopCurrency copyCurrency(ShopCurrency src) {
        ShopCurrency c = new ShopCurrency();
        c.id = src.id;
        c.name = src.name;
        c.charSymbol = src.charSymbol;
        c.priority = src.priority;
        return c;
    }

    public static ShopCategory copyCategory(ShopCategory src) {
        ShopCategory c = new ShopCategory();
        c.id = src.id;
        c.name = src.name;
        c.description = src.description;
        c.item = src.item;
        c.priority = src.priority;
        return c;
    }

    public static ShopEntry copyEntry(ShopEntry src) {
        ShopEntry e = new ShopEntry();
        e.id = src.id;
        e.inCategory = src.inCategory;
        e.type = src.type;
        e.item = src.item;
        e.fluid = src.fluid;
        e.gas = src.gas;
        e.amount = src.amount;
        e.itemCount = src.itemCount;
        e.currency = src.currency;
        e.valute = src.valute;
        e.buy = src.buy;
        e.sell = src.sell;
        e.priority = src.priority;
        e.free = src.free;
        if (src.stages != null) {
            e.stages = new net.unfamily.iskautils.shop.ShopStage[src.stages.length];
            for (int i = 0; i < src.stages.length; i++) {
                if (src.stages[i] == null) {
                    continue;
                }
                net.unfamily.iskautils.shop.ShopStage st = new net.unfamily.iskautils.shop.ShopStage();
                st.stage = src.stages[i].stage;
                st.stageType = src.stages[i].stageType;
                st.is = src.stages[i].is;
                e.stages[i] = st;
            }
        }
        return e;
    }
}

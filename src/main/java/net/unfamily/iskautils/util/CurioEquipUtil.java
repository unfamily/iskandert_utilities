package net.unfamily.iskautils.util;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.item.custom.NecroticCrystalHeartItem;
import net.unfamily.iskautils.item.custom.artifact.CursedCandleItem;
import net.unfamily.iskautils.item.custom.artifact.CursedArtifactItem;
import net.unfamily.iskautils.item.custom.artifact.TheDeceptionItem;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Small helper to detect if an item is "equipped" via Curios without a hard dependency.
 * We treat main/offhand as equipped as well (useful when Curios is not installed).
 */
public final class CurioEquipUtil {
    private static final String CURIOS_API_CLASS = "top.theillusivec4.curios.api.CuriosApi";

    private static volatile boolean inventoryApiProbed;
    private static volatile boolean inventoryApiAvailable;
    private static Method getCuriosInventoryMethod;

    private CurioEquipUtil() {}

    public static boolean hasEquipped(Player player, Item item) {
        if (player == null || item == null) return false;

        if (player.getMainHandItem().getItem() == item) return true;
        if (player.getOffhandItem().getItem() == item) return true;

        if (ModUtils.isCuriosLoaded()) {
            if (isInCuriosSlots(player, item)) return true;
        }

        return false;
    }

    public static void forEachEquippedCurioStack(Player player, Consumer<ItemStack> consumer) {
        if (player == null || consumer == null || !ModUtils.isCuriosLoaded()) {
            return;
        }
        forEachViaCuriosInventory(player, consumer);
    }

    private static boolean ensureInventoryApi() {
        if (inventoryApiProbed) {
            return inventoryApiAvailable;
        }
        synchronized (CurioEquipUtil.class) {
            if (inventoryApiProbed) {
                return inventoryApiAvailable;
            }
            try {
                Class<?> curiosApiClass = Class.forName(CURIOS_API_CLASS);
                getCuriosInventoryMethod = curiosApiClass.getMethod("getCuriosInventory", LivingEntity.class);
                inventoryApiAvailable = true;
            } catch (Throwable ignored) {
                inventoryApiAvailable = false;
            }
            inventoryApiProbed = true;
            return inventoryApiAvailable;
        }
    }

    private static void forEachViaCuriosInventory(Player player, Consumer<ItemStack> consumer) {
        if (!ensureInventoryApi()) {
            return;
        }
        try {
            Object curiosInventoryOpt = getCuriosInventoryMethod.invoke(null, player);
            if (!(curiosInventoryOpt instanceof Optional<?> opt) || opt.isEmpty()) {
                return;
            }
            Object curiosInventory = opt.get();
            Method getCurios = curiosInventory.getClass().getMethod("getCurios");
            Object curiosMap = getCurios.invoke(curiosInventory);
            if (!(curiosMap instanceof Map<?, ?> map)) {
                return;
            }
            for (Object slotInventory : map.values()) {
                Method getStacks = slotInventory.getClass().getMethod("getStacks");
                Object stacksHandler = getStacks.invoke(slotInventory);
                Method getSlots = stacksHandler.getClass().getMethod("getSlots");
                int slots = (Integer) getSlots.invoke(stacksHandler);
                Method getStackInSlot = stacksHandler.getClass().getMethod("getStackInSlot", int.class);
                for (int i = 0; i < slots; i++) {
                    ItemStack stack = (ItemStack) getStackInSlot.invoke(stacksHandler, i);
                    if (stack != null && !stack.isEmpty()) {
                        consumer.accept(stack);
                    }
                }
            }
        } catch (Throwable ignored) {
            // Curios not ready or API mismatch; avoid per-tick logging.
        }
    }

    public static boolean isStackEquippedInCurios(Player player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty() || !ModUtils.isCuriosLoaded()) {
            return false;
        }
        boolean[] found = {false};
        forEachEquippedCurioStack(player, curioStack -> {
            if (curioStack == stack) {
                found[0] = true;
            }
        });
        return found[0];
    }

    public static int countEquippedCurioStacks(Player player, Predicate<ItemStack> predicate) {
        if (player == null || predicate == null || !ModUtils.isCuriosLoaded()) {
            return 0;
        }
        int[] count = {0};
        forEachEquippedCurioStack(player, stack -> {
            if (predicate.test(stack)) {
                count[0]++;
            }
        });
        return count[0];
    }

    /**
     * Counts distinct {@link Item} types in Curios matching {@code itemPredicate}.
     * Duplicate stacks of the same item (e.g. two Busted Crowns) count once.
     */
    public static int countDistinctEquippedCurioItems(Player player, Predicate<Item> itemPredicate) {
        if (player == null || itemPredicate == null || !ModUtils.isCuriosLoaded()) {
            return 0;
        }
        Set<Item> seen = new HashSet<>();
        forEachEquippedCurioStack(player, stack -> {
            Item item = stack.getItem();
            if (itemPredicate.test(item)) {
                seen.add(item);
            }
        });
        return seen.size();
    }

    /**
     * Cursed artifacts counted for {@link net.unfamily.iskautils.events.CursedArtifactEffects} (Busted Crown).
     * Each cursed artifact type counts at most once in Curios; {@link ModItems#CURSED_CANDLE} also counts from inventory or hands.
     */
    public static int countEquippedCursedArtifacts(Player player) {
        if (player == null) {
            return 0;
        }
        int count = ModUtils.isCuriosLoaded()
                ? countDistinctEquippedCurioItems(player, CurioEquipUtil::isCursedArtifactItem)
                : 0;
        if (!findActiveStack(player, ModItems.CURSED_CANDLE.get()).isEmpty()) {
            if (!ModUtils.isCuriosLoaded() || !isInCuriosSlots(player, ModItems.CURSED_CANDLE.get())) {
                count++;
            }
        }
        return count;
    }

    public static ItemStack findEquippedCurioStack(Player player, Item item) {
        if (player == null || item == null || !ModUtils.isCuriosLoaded()) {
            return ItemStack.EMPTY;
        }
        ItemStack[] found = new ItemStack[1];
        forEachEquippedCurioStack(player, stack -> {
            if (found[0] == null && stack.is(item)) {
                found[0] = stack;
            }
        });
        return found[0] != null ? found[0] : ItemStack.EMPTY;
    }

    private static boolean isCursedArtifactItem(Item item) {
        return item instanceof CursedArtifactItem
                || item instanceof CursedCandleItem
                || item instanceof TheDeceptionItem
                || item instanceof NecroticCrystalHeartItem;
    }

    private static boolean isInCuriosSlots(Player player, Item item) {
        final boolean[] found = {false};
        forEachEquippedCurioStack(player, stack -> {
            if (stack.getItem() == item) {
                found[0] = true;
            }
        });
        return found[0];
    }

    /**
     * Finds an active item stack: Curios slots, then hands, then inventory.
     */
    public static ItemStack findActiveStack(Player player, Item item) {
        if (player == null || item == null) {
            return ItemStack.EMPTY;
        }
        ItemStack[] curioFound = new ItemStack[1];
        forEachEquippedCurioStack(player, stack -> {
            if (curioFound[0] == null && stack.is(item)) {
                curioFound[0] = stack;
            }
        });
        if (curioFound[0] != null) {
            return curioFound[0];
        }
        if (player.getMainHandItem().is(item)) {
            return player.getMainHandItem();
        }
        if (player.getOffhandItem().is(item)) {
            return player.getOffhandItem();
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}

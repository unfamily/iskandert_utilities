package net.unfamily.iskautils.util;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.unfamily.iskautils.item.custom.relic.CursedCandleItem;
import net.unfamily.iskautils.item.custom.relic.CursedRelicItem;

import java.lang.reflect.Method;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Small helper to detect if an item is "equipped" via Curios without a hard dependency.
 * We treat main/offhand as equipped as well (useful when Curios is not installed).
 */
public final class CurioEquipUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(CurioEquipUtil.class);

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
        if (forEachViaCuriosInventory(player, consumer)) {
            return;
        }
        forEachViaCuriosHelper(player, consumer);
    }

    private static boolean forEachViaCuriosInventory(Player player, Consumer<ItemStack> consumer) {
        try {
            Class<?> curiosApiClass = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            Method getCuriosInventory = curiosApiClass.getMethod("getCuriosInventory", LivingEntity.class);
            Object curiosInventoryOpt = getCuriosInventory.invoke(null, player);
            if (!(curiosInventoryOpt instanceof java.util.Optional<?> opt) || opt.isEmpty()) {
                return false;
            }
            Object curiosInventory = opt.get();
            Method getCurios = curiosInventory.getClass().getMethod("getCurios");
            Object curiosMap = getCurios.invoke(curiosInventory);
            if (!(curiosMap instanceof java.util.Map<?, ?> map)) {
                return false;
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
            return true;
        } catch (Throwable t) {
            LOGGER.debug("Curios inventory scan failed: {}", t.toString());
            return false;
        }
    }

    private static void forEachViaCuriosHelper(Player player, Consumer<ItemStack> consumer) {
        try {
            Class<?> curiosApiClass = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            Method getCuriosHelperMethod = curiosApiClass.getMethod("getCuriosHelper");
            Object curiosHelper = getCuriosHelperMethod.invoke(null);

            Method getEquippedCurios = curiosHelper.getClass().getMethod("getEquippedCurios", LivingEntity.class);
            Object equippedCurios = getEquippedCurios.invoke(curiosHelper, player);

            if (equippedCurios instanceof Iterable<?> items) {
                for (Object itemPair : items) {
                    Method getRight = itemPair.getClass().getMethod("getRight");
                    ItemStack stack = (ItemStack) getRight.invoke(itemPair);
                    if (stack != null && !stack.isEmpty()) {
                        consumer.accept(stack);
                    }
                }
            }
        } catch (Throwable t) {
            LOGGER.debug("Curios helper scan failed: {}", t.toString());
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

    public static int countEquippedCursedRelics(Player player) {
        return countEquippedCurioStacks(player, stack -> isCursedArtifactItem(stack.getItem()));
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
        return item instanceof CursedRelicItem || item instanceof CursedCandleItem;
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

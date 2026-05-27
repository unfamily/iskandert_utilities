package net.unfamily.iskautils.util;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

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

    private static boolean isInCuriosSlots(Player player, Item item) {
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
                    if (stack != null && stack.getItem() == item) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Throwable t) {
            LOGGER.debug("Curios equipped check failed: {}", t.toString());
            return false;
        }
    }
}


package net.unfamily.iskautils.util;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.unfamily.iskautils.stage.StageRegistry;

/**
 * Relic activation helper.
 *
 * Design constraint (from reliquie.txt):
 * - item inventoryTick runs for stacks in vanilla inventory too
 * - Curios stacks are not part of the vanilla inventory list
 *
 * Semantics:
 * - ACTIVE only when the stack is NOT in vanilla inventory and NOT in hands.
 * - This effectively means: stack is in Curios (or another non-vanilla slot extension).
 */
public final class RelicActivationUtil {
    private RelicActivationUtil() {}

    public static boolean isActiveInCurioOnly(Player player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) return false;
        if (isStackInHands(player, stack)) return false;
        return !ModUtils.isStackInVanillaPlayerInventory(player, stack);
    }

    public static void syncCurioOnlyStage(Player player, ItemStack stack, String stageId) {
        if (player == null || stageId == null || stageId.isBlank()) return;
        if (isActiveInCurioOnly(player, stack)) {
            StageRegistry.addPlayerStage(player, stageId, true);
        } else {
            StageRegistry.removePlayerStage(player, stageId, true);
        }
    }

    public static boolean isStackInHands(Player player, ItemStack stack) {
        ItemStack main = player.getItemBySlot(EquipmentSlot.MAINHAND);
        if (main == stack) return true;
        ItemStack off = player.getItemBySlot(EquipmentSlot.OFFHAND);
        return off == stack;
    }
}


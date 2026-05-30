package net.unfamily.iskautils.util;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.unfamily.iskalib.stage.StageRegistry;

/**
 * Artifact activation helper.
 *
 * Design constraint (from legacy design notes):
 * - item inventoryTick runs for stacks in vanilla inventory too
 * - Curios stacks are not part of the vanilla inventory list
 *
 * Semantics:
 * - ACTIVE only when the stack is NOT in vanilla inventory and NOT in hands.
 * - This effectively means: stack is in Curios (or another non-vanilla slot extension).
 */
public final class ArtifactActivationUtil {
    private ArtifactActivationUtil() {}

    public static boolean isActiveInCurioOnly(Player player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) return false;
        if (isStackInHands(player, stack)) return false;
        if (ModUtils.isStackInVanillaPlayerInventory(player, stack)) return false;
        return CurioEquipUtil.isStackEquippedInCurios(player, stack);
    }

    /**
     * True when at least one equipped Curios stack is {@code item} and that stack is not in vanilla inventory or hands
     * (same rule as {@link net.unfamily.iskautils.events.CurioEquipStageSync}).
     */
    public static boolean hasItemActiveInCurioOnly(Player player, Item item) {
        if (player == null || item == null || !ModUtils.isCuriosLoaded()) {
            return false;
        }
        boolean[] found = {false};
        CurioEquipUtil.forEachEquippedCurioStack(player, stack -> {
            if (!stack.is(item)) {
                return;
            }
            if (ModUtils.isStackInVanillaPlayerInventory(player, stack)) {
                return;
            }
            if (isStackInHands(player, stack)) {
                return;
            }
            found[0] = true;
        });
        return found[0];
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


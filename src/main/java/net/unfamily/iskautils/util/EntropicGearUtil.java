package net.unfamily.iskautils.util;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.unfamily.iskautils.item.ModItems;

/**
 * Helpers for entropic gear effects.
 */
public final class EntropicGearUtil {
    private EntropicGearUtil() {}

    public static boolean isEntropicArmor(Item item) {
        return item == ModItems.ENTROPIC_HELMET.get()
                || item == ModItems.ENTROPIC_CHESTPLATE.get()
                || item == ModItems.ENTROPIC_LEGGINGS.get()
                || item == ModItems.ENTROPIC_BOOTS.get();
    }

    public static boolean isWearing(Player player, Item armorItem) {
        for (ItemStack stack : player.getArmorSlots()) {
            if (stack.is(armorItem)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isHolding(Player player, Item... items) {
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        for (Item item : items) {
            if (main.is(item) || off.is(item)) {
                return true;
            }
        }
        return false;
    }

    public static int countEntropicArmorPieces(Player player, EquipmentSlot exclude) {
        int count = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!slot.isArmor()) {
                continue;
            }
            if (slot == exclude) {
                continue;
            }
            if (isEntropicArmor(player.getItemBySlot(slot).getItem())) {
                count++;
            }
        }
        return count;
    }

    /** Whole hearts/HP points missing (max health minus current health). */
    public static int missingHealthPoints(Player player) {
        return (int) Math.floor(Math.max(0.0D, player.getMaxHealth() - player.getHealth()));
    }

    public static boolean isEntropicArmorPenWeapon(ItemStack stack) {
        return stack.is(ModItems.ENTROPIC_SWORD.get())
                || stack.is(ModItems.ENTROPIC_AXE.get())
                || stack.is(ModItems.ENTROPIC_PAXEL.get());
    }

    public static boolean isEntropicPickaxeTool(ItemStack stack) {
        return stack.is(ModItems.ENTROPIC_PICKAXE.get()) || stack.is(ModItems.ENTROPIC_PAXEL.get());
    }

    public static boolean isEntropicAxeTool(ItemStack stack) {
        return stack.is(ModItems.ENTROPIC_AXE.get()) || stack.is(ModItems.ENTROPIC_PAXEL.get());
    }

    public static boolean isEntropicShovelTool(ItemStack stack) {
        return stack.is(ModItems.ENTROPIC_SHOVEL.get()) || stack.is(ModItems.ENTROPIC_PAXEL.get());
    }
}

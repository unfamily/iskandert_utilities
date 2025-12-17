package net.unfamily.iskautils.item.custom;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.unfamily.iskautils.stage.StageRegistry;

/**
 * Greedy Shield Item - When taking damage, has a chance to completely block it,
 * or if that fails, has a chance to reduce it significantly.
 */
public class GreedyShieldItem extends Item {

    public GreedyShieldItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        
        if (entity instanceof Player player) {
            // Verify if the item is in the vanilla inventory
            boolean isInVanillaInventory = false;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                if (player.getInventory().getItem(i) == stack) {
                    isInVanillaInventory = true;
                    break;
                }
            }
            
            // If the item is not in the vanilla inventory (i.e., it's in Curios), add the stage
            if (!isInVanillaInventory) {
                StageRegistry.addPlayerStage(player, "iska_utils_internal-greedy_shield_equip");
            }
        }
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack itemstack, Player entity) {
        StageRegistry.removePlayerStage(entity, "iska_utils_internal-greedy_shield_equip");
        return true;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }
}

package net.unfamily.iskautils.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.unfamily.iskalib.stage.StageRegistry;
import net.unfamily.iskautils.Config;
import java.util.List;
import java.util.function.Consumer;

/**
 * Greedy Shield Item - When taking damage, has a chance to completely block it,
 * or if that fails, has a chance to reduce it significantly.
 */
public class GreedyShieldItem extends Item {

    public GreedyShieldItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltipComponents, tooltipFlag);
        
        // Get values from config and convert to percentages
        int blockChancePercent = (int) Math.round(Config.greedyShieldBlockChance * 100);
        int reduceChancePercent = (int) Math.round(Config.greedyShieldReduceChance * 100);
        int reduceAmountPercent = (int) Math.round((1.0 - Config.greedyShieldReduceAmount) * 100); // Percentage blocked
        int remainingPercent = (int) Math.round(Config.greedyShieldReduceAmount * 100); // Percentage remaining
        
        tooltipComponents.accept(Component.translatable("tooltip.iska_utils.greedy_shield.desc0"));
        tooltipComponents.accept(Component.translatable("tooltip.iska_utils.greedy_shield.desc1", blockChancePercent)
                .withStyle(ChatFormatting.GRAY));
        tooltipComponents.accept(Component.translatable("tooltip.iska_utils.greedy_shield.desc2", reduceChancePercent)
                .withStyle(ChatFormatting.GRAY));
        tooltipComponents.accept(Component.translatable("tooltip.iska_utils.greedy_shield.desc3", reduceAmountPercent, remainingPercent)
                .withStyle(ChatFormatting.GRAY));
        
        if (Config.greedyShieldInfo) {
            tooltipComponents.accept(Component.translatable("tooltip.iska_utils.greedy_shield.info"));
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @org.jspecify.annotations.Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
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
}

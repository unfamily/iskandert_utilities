package net.unfamily.iskautils.item.custom;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.unfamily.iskautils.block.PotionPlateBlock;
import net.unfamily.iskautils.data.PotionPlateConfig;

import java.util.List;

/**
 * Custom BlockItem for Potion Plates to add tooltip descriptions
 */
public class PotionPlateBlockItem extends BlockItem {
    
    public PotionPlateBlockItem(Block block, Properties properties) {
        super(block, properties);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        
        // Get the config from the block
        if (this.getBlock() instanceof PotionPlateBlock potionPlateBlock) {
            PotionPlateConfig config = potionPlateBlock.getConfig();
            if (config != null && config.getTooltipLines() > 0) {
                // Add tooltip lines based on config
                String plateId = config.getPlateId();
                for (int i = 0; i < config.getTooltipLines(); i++) {
                    String key = "tooltip.iska_utils." + plateId.replace("-", "_") + ".desc" + i;
                    Component tooltipLine = Component.translatable(key);
                    // Only add if translation exists (not equal to the key itself)
                    if (!tooltipLine.getString().equals(key)) {
                        tooltip.add(tooltipLine);
                    }
                }
            }
        }
    }
}

package net.unfamily.iskautils.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Custom BlockItem for Sacred Rubber Sapling with glowing/enchant effect and tooltip
 */
public class SacredRubberSaplingBlockItem extends BlockItem {
    
    public SacredRubberSaplingBlockItem(@NotNull Block block, @NotNull Properties properties) {
        super(block, properties);
    }
    
    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        // Add the 'enchanted' glowing effect like Vector Charm
        return true;
    }
    
    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        
        // Add tooltip lines in gray
        tooltip.add(Component.translatable("tooltip.iska_utils.sacred_rubber_sapling.desc0")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.iska_utils.sacred_rubber_sapling.desc1")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.iska_utils.sacred_rubber_sapling.desc2")
                .withStyle(ChatFormatting.GRAY));
    }
}

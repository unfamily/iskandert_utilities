package net.unfamily.iskautils.item.custom;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import java.util.List;

public class MiningEquitizer extends Item {

    public MiningEquitizer(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        
        tooltipComponents.add(Component.translatable("tooltip.iska_utils.mining_equitizer.desc0"));
        tooltipComponents.add(Component.translatable("tooltip.iska_utils.mining_equitizer.desc1"));
    }
} 
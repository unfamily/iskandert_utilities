package net.unfamily.iskautils.item.custom;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class MiningEquitizer extends Item {
    private static final Logger LOGGER = LoggerFactory.getLogger(MiningEquitizer.class);
    
    public MiningEquitizer(Properties properties) {
        super(properties);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltipComponents, tooltipFlag);
        
        tooltipComponents.accept(Component.translatable("tooltip.iska_utils.mining_equitizer.desc0"));
        tooltipComponents.accept(Component.translatable("tooltip.iska_utils.mining_equitizer.desc1"));
    }
} 
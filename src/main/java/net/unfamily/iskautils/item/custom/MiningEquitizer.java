package net.unfamily.iskautils.item.custom;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.unfamily.iskautils.stage.StageRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import net.minecraft.world.entity.Entity;

public class MiningEquitizer extends Item {
    private static final Logger LOGGER = LoggerFactory.getLogger(MiningEquitizer.class);
    
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
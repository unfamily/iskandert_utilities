package net.unfamily.iskautils.item.custom;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.unfamily.iskautils.util.RelicActivationUtil;

public class MiningEquitizer extends Item {
    private static final Logger LOGGER = LoggerFactory.getLogger(MiningEquitizer.class);
    private static final String STAGE_ID = "iska_utils_internal-mining_equitizer_equip";
    
    public MiningEquitizer(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!(entity instanceof Player player)) return;
        RelicActivationUtil.syncCurioOnlyStage(player, stack, STAGE_ID);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        
        tooltipComponents.add(Component.translatable("tooltip.iska_utils.mining_equitizer.desc0"));
        tooltipComponents.add(Component.translatable("tooltip.iska_utils.mining_equitizer.desc1"));
    }
} 
package net.unfamily.iskautils.item.custom.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.unfamily.iskautils.util.RelicActivationUtil;

import java.util.List;

/**
 * Sharpened Bone relic.
 * Effects are implemented via event handlers; this item is just the carrier.
 */
public class SharpenedBoneItem extends Item {
    private static final String STAGE_ID = "iska_utils_internal-sharpened_bone_equip";

    public SharpenedBoneItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!(entity instanceof Player player)) return;
        RelicActivationUtil.syncCurioOnlyStage(player, stack, STAGE_ID);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.iska_utils.sharpened_bone.desc0"));
        tooltip.add(Component.translatable("tooltip.iska_utils.sharpened_bone.desc1"));
    }
}


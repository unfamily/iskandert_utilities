package net.unfamily.iskautils.item.custom.artifact;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.util.ArtifactActivationUtil;
import net.unfamily.iskautils.util.ArtifactBalanceFormat;
import net.unfamily.iskautils.util.ArtifactTooltipUtil;

import java.util.List;

/**
 * Sharpened Bone artifact.
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
        ArtifactActivationUtil.syncCurioOnlyStage(player, stack, STAGE_ID);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        ArtifactTooltipUtil.appendDescLines(
                tooltip,
                "sharpened_bone",
                2,
                2,
                ArtifactBalanceFormat.percent(Config.sharpenedBoneArmorIgnoreChance));
    }
}


package net.unfamily.iskautils.item.custom.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.util.RelicActivationUtil;
import net.unfamily.iskautils.util.RelicBalanceFormat;
import net.unfamily.iskautils.util.RelicTooltipUtil;

import java.util.function.Consumer;

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
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @org.jspecify.annotations.Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        if (!(entity instanceof Player player)) return;
        RelicActivationUtil.syncCurioOnlyStage(player, stack, STAGE_ID);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltip, flag);
        RelicTooltipUtil.appendDescLines(
                tooltip,
                "sharpened_bone",
                2,
                RelicBalanceFormat.flatBonus(Config.sharpenedBoneBonusDamage),
                RelicBalanceFormat.percent(Config.sharpenedBoneProcChance));
    }
}


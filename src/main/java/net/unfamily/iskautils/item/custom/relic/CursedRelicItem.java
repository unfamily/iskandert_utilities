package net.unfamily.iskautils.item.custom.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.unfamily.iskautils.util.RelicActivationUtil;

import java.util.List;

/**
 * Base class for cursed relics.
 * Concrete effects are implemented elsewhere (events / keybind integration).
 */
public class CursedRelicItem extends Item {
    public CursedRelicItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!(entity instanceof Player player)) return;
        ResourceLocation id = stack.getItem().builtInRegistryHolder().key().location();
        String stageId = "iska_utils_internal-" + id.getPath() + "_equip";
        RelicActivationUtil.syncCurioOnlyStage(player, stack, stageId);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        ResourceLocation id = stack.getItem().builtInRegistryHolder().key().location();
        String path = id.getPath();
        tooltip.add(Component.translatable("tooltip.iska_utils." + path + ".cursed"));
        tooltip.add(Component.translatable("tooltip.iska_utils." + path + ".desc0"));
        tooltip.add(Component.translatable("tooltip.iska_utils." + path + ".desc1"));
    }
}


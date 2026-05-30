package net.unfamily.iskautils.item.custom.artifact;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.util.ArtifactActivationUtil;
import net.unfamily.iskautils.util.ArtifactEquipStages;
import net.unfamily.iskautils.util.ArtifactTooltipUtil;

import java.util.List;

/**
 * The Chosen Cheese artifact.
 * Stores an internal level (Y) in NBT and applies up to a cap (X) from config.
 */
public class ChosenCheeseItem extends Item {
    public static final String NBT_LEVEL = "chosen_cheese_level";

    public ChosenCheeseItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static int getLevel(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 1;
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(NBT_LEVEL)) {
            return 1;
        }
        return Math.max(1, tag.getInt(NBT_LEVEL));
    }

    public static void setLevel(ItemStack stack, int level) {
        if (stack == null || stack.isEmpty()) return;
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(NBT_LEVEL, Math.max(1, level));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        int y = getLevel(stack);
        int x = Config.chosenCheeseMax;
        ArtifactTooltipUtil.appendDescLines(tooltip, "chosen_cheese", 2, 3, y, x);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!(entity instanceof Player player)) return;
        ArtifactActivationUtil.syncCurioOnlyStage(player, stack, ArtifactEquipStages.CHOSEN_CHEESE);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.pass(stack);
        }
        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }

        int foundSlot = -1;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s == stack) continue;
            if (!s.isEmpty() && s.getItem() == this) {
                foundSlot = i;
                break;
            }
        }
        if (foundSlot < 0) {
            return InteractionResultHolder.pass(stack);
        }

        ItemStack other = player.getInventory().getItem(foundSlot);
        int handLevel = getLevel(stack);
        int otherLevel = getLevel(other);
        if (handLevel >= Config.chosenCheeseMax || otherLevel >= Config.chosenCheeseMax) {
            return InteractionResultHolder.pass(stack);
        }

        int inc = Math.max(1, otherLevel);
        setLevel(stack, Math.min(Config.chosenCheeseMax, handLevel + inc));
        other.shrink(1);
        return InteractionResultHolder.success(stack);
    }
}

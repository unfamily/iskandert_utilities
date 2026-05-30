package net.unfamily.iskautils.item.custom.artifact;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.util.CustomModelDataUtil;
import net.unfamily.iskautils.util.ArtifactActivationUtil;
import net.unfamily.iskautils.util.ArtifactEquipStages;
import net.unfamily.iskautils.util.ArtifactTooltipUtil;

import java.util.function.Consumer;

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
        return Math.max(1, tag.getInt(NBT_LEVEL).orElse(1));
    }

    public static void setLevel(ItemStack stack, int level) {
        if (stack == null || stack.isEmpty()) return;
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(NBT_LEVEL, Math.max(1, level));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> output, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, output, flag);
        int y = getLevel(stack);
        int x = Config.chosenCheeseMax;
        ArtifactTooltipUtil.appendDescLines(output, "chosen_cheese", 2, 3, y, x);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        syncDisplayModel(stack);
        if (!(entity instanceof Player player)) return;
        ArtifactActivationUtil.syncCurioOnlyStage(player, stack, ArtifactEquipStages.CHOSEN_CHEESE);
    }

    /** Updates the full/partial item model via {@link DataComponents#CUSTOM_MODEL_DATA}. */
    public static void syncDisplayModel(ItemStack stack) {
        int y = getLevel(stack);
        int x = Config.chosenCheeseMax;
        CustomModelDataUtil.setFloat0(stack, y >= x ? 1.0F : 0.0F);
    }

    @Override
    public InteractionResult use(net.minecraft.world.level.Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResult.PASS;
        }
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
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
            return InteractionResult.PASS;
        }

        ItemStack other = player.getInventory().getItem(foundSlot);
        int handLevel = getLevel(stack);
        int otherLevel = getLevel(other);
        if (handLevel >= Config.chosenCheeseMax || otherLevel >= Config.chosenCheeseMax) {
            return InteractionResult.PASS;
        }

        int inc = Math.max(1, otherLevel);
        setLevel(stack, Math.min(Config.chosenCheeseMax, handLevel + inc));
        syncDisplayModel(stack);
        other.shrink(1);
        return InteractionResult.SUCCESS;
    }
}

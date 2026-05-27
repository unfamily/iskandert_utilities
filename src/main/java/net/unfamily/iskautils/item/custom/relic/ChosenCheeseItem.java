package net.unfamily.iskautils.item.custom.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.util.CurioEquipUtil;

import java.util.List;

/**
 * The Chosen Cheese relic.
 * Stores an internal level (Y) in NBT and applies up to a cap (X) from config.
 * Combining is implemented via a custom crafting recipe.
 */
public class ChosenCheeseItem extends Item {
    public static final String NBT_LEVEL = "chosen_cheese_level";

    public ChosenCheeseItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static int getLevel(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return Math.max(0, tag.getInt(NBT_LEVEL).orElse(0));
    }

    public static void setLevel(ItemStack stack, int level) {
        if (stack == null || stack.isEmpty()) return;
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(NBT_LEVEL, Math.max(0, level));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, java.util.function.Consumer<Component> output, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, output, flag);
        int y = getLevel(stack);
        int x = Config.chosenCheeseMax;
        output.accept(Component.translatable("tooltip.iska_utils.chosen_cheese.desc0", y, x));
    }

    @Override
    public void inventoryTick(ItemStack stack, net.minecraft.server.level.ServerLevel level, Entity entity, net.minecraft.world.entity.EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        if (!(entity instanceof Player player)) return;
        if (!CurioEquipUtil.hasEquipped(player, this)) return;

        int effective = Math.min(getLevel(stack), Config.chosenCheeseMax);
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) return;

        double desired = 20.0 + (double) effective;
        if (maxHealth.getBaseValue() != desired) {
            maxHealth.setBaseValue(desired);
            if (player.getHealth() > player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
            }
        }
    }
}


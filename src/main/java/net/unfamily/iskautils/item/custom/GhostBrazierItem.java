package net.unfamily.iskautils.item.custom;

import net.unfamily.iskautils.util.ModLogger;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.server.level.ServerLevel;
import net.unfamily.iskautils.data.GhostBrazierData;
import net.unfamily.iskautils.network.ModMessages;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.util.CurioEquipUtil;
import net.unfamily.iskautils.util.ModUtils;
import net.unfamily.iskautils.util.KeybindTooltipUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.component.TooltipDisplay;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Consumer;

/**
 * Ghost Brazier Item - Allows player to toggle between Survival and Spectator mode
 * when held in inventory and keybind is pressed
 */
public class GhostBrazierItem extends Item {
    private static final ModLogger LOGGER = ModLogger.of(GhostBrazierItem.class);

    public GhostBrazierItem(Properties properties) {
        super(properties);
    }

    /**
     * Checks if the player has a Ghost Brazier in inventory, hands, or Curios slots
     * @param player The player to check
     * @return true if the player has a Ghost Brazier
     */
    public static boolean hasGhostBrazier(Player player) {
        if (player == null) {
            return false;
        }
        return !CurioEquipUtil.findActiveStack(player, ModItems.GHOST_BRAZIER.get()).isEmpty();
    }

    public static void tickEquipped(ServerPlayer serverPlayer) {
        GhostBrazierData.setHasGhostBrazier(serverPlayer, true);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @org.jspecify.annotations.Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltip, flag);

        Component keybindName = KeybindTooltipUtil.keybindOrTranslation("key.iska_utils.ghost_brazier_toggle", "GHOST_BRAZIER_TOGGLE_KEY");

        // Show description
        tooltip.accept(Component.translatable("tooltip.iska_utils.ghost_brazier.desc0"));
        tooltip.accept(Component.translatable("tooltip.iska_utils.ghost_brazier.desc1", keybindName));
    }
}

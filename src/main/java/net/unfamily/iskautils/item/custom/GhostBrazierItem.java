package net.unfamily.iskautils.item.custom;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.unfamily.iskautils.client.KeyBindings;
import net.unfamily.iskautils.data.GhostBrazierData;
import net.unfamily.iskautils.network.ModMessages;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.util.CurioEquipUtil;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Ghost Brazier Item - Allows player to toggle between Survival and Spectator mode
 * when held in inventory and keybind is pressed
 */
public class GhostBrazierItem extends Item {

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
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (entity instanceof Player player && level.isClientSide) {
            if (KeyBindings.consumeGhostBrazierToggleKeyClick()) {
                ModMessages.sendGhostBrazierTogglePacket();
            }
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        // Get the keybind name
        String keybindName = KeyBindings.GHOST_BRAZIER_TOGGLE_KEY.getTranslatedKeyMessage().getString();

        // Show description
        tooltip.add(Component.translatable("tooltip.iska_utils.ghost_brazier.desc0"));
        tooltip.add(Component.translatable("tooltip.iska_utils.ghost_brazier.desc1", keybindName));
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }
}

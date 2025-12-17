package net.unfamily.iskautils.item.custom;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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
import net.unfamily.iskautils.util.ModUtils;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Ghost Brazier Item - Allows player to toggle between Survival and Spectator mode
 * when held in inventory and keybind is pressed
 */
public class GhostBrazierItem extends Item {
    private static final Logger LOGGER = LoggerFactory.getLogger(GhostBrazierItem.class);

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

        // Check hands first (highest priority)
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem() instanceof GhostBrazierItem) {
            return true;
        }

        ItemStack offHand = player.getOffhandItem();
        if (offHand.getItem() instanceof GhostBrazierItem) {
            return true;
        }

        // If Curios is loaded, check Curios slots (second priority)
        if (ModUtils.isCuriosLoaded()) {
            if (checkCuriosSlots(player)) {
                return true;
            }
        }

        // Check player inventory (lowest priority)
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof GhostBrazierItem) {
                return true;
            }
        }

        return false;
    }

    /**
     * Uses reflection to check if the Ghost Brazier is equipped in a Curios slot
     */
    private static boolean checkCuriosSlots(Player player) {
        try {
            Class<?> curioApiClass = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            Method getCuriosHandlerMethod = curioApiClass.getMethod("getCuriosHelper");
            Object curiosHelper = getCuriosHandlerMethod.invoke(null);
            Method getEquippedCurios = curiosHelper.getClass().getMethod("getEquippedCurios", LivingEntity.class);
            Object equippedCurios = getEquippedCurios.invoke(curiosHelper, player);
            
            if (equippedCurios instanceof Iterable<?> items) {
                for (Object itemPair : items) {
                    Method getStackMethod = itemPair.getClass().getMethod("getRight");
                    ItemStack stack = (ItemStack) getStackMethod.invoke(itemPair);
                    if (stack.getItem() instanceof GhostBrazierItem) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            LOGGER.warn("Error checking Curios slots for Ghost Brazier: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        // Execute only for players
        if (entity instanceof Player player) {
            // Handle keybind on client side
            if (level.isClientSide) {
                // Check if the ghost brazier keybind was pressed
                if (KeyBindings.consumeGhostBrazierToggleKeyClick()) {
                    // Send toggle request to server
                    ModMessages.sendGhostBrazierTogglePacket();
                }
            }

            // Mark that this player has a Ghost Brazier (server side)
            // Works for both normal inventory and Curios (Curios calls inventoryTick automatically)
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                GhostBrazierData.setHasGhostBrazier(serverPlayer, true);
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

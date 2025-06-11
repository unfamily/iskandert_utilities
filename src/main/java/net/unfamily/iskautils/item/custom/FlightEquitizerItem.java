package net.unfamily.iskautils.item.custom;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.unfamily.iskautils.IskaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.unfamily.iskautils.stage.StageRegistry;

import java.util.List;

/**
 * Flight Equitizer - An item that negates the mining speed penalty when flying in Minecraft
 */
@EventBusSubscriber(modid = IskaUtils.MOD_ID)
public class FlightEquitizerItem extends Item {
    private static final Logger LOGGER = LoggerFactory.getLogger(FlightEquitizerItem.class);
    
    public FlightEquitizerItem(Properties properties) {
        super(properties);
    }
    
    /**
     * This method is called every tick for every item in the inventory
     */
    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (entity instanceof Player player) {
            StageRegistry.addPlayerStage(player, "iska_utils_internal-flight_equitizer_equip");
        }
    }
    
    /**
     * Handles the mining event when the player is flying
     */
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();

        // If the player is on the ground or in water, do nothing
        if (player.onGround()) {
            return;
        }
            
        // Check if the player has the flight equitizer in the inventory
        if (StageRegistry.playerHasStage(player, "iska_utils_internal-flight_equitizer_equip")) {
            // Negate the mining speed penalty when flying
            // Minecraft normally applies a 5x penalty, so we multiply by 5
            event.setNewSpeed(event.getOriginalSpeed() * 5.0F);
        }
    }
} 
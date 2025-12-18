package net.unfamily.iskautils.events;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.stage.StageRegistry;

import java.time.LocalDate;

/**
 * Handler for date events.
 *  - Handler for giving gift item to players on login during December 20-30
 */
@EventBusSubscriber(modid = IskaUtils.MOD_ID)
public class DateEvents {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        LocalDate currentDate = LocalDate.now();
        int month = currentDate.getMonthValue();
        int day = currentDate.getDayOfMonth();
        int year = currentDate.getYear();

        // Check if date is between December 20-30
        if (month != 12 || day < 20 || day > 30) {
            return;
        }

        // Check if player already has the stage for this year
        String stageName = "iska_utils_internal-CH:" + year;
        StageRegistry registry = StageRegistry.getInstance(player.getServer());
        
        if (registry.hasPlayerStage(player, stageName)) {
            // Player already received gift this year
            return;
        }

        // Give gift item to player
        ItemStack giftStack = new ItemStack(ModItems.GIFT.get(), 1);
        
        // Try to add to inventory
        if (!player.getInventory().add(giftStack)) {
            // If inventory is full, drop the item
            player.drop(giftStack, false);
        }

        // Give the stage to prevent giving gift again this year
        registry.setPlayerStage(player, stageName, true);
    }
}

package net.unfamily.iskautils.events;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskalib.stage.StageRegistry;

import java.time.LocalDate;

/**
 * Handler for date events.
 *  - Gives the Gift item on login during December 20-30.
 *  - Gives the Entropic Champagne item on login during January 1-10.
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

        giveGift(player, month, day, year);
        giveChampagne(player, month, day, year);
    }

    private static void giveGift(ServerPlayer player, int month, int day, int year) {
        if (month != 12 || day < 20 || day > 30) {
            return;
        }
        String stageName = "iska_utils_internal-CH:" + year;
        StageRegistry registry = StageRegistry.getInstance(((ServerLevel) player.level()).getServer());
        if (registry.hasPlayerStage(player, stageName)) {
            return;
        }
        ItemStack giftStack = new ItemStack(ModItems.GIFT.get(), 1);
        if (!player.getInventory().add(giftStack)) {
            player.drop(giftStack, false);
        }
        registry.setPlayerStage(player, stageName, true);
    }

    private static void giveChampagne(ServerPlayer player, int month, int day, int year) {
        if (month != 1 || day < 1 || day > 10) {
            return;
        }
        String stageName = "iska_utils_internal-champagne:" + year;
        StageRegistry registry = StageRegistry.getInstance(((ServerLevel) player.level()).getServer());
        if (registry.hasPlayerStage(player, stageName)) {
            return;
        }
        ItemStack champagneStack = new ItemStack(ModItems.ENTROPIC_CHAMPAGNE.get(), 1);
        if (!player.getInventory().add(champagneStack)) {
            player.drop(champagneStack, false);
        }
        registry.setPlayerStage(player, stageName, true);
    }
}

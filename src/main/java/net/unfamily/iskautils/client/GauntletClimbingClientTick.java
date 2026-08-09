package net.unfamily.iskautils.client;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.item.custom.GauntletOfClimbingItem;
import net.unfamily.iskautils.util.CurioEquipUtil;

/**
 * Client-side wall climb: movement must run locally every tick (server-only setDeltaMovement is overwritten).
 */
@EventBusSubscriber(value = Dist.CLIENT)
public final class GauntletClimbingClientTick {
    private GauntletClimbingClientTick() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide()) {
            return;
        }
        ItemStack gauntlet = CurioEquipUtil.findActiveStack(player, ModItems.GAUNTLET_OF_CLIMBING.get());
        if (!gauntlet.isEmpty()) {
            GauntletOfClimbingItem.tickEquipped(player);
        }
    }
}

package net.unfamily.iskautils.events;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.item.custom.BurningBrazierItem;
import net.unfamily.iskautils.item.custom.FanpackItem;
import net.unfamily.iskautils.item.custom.GauntletOfClimbingItem;
import net.unfamily.iskautils.item.custom.GhostBrazierItem;
import net.unfamily.iskautils.item.custom.PortableDislocatorItem;
import net.unfamily.iskautils.util.CurioEquipUtil;

/**
 * Centralizes server ticks for Curio-backed items (Curios, hands, or inventory).
 */
@EventBusSubscriber
public final class CurioBackedItemTickHandler {
    private CurioBackedItemTickHandler() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }
        if (!(player instanceof ServerPlayer sp)) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        ItemStack gauntlet = CurioEquipUtil.findActiveStack(sp, ModItems.GAUNTLET_OF_CLIMBING.get());
        if (!gauntlet.isEmpty()) {
            GauntletOfClimbingItem.tickEquipped(sp);
        }

        ItemStack brazier = CurioEquipUtil.findActiveStack(sp, ModItems.BURNING_BRAZIER.get());
        if (!brazier.isEmpty()) {
            BurningBrazierItem.tickEquipped(sp, level, brazier);
        }

        ItemStack cursedCandle = CurioEquipUtil.findActiveStack(sp, ModItems.CURSED_CANDLE.get());
        if (!cursedCandle.isEmpty()) {
            BurningBrazierItem.tickEquipped(sp, level, cursedCandle);
        }

        ItemStack fanpack = CurioEquipUtil.findActiveStack(sp, ModItems.FANPACK.get());
        if (!fanpack.isEmpty() && fanpack.getItem() instanceof FanpackItem pack) {
            pack.tickEquipped(sp, fanpack, level);
        }

        ItemStack dislocator = CurioEquipUtil.findActiveStack(sp, ModItems.PORTABLE_DISLOCATOR.get());
        if (!dislocator.isEmpty()) {
            PortableDislocatorItem.tickEquipped(sp, dislocator, level);
        }

        if (!CurioEquipUtil.findActiveStack(sp, ModItems.GHOST_BRAZIER.get()).isEmpty()) {
            GhostBrazierItem.tickEquipped(sp);
        }
    }
}

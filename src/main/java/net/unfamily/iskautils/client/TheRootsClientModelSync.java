package net.unfamily.iskautils.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.item.custom.relic.TheRootsItem;
import net.unfamily.iskautils.util.CurioEquipUtil;

/**
 * Client-only: swap The Roots model to {@code the_root} on Unix-like OS (see reliquie easteregg).
 */
@EventBusSubscriber(value = Dist.CLIENT)
public final class TheRootsClientModelSync {
    private TheRootsClientModelSync() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        syncIfRoots(player.getMainHandItem());
        syncIfRoots(player.getOffhandItem());
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            syncIfRoots(player.getInventory().getItem(i));
        }
        CurioEquipUtil.forEachEquippedCurioStack(player, TheRootsClientModelSync::syncIfRoots);
    }

    private static void syncIfRoots(ItemStack stack) {
        if (stack.is(ModItems.THE_ROOTS.get())) {
            TheRootsItem.syncClientCustomModelData(stack);
        }
    }
}

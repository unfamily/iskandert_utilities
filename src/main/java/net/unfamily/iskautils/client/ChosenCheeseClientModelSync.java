package net.unfamily.iskautils.client;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.item.custom.artifact.ChosenCheeseItem;
import net.unfamily.iskautils.util.CurioEquipUtil;

/**
 * Keeps chosen cheese display models in sync on the client (including Curios slots).
 */
@EventBusSubscriber(value = Dist.CLIENT)
public final class ChosenCheeseClientModelSync {
    private ChosenCheeseClientModelSync() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide()) {
            return;
        }
        syncIfCheese(player.getMainHandItem());
        syncIfCheese(player.getOffhandItem());
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            syncIfCheese(player.getInventory().getItem(i));
        }
        CurioEquipUtil.forEachEquippedCurioStack(player, ChosenCheeseClientModelSync::syncIfCheese);
    }

    private static void syncIfCheese(ItemStack stack) {
        if (stack.is(ModItems.CHOSEN_CHEESE.get())) {
            ChosenCheeseItem.syncDisplayModel(stack);
        }
    }
}

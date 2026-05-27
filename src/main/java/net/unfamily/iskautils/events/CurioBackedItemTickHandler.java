package net.unfamily.iskautils.events;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.item.custom.BurningBrazierItem;
import net.unfamily.iskautils.item.custom.GauntletOfClimbingItem;
import net.unfamily.iskautils.util.CurioEquipUtil;

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

        ItemStack gauntlet = findActiveStack(sp, ModItems.GAUNTLET_OF_CLIMBING.get());
        if (gauntlet != null) {
            GauntletOfClimbingItem.tickEquipped(sp);
        }

        ItemStack brazier = findActiveStack(sp, ModItems.BURNING_BRAZIER.get());
        if (brazier != null) {
            BurningBrazierItem.tickEquipped(sp, level, brazier);
        }
    }

    private static ItemStack findActiveStack(ServerPlayer player, Item item) {
        ItemStack[] curioFound = new ItemStack[1];
        CurioEquipUtil.forEachEquippedCurioStack(player, stack -> {
            if (curioFound[0] == null && stack.is(item)) {
                curioFound[0] = stack;
            }
        });
        if (curioFound[0] != null) {
            return curioFound[0];
        }
        if (player.getMainHandItem().is(item)) {
            return player.getMainHandItem();
        }
        if (player.getOffhandItem().is(item)) {
            return player.getOffhandItem();
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) {
                return stack;
            }
        }
        return null;
    }
}

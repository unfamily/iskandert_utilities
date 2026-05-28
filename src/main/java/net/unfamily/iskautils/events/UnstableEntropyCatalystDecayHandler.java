package net.unfamily.iskautils.events;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.item.component.UnstableEntropyCatalystDecay;

@EventBusSubscriber(modid = IskaUtils.MOD_ID)
public final class UnstableEntropyCatalystDecayHandler {

    private UnstableEntropyCatalystDecayHandler() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!UnstableEntropyCatalystDecay.isDecayEnabled()) {
            return;
        }
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.is(ModItems.UNSTABLE_ENTROPY_CATALYST.get())) {
                continue;
            }
            int remaining = UnstableEntropyCatalystDecay.getRemainingTicks(stack);
            remaining--;
            if (remaining <= 0) {
                inv.setItem(i, ItemStack.EMPTY);
                if (Config.unstableEntropyCatalystDecayKillsPlayer) {
                    player.hurt(player.level().damageSources().magic(), Float.MAX_VALUE);
                }
            } else {
                UnstableEntropyCatalystDecay.setRemainingTicks(stack, remaining);
            }
        }
    }
}

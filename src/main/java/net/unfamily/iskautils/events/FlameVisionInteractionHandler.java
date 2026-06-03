package net.unfamily.iskautils.events;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.client.FlameVisibilityClient;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.network.packet.FlameVisionToggleC2SPacket;

@EventBusSubscriber(modid = IskaUtils.MOD_ID)
public final class FlameVisionInteractionHandler {

    private FlameVisionInteractionHandler() {}

    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        if (!event.getLevel().isClientSide()) {
            return;
        }
        tryToggleFromHand(event.getEntity());
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!event.getLevel().isClientSide()) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (!isFlameVisionItem(stack)) {
            return;
        }
        event.setCanceled(true);
        tryToggleFromHand(event.getEntity());
    }

    private static void tryToggleFromHand(Player player) {
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        if (!isFlameVisionItem(main) && !isFlameVisionItem(off)) {
            return;
        }
        boolean next = !FlameVisibilityClient.isGlobalFlameVisionEnabled();
        FlameVisibilityClient.applyGlobalFlameVision(next);
        PacketDistributor.sendToServer(new FlameVisionToggleC2SPacket(next));
        player.displayClientMessage(
                Component.translatable(
                        next
                                ? "message.iska_utils.flame_vision.enabled"
                                : "message.iska_utils.flame_vision.disabled"),
                true);
    }

    private static boolean isFlameVisionItem(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.is(ModItems.BURNING_BRAZIER.get()) || stack.is(ModItems.CURSED_CANDLE.get()));
    }
}

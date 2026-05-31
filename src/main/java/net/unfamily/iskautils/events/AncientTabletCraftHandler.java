package net.unfamily.iskautils.events;

import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.data.load.ancienttablet.AncientTabletCraftLogic;
import net.unfamily.iskautils.data.load.ancienttablet.AncientTabletRecipeEntry;
import net.unfamily.iskautils.data.load.ancienttablet.AncientTabletRecipeLoader;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.network.packet.AncientTabletCraftC2SPacket;

@EventBusSubscriber(modid = IskaUtils.MOD_ID)
public final class AncientTabletCraftHandler {

    private AncientTabletCraftHandler() {}

    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        if (event.getLevel().isClientSide) {
            PacketDistributor.sendToServer(new AncientTabletCraftC2SPacket());
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            tryCraftFromServer(sp);
        }
    }

    public static void tryCraftFromServer(ServerPlayer serverPlayer) {
        Level level = serverPlayer.level();
        if (level.isClientSide) {
            return;
        }
        ItemStack tablet = serverPlayer.getMainHandItem();
        if (!tablet.is(ModItems.ANCIENT_TABLET.get())) {
            return;
        }
        List<net.unfamily.iskautils.item.component.AncientTabletContents.SlotView> slots =
                AncientTabletCraftLogic.expandTabletSlots(tablet, serverPlayer.registryAccess());
        if (slots.isEmpty()) {
            return;
        }

        Optional<AncientTabletCraftLogic.CraftSuccess> success =
                AncientTabletCraftLogic.tryCraftTablet(slots, AncientTabletRecipeLoader.getEntries(), serverPlayer);
        if (success.isPresent()) {
            applySuccess(
                    serverPlayer,
                    level,
                    tablet,
                    success.get().resolved(),
                    success.get().consumedSlotIndices());
            return;
        }

        Optional<AncientTabletRecipeEntry> wrongOrder =
                AncientTabletCraftLogic.findWrongOrderMatch(
                        slots, AncientTabletRecipeLoader.getEntries(), serverPlayer);
        wrongOrder.ifPresent(entry -> handleWrongOrder(serverPlayer, tablet, entry));
    }

    private static void applySuccess(
            ServerPlayer player,
            Level level,
            ItemStack tablet,
            AncientTabletRecipeEntry.ResolvedCraft recipe,
            List<Integer> consumedIndices) {
        AncientTabletCraftLogic.consumeTabletAtIndices(tablet, player.registryAccess(), consumedIndices);
        AncientTabletCraftLogic.giveOutputsToPlayer(
                player, level, AncientTabletCraftLogic.outputStacks(recipe));
        player.playSound(SoundEvents.UI_STONECUTTER_TAKE_RESULT, 0.9f, 1.0f);
        player.containerMenu.broadcastChanges();
    }

    private static void handleWrongOrder(ServerPlayer player, ItemStack tablet, AncientTabletRecipeEntry recipe) {
        if (recipe.destroyIfWrong()) {
            net.unfamily.iskautils.item.component.AncientTabletContents.clear(tablet);
            player.playSound(SoundEvents.ITEM_BREAK, 0.9f, 0.9f);
            player.sendSystemMessage(
                    Component.translatable("message.iska_utils.ancient_tablet.wrong_order_destroyed"), true);
        } else {
            player.playSound(SoundEvents.VILLAGER_NO, 0.6f, 1.0f);
            player.sendSystemMessage(Component.translatable("message.iska_utils.ancient_tablet.wrong_order"), true);
        }
        player.containerMenu.broadcastChanges();
    }
}

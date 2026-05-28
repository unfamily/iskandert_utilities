package net.unfamily.iskautils.events;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.data.load.ancienttablet.AncientTabletRecipeEntry;
import net.unfamily.iskautils.data.load.ancienttablet.AncientTabletRecipeLoader;
import net.unfamily.iskautils.data.load.ancienttablet.AncientTabletRecipeMatcher;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.item.component.AncientTabletContents;
import net.unfamily.iskautils.network.packet.AncientTabletCraftC2SPacket;

import java.util.ArrayList;
import java.util.List;

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
        List<AncientTabletContents.SlotView> slots =
                new ArrayList<>(AncientTabletContents.expandForMatching(tablet, serverPlayer.registryAccess()));
        if (slots.isEmpty()) {
            return;
        }

        for (AncientTabletRecipeEntry recipe : AncientTabletRecipeLoader.getEntries()) {
            AncientTabletRecipeMatcher.MatchOutcome outcome =
                    AncientTabletRecipeMatcher.tryMatch(recipe, slots);
            switch (outcome.result()) {
                case SUCCESS -> {
                    applySuccess(serverPlayer, level, tablet, recipe, outcome.consumedSlotIndices());
                    return;
                }
                case WRONG_ORDER -> {
                    handleWrongOrder(serverPlayer, tablet, recipe);
                    return;
                }
                case NO_MATCH -> {
                }
            }
        }
    }

    private static void applySuccess(
            ServerPlayer player,
            Level level,
            ItemStack tablet,
            AncientTabletRecipeEntry recipe,
            List<Integer> consumedIndices) {
        AncientTabletContents.consumeSlotsAtIndices(tablet, player.registryAccess(), consumedIndices);
        List<ItemStack> outputs = AncientTabletRecipeMatcher.expandToExampleStacks(recipe.produce());
        for (ItemStack out : outputs) {
            if (out.isEmpty()) {
                continue;
            }
            ItemStack copy = out.copy();
            if (!player.getInventory().add(copy)) {
                ItemEntity drop = new ItemEntity(level, player.getX(), player.getY(), player.getZ(), copy);
                level.addFreshEntity(drop);
            }
        }
        player.playSound(SoundEvents.UI_STONECUTTER_TAKE_RESULT, 0.9f, 1.0f);
        player.containerMenu.broadcastChanges();
    }

    private static void handleWrongOrder(ServerPlayer player, ItemStack tablet, AncientTabletRecipeEntry recipe) {
        if (recipe.destroyIfWrong()) {
            AncientTabletContents.clear(tablet);
            // Failure with consumption/destruction of input.
            player.playSound(SoundEvents.ITEM_BREAK, 0.9f, 0.9f);
            player.displayClientMessage(
                    Component.translatable("message.iska_utils.ancient_tablet.wrong_order_destroyed"),
                    true);
        } else {
            // Failure without consumption.
            player.playSound(SoundEvents.VILLAGER_NO, 0.6f, 1.0f);
            player.displayClientMessage(
                    Component.translatable("message.iska_utils.ancient_tablet.wrong_order"),
                    true);
        }
        player.containerMenu.broadcastChanges();
    }
}

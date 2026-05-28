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
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.data.load.ancienttablet.AncientTabletRecipeEntry;
import net.unfamily.iskautils.data.load.ancienttablet.AncientTabletRecipeLoader;
import net.unfamily.iskautils.data.load.ancienttablet.AncientTabletRecipeMatcher;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.item.component.AncientTabletContents;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = IskaUtils.MOD_ID)
public final class AncientTabletCraftHandler {

    private AncientTabletCraftHandler() {}

    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        tryCraft(event.getEntity(), event.getLevel());
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        tryCraft(event.getEntity(), event.getLevel());
    }

    private static void tryCraft(Player player, Level level) {
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        ItemStack tablet = player.getMainHandItem();
        if (!tablet.is(ModItems.ANCIENT_TABLET.get())) {
            return;
        }
        List<ItemStack> slots = new ArrayList<>(AncientTabletContents.getSlots(tablet, serverPlayer.registryAccess()));
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
            player.playSound(SoundEvents.FIRE_EXTINGUISH, 0.8f, 0.6f);
        } else {
            player.displayClientMessage(
                    Component.translatable("message.iska_utils.ancient_tablet.wrong_order"),
                    true);
        }
        player.containerMenu.broadcastChanges();
    }
}

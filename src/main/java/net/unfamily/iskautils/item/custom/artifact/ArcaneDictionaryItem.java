package net.unfamily.iskautils.item.custom.artifact;

import net.unfamily.iskautils.util.ModLogger;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.unfamily.iskautils.arcane.ArcaneDictionaryContents;
import net.unfamily.iskautils.arcane.ArcaneDictionaryLoader;
import net.unfamily.iskautils.arcane.ArcaneDictionaryReroll;

import java.util.function.Consumer;

public class ArcaneDictionaryItem extends CursedArtifactItem {
    private static final ModLogger LOGGER = ModLogger.of(ArcaneDictionaryItem.class);

    public ArcaneDictionaryItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltip, flag);
        ArcaneDictionaryContents.appendTooltip(stack, flag, tooltip);
    }

    @Override
    public void onCraftedBy(ItemStack stack, Player player) {
        super.onCraftedBy(stack, player);
        tryRollInitialTraits(player.level(), player, stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        if (entity instanceof Player player) {
            tryRollInitialTraits(level, player, stack);
        }
    }

    private static void tryRollInitialTraits(Level level, Player player, ItemStack stack) {
        if (level.isClientSide() || !ArcaneDictionaryContents.needsInitialTraitRoll(stack)) {
            return;
        }
        if (ArcaneDictionaryLoader.getEntries().isEmpty()) {
            return;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        try {
            ArcaneDictionaryReroll.rollInitialTraits(serverPlayer, stack);
        } catch (Throwable t) {
            LOGGER.error("Failed to roll initial Arcane Dictionary traits for {}", player.getName().getString(), t);
        } finally {
            ArcaneDictionaryContents.markInitialRollAttempted(stack);
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
            ArcaneDictionaryReroll.tryReroll(sp, stack);
        }
        return InteractionResult.SUCCESS;
    }
}

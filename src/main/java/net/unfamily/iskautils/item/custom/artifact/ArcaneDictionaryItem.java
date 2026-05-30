package net.unfamily.iskautils.item.custom.artifact;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.unfamily.iskautils.arcane.ArcaneDictionaryContents;
import net.unfamily.iskautils.arcane.ArcaneDictionaryReroll;

import java.util.List;

public class ArcaneDictionaryItem extends CursedArtifactItem {

    public ArcaneDictionaryItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        ArcaneDictionaryContents.appendTooltip(stack, flag, tooltip::add);
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        super.onCraftedBy(stack, level, player);
        tryRollInitialTraits(level, player, stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (entity instanceof Player player) {
            tryRollInitialTraits(level, player, stack);
        }
    }

    private static void tryRollInitialTraits(Level level, Player player, ItemStack stack) {
        if (level.isClientSide() || !ArcaneDictionaryContents.isTraitless(stack)) {
            return;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            ArcaneDictionaryReroll.rollInitialTraits(serverPlayer, stack);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }
        if (level.isClientSide()) {
            return InteractionResultHolder.success(stack);
        }
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
            ArcaneDictionaryReroll.tryReroll(sp, stack);
        }
        return InteractionResultHolder.success(stack);
    }
}

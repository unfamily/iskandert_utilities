package net.unfamily.iskautils.item.entropic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BrushableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.iskautils.Config;

import java.util.Optional;

/**
 * Shared right-click behaviours for entropic tools (axe strip, shovel brush, hoe crop reset).
 */
public final class EntropicInteractions {
    private EntropicInteractions() {}

    public static InteractionResultHolder<ItemStack> onAxeUse(Level level, Player player, InteractionHand hand) {
        if (!Config.entropicAxeStripEnabled) {
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        }
        if (level.isClientSide()) {
            return InteractionResultHolder.success(player.getItemInHand(hand));
        }

        ItemStack tool = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            ItemStack offhand = player.getOffhandItem();
            if (offhand.isEmpty() || stripLogStack(offhand).isEmpty()) {
                return InteractionResultHolder.pass(tool);
            }
            int stripped = stripMatchingLogsInInventory(player, offhand.getItem());
            if (stripped > 0) {
                level.playSound(null, player.blockPosition(), SoundEvents.AXE_STRIP, SoundSource.PLAYERS, 1.0F, 1.0F);
                return InteractionResultHolder.success(tool);
            }
            return InteractionResultHolder.pass(tool);
        }

        ItemStack offhand = player.getOffhandItem();
        if (offhand.isEmpty()) {
            return InteractionResultHolder.pass(tool);
        }

        Optional<ItemStack> stripped = stripLogStack(offhand);
        if (stripped.isEmpty()) {
            return InteractionResultHolder.pass(tool);
        }

        int count = offhand.getCount();
        player.setItemInHand(InteractionHand.OFF_HAND, stripped.get().copyWithCount(count));
        level.playSound(null, player.blockPosition(), SoundEvents.AXE_STRIP, SoundSource.PLAYERS, 1.0F, 1.0F);
        return InteractionResultHolder.success(tool);
    }

    public static InteractionResult onShovelUseOn(UseOnContext context) {
        if (!Config.entropicShovelBrushEnabled) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }

        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BrushableBlock) || !(level.getBlockEntity(pos) instanceof BrushableBlockEntity brushable)) {
            return InteractionResult.PASS;
        }

        Direction direction = context.getClickedFace();
        ItemStack tool = context.getItemInHand();
        long baseTick = level.getGameTime();
        int brushTicks = Config.entropicShovelBrushTicks;
        for (int i = 0; i < brushTicks; i++) {
            if (brushable.brush(baseTick + i * 11L, player, direction)) {
                SoundEvent completeSound = state.getBlock() instanceof BrushableBlock brushableBlock
                        ? brushableBlock.getBrushSound()
                        : SoundEvents.BRUSH_SAND_COMPLETED;
                level.playSound(null, pos, completeSound, SoundSource.BLOCKS, 1.0F, 1.0F);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.SUCCESS;
    }

    public static InteractionResult onHoeUseOn(UseOnContext context) {
        if (!Config.entropicHoeCropPowerEnabled) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof CropBlock crop)) {
            return InteractionResult.PASS;
        }
        if (Config.entropicHoeRequireMatureCrop && !crop.isMaxAge(state)) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }

        ItemStack seed = crop.getCloneItemStack(level, pos, crop.getStateForAge(0));
        if (!seed.isEmpty()) {
            Block.popResource(level, pos, seed.copyWithCount(1));
        }
        level.setBlock(pos, crop.getStateForAge(0), 2);
        level.playSound(null, pos, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0F, 1.0F);
        return InteractionResult.SUCCESS;
    }

    /** Strips off-hand (if matched) and inventory stacks of the same log item type. */
    private static int stripMatchingLogsInInventory(Player player, Item logItem) {
        Optional<ItemStack> strippedTemplate = stripLogStack(new ItemStack(logItem));
        if (strippedTemplate.isEmpty()) {
            return 0;
        }

        int strippedCount = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty() || !stack.is(logItem)) {
                continue;
            }
            player.getInventory().setItem(i, strippedTemplate.get().copyWithCount(stack.getCount()));
            strippedCount += stack.getCount();
        }
        return strippedCount;
    }

    private static Optional<ItemStack> stripLogStack(ItemStack stack) {
        Block block = Block.byItem(stack.getItem());
        if (block == null || block == Blocks.AIR) {
            return Optional.empty();
        }
        BlockState strippedState = AxeItem.getAxeStrippingState(block.defaultBlockState());
        if (strippedState == null) {
            return Optional.empty();
        }
        return Optional.of(new ItemStack(strippedState.getBlock().asItem(), stack.getCount()));
    }
}

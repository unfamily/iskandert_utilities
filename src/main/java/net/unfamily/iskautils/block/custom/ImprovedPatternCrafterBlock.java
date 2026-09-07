package net.unfamily.iskautils.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.unfamily.iskautils.block.entity.ModBlockEntities;
import net.unfamily.iskautils.block.entity.ImprovedPatternCrafterBlockEntity;
import net.unfamily.iskautils.client.gui.ImprovedPatternCrafterMenu;
import org.jetbrains.annotations.Nullable;

/**
 * Block for the Improved Pattern Crafter machine.
 * Allows crafting based on user-defined patterns.
 * No facing — omnidirectional / non-orientable machine.
 */
public class ImprovedPatternCrafterBlock extends BaseEntityBlock {

    public static final MapCodec<ImprovedPatternCrafterBlock> CODEC = simpleCodec(ImprovedPatternCrafterBlock::new);

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public ImprovedPatternCrafterBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ImprovedPatternCrafterBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, ModBlockEntities.IMPROVED_PATTERN_CRAFTER_BE.get(),
                ImprovedPatternCrafterBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof ImprovedPatternCrafterBlockEntity) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.openMenu(new MenuProvider() {
                    @Override
                    public Component getDisplayName() {
                        return Component.translatable("block.iska_utils.improved_pattern_crafter");
                    }

                    @Override
                    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
                        return new ImprovedPatternCrafterMenu(id, inv, entity);
                    }
                }, pos);
            }
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ImprovedPatternCrafterBlockEntity pcbe) {
                dropHandlerContents(level, pos, pcbe.getInputHandler());
                dropHandlerContents(level, pos, pcbe.getOutputHandler());
                dropHandlerContents(level, pos, pcbe.getUpgradeHandler());
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    private static void dropHandlerContents(Level level, BlockPos pos, ItemStackHandler handler) {
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
        }
    }
}

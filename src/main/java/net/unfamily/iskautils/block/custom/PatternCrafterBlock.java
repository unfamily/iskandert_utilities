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
import net.unfamily.iskautils.block.entity.ImprovedPatternCrafterBlockEntity;
import net.unfamily.iskautils.block.entity.ModBlockEntities;
import net.unfamily.iskautils.block.entity.PatternCrafterBlockEntity;
import net.unfamily.iskautils.client.gui.ImprovedPatternCrafterMenu;
import org.jetbrains.annotations.Nullable;

/**
 * Normal Pattern Crafter block. Uses same GUI and logic as Improved;
 * config and behavior (no energy, no upgrades by default) come from PatternCrafterBlockEntity.
 * No facing — omnidirectional / non-orientable machine.
 */
public class PatternCrafterBlock extends BaseEntityBlock {

    public static final MapCodec<PatternCrafterBlock> CODEC = simpleCodec(PatternCrafterBlock::new);

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public PatternCrafterBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PatternCrafterBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, ModBlockEntities.PATTERN_CRAFTER_BE.get(),
                (lvl, pos, st, be) -> ImprovedPatternCrafterBlockEntity.serverTick(lvl, pos, st, be));
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
                        return Component.translatable("block.iska_utils.pattern_crafter");
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
    protected void affectNeighborsAfterRemoval(BlockState state, net.minecraft.server.level.ServerLevel level,
                                                BlockPos pos, boolean movedByPiston) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof ImprovedPatternCrafterBlockEntity pcbe) {
            dropHandlerContents(level, pos, pcbe.getInputHandler());
            dropHandlerContents(level, pos, pcbe.getOutputHandler());
            dropHandlerContents(level, pos, pcbe.getUpgradeHandler());
        }
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
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

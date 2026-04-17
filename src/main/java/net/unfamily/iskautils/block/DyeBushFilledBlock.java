package net.unfamily.iskautils.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.block.entity.DyeBushEmptyBlockEntity;
import net.unfamily.iskautils.block.entity.ModBlockEntities;
import net.unfamily.iskautils.block.entity.PassiveFilledBlockEntity;
import net.unfamily.iskautils.item.ModItems;
import org.jetbrains.annotations.Nullable;

/**
 * Filled dye bush block (full block, leaves-like).
 * Right-click (empty hand or any item) harvests 1 dye_berry and converts to empty.
 * Has a passive BlockEntity that ticks at normal rate but does nothing (no logic).
 */
public class DyeBushFilledBlock extends Block implements EntityBlock {
    public static final MapCodec<DyeBushFilledBlock> CODEC = simpleCodec(DyeBushFilledBlock::new);

    public DyeBushFilledBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        performHarvest(level, pos, player);
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        performHarvest(level, pos, player);
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
    }

    private void performHarvest(Level level, BlockPos pos, Player player) {
        if (level.isClientSide()) return;
        ItemStack berry = new ItemStack(ModItems.DYE_BERRY.get());
        if (!player.getInventory().add(berry)) {
            player.drop(berry, false);
        }
        level.setBlock(pos, ModBlocks.DYE_BUSH_EMPTY.get().defaultBlockState(), Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof DyeBushEmptyBlockEntity blockEntity) {
            int min = Config.MIN_DYE_BUSH_REFILL_TIME.get();
            int max = Config.MAX_DYE_BUSH_REFILL_TIME.get();
            blockEntity.setRefillTime(min + level.getRandom().nextInt(Math.max(1, max - min)));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PassiveFilledBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.PASSIVE_FILLED.get(), (lvl, pos, st, be) -> {});
    }

    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actualType, BlockEntityType<E> expectedType, BlockEntityTicker<? super E> ticker) {
        return expectedType == actualType ? (BlockEntityTicker<A>) ticker : null;
    }
}

package net.unfamily.iskautils.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.unfamily.iskautils.block.entity.ModBlockEntities;
import net.unfamily.iskautils.block.entity.TemporalOverclockerBlockEntity;
import net.unfamily.iskautils.item.ModItems;
import org.jetbrains.annotations.Nullable;

/**
 * Blocco Temporal Overclocker
 * Accelera i tick dei blocchi collegati tramite chipset
 */
public class TemporalOverclockerBlock extends Block implements EntityBlock {
    public static final BooleanProperty POWERED = net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED;
    public static final MapCodec<TemporalOverclockerBlock> CODEC = simpleCodec(TemporalOverclockerBlock::new);
    
    public TemporalOverclockerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(POWERED, false));
    }
    
    @Override
    public MapCodec<? extends TemporalOverclockerBlock> codec() {
        return CODEC;
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
    }
    
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TemporalOverclockerBlockEntity(pos, state);
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(blockEntityType, ModBlockEntities.TEMPORAL_OVERCLOCKER_BE.get(), 
                TemporalOverclockerBlockEntity::tick);
    }
    
    @Nullable
    protected static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actualType, BlockEntityType<E> expectedType, BlockEntityTicker<? super E> ticker) {
        return expectedType == actualType ? (BlockEntityTicker<A>) ticker : null;
    }
    
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof TemporalOverclockerBlockEntity overclocker) {
            ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
            
            // Se il giocatore tiene il chipset, mostra informazioni
            if (heldItem.is(ModItems.TEMPORAL_OVERCLOCKER_CHIPSET.get())) {
                // Il linking viene gestito dall'item
                return InteractionResult.PASS;
            }
            
            // Altrimenti mostra informazioni sui blocchi collegati
            var linkedBlocks = overclocker.getLinkedBlocks();
            if (linkedBlocks.isEmpty()) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("block.iska_utils.temporal_overclocker.no_links"));
            } else {
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("block.iska_utils.temporal_overclocker.links_count", linkedBlocks.size()));
            }
            
            return InteractionResult.CONSUME;
        }
        
        return InteractionResult.PASS;
    }
    
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        
        if (!level.isClientSide()) {
            boolean isPowered = level.hasNeighborSignal(pos);
            
            if (isPowered != state.getValue(POWERED)) {
                level.setBlock(pos, state.setValue(POWERED, isPowered), Block.UPDATE_ALL);
            }
        }
    }
    
}


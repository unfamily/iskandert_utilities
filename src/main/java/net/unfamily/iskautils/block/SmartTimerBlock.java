package net.unfamily.iskautils.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.server.level.ServerPlayer;
import net.unfamily.iskautils.block.entity.ModBlockEntities;
import net.unfamily.iskautils.block.entity.SmartTimerBlockEntity;

import javax.annotation.Nullable;

/**
 * Smart Timer Block
 * Un blocco che ogni X secondi emette un segnale redstone di durata Y secondi
 * Default: 5s di cooldown, 3s di durata del segnale
 */
public class SmartTimerBlock extends Block implements EntityBlock {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final DirectionProperty FACING = BlockStateProperties.FACING; // Supports all 6 directions
    public static final MapCodec<SmartTimerBlock> CODEC = simpleCodec(SmartTimerBlock::new);
    
    public SmartTimerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(POWERED, false)
                .setValue(FACING, Direction.NORTH));
    }
    
    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED, FACING);
    }
    
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Get the face that was clicked
        Direction clickedFace = context.getClickedFace();
        Direction facing;
        
        // If clicking on top or bottom face, use that direction directly (block faces up/down like player)
        if (clickedFace == Direction.UP || clickedFace == Direction.DOWN) {
            facing = clickedFace;
        } else {
            // For horizontal faces, use player's horizontal facing direction (block faces same direction as player)
            facing = context.getHorizontalDirection();
        }
        
        return this.defaultBlockState()
                .setValue(FACING, facing)
                .setValue(POWERED, false);
    }
    
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
    
    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }
    
    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        // Non emettere segnale dal front (faccia FACING)
        Direction facing = state.getValue(FACING);
        if (direction == facing) {
            return 0; // Front face non emette segnale
        }
        
        // Controlla se questa faccia è configurata come OUTPUT
        if (level.getBlockEntity(pos) instanceof net.unfamily.iskautils.block.entity.SmartTimerBlockEntity blockEntity) {
            byte ioConfig = blockEntity.getIoConfig(direction);
            if (ioConfig != 2) { // 2 = OUTPUT
                return 0; // Solo OUTPUT emette segnale
            }
        } else {
            // Se non c'è BlockEntity, usa comportamento legacy (emetti da tutte le facce tranne front)
            // Questo può accadere durante il caricamento iniziale
            return state.getValue(POWERED) ? 15 : 0;
        }
        
        return state.getValue(POWERED) ? 15 : 0;
    }
    
    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        // Non emettere segnale dal front (faccia FACING)
        Direction facing = state.getValue(FACING);
        if (direction == facing) {
            return 0; // Front face non emette segnale
        }
        
        // Controlla se questa faccia è configurata come OUTPUT
        if (level.getBlockEntity(pos) instanceof net.unfamily.iskautils.block.entity.SmartTimerBlockEntity blockEntity) {
            byte ioConfig = blockEntity.getIoConfig(direction);
            if (ioConfig != 2) { // 2 = OUTPUT
                return 0; // Solo OUTPUT emette segnale
            }
        } else {
            // Se non c'è BlockEntity, usa comportamento legacy (emetti da tutte le facce tranne front)
            return state.getValue(POWERED) ? 15 : 0;
        }
        
        return state.getValue(POWERED) ? 15 : 0;
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SmartTimerBlockEntity(pos, state);
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return createTickerHelper(
                blockEntityType,
                ModBlockEntities.SMART_TIMER_BE.get(),
                SmartTimerBlockEntity::tick
        );
    }
    
    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> typeCheck, BlockEntityType<E> typeExpected, BlockEntityTicker<? super E> ticker) {
        return typeExpected == typeCheck ? (BlockEntityTicker<A>) ticker : null;
    }
    
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        
        BlockEntity entity = level.getBlockEntity(pos);
        if (!(entity instanceof SmartTimerBlockEntity timer)) {
            return InteractionResult.PASS;
        }
        
        // Apri la GUI
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new net.minecraft.world.MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.translatable("block.iska_utils.smart_timer");
                }
                
                @Override
                public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inv, Player player) {
                    return new net.unfamily.iskautils.client.gui.SmartTimerMenu(id, inv, timer);
                }
            }, pos);
        }
        return InteractionResult.CONSUME;
    }
}

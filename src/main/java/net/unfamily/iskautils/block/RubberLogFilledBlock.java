package net.unfamily.iskautils.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.block.entity.RubberLogEmptyBlockEntity;
import net.unfamily.iskautils.item.ModItems;

import java.util.List;

/**
 * Rubber wood block with full sap.
 * Directional towards the 4 cardinal points.
 * When clicked with a treetap, releases sap and becomes an empty block.
 */
public class RubberLogFilledBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<RubberLogFilledBlock> CODEC = simpleCodec(RubberLogFilledBlock::new);
    
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public RubberLogFilledBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }
    
    @Override
    public MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
    
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }
    
    /**
     * Alternative implementation using our custom method instead of the standard use method.
     * Will be called by TreeTapItem instead of using the standard use method.
     */
    public InteractionResult onTapWithTreeTap(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand) {
        ItemStack itemInHand = player.getItemInHand(hand);
        
        // Only on server
        if (!level.isClientSide()) {
            // Damage the treetap if it's damageable and we're not in creative mode
            if (itemInHand.isDamageableItem() && !player.getAbilities().instabuild) {
                // Damage the item by 1
                int newDamage = itemInHand.getDamageValue() + 1;
                if (newDamage >= itemInHand.getMaxDamage()) {
                    // If the item would break, remove it
                    itemInHand.setCount(0);
                } else {
                    // Otherwise add damage
                    itemInHand.setDamageValue(newDamage);
                }
            }
            
            // Release a sap
            ItemStack sapStack = new ItemStack(ModItems.SAP.get());
            if (!player.getInventory().add(sapStack)) {
                player.drop(sapStack, false);
            }
            
            // Convert to an empty block
            int refillTime = Config.MIN_SAP_REFILL_TIME.get() + 
                    level.getRandom().nextInt(Config.MAX_SAP_REFILL_TIME.get() - Config.MIN_SAP_REFILL_TIME.get());
                    
            Direction facing = state.getValue(FACING);
            BlockState emptyState = ModBlocks.RUBBER_LOG_EMPTY.get().defaultBlockState()
                    .setValue(RubberLogEmptyBlock.FACING, facing);
                    
            level.setBlock(pos, emptyState, Block.UPDATE_ALL);
            
            // Get the block entity and set the refill time
            if (level.getBlockEntity(pos) instanceof RubberLogEmptyBlockEntity blockEntity) {
                blockEntity.setRefillTime(refillTime);
            }
        }
        
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter world, BlockPos pos, Direction face) {
        return 15;
    }
    
    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter world, BlockPos pos, Direction face) {
        return 10;
    }
} 
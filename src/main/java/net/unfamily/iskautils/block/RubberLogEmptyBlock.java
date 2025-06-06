package net.unfamily.iskautils.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.block.entity.RubberLogEmptyBlockEntity;
import net.unfamily.iskautils.item.ModItems;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Empty rubber wood block with no sap.
 * Directional towards the 4 cardinal points.
 * Refills after a certain time and becomes a filled block.
 */
public class RubberLogEmptyBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<RubberLogEmptyBlock> CODEC = simpleCodec(RubberLogEmptyBlock::new);
    
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public RubberLogEmptyBlock(Properties properties) {
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
     * Handles interaction with tree tap on empty rubber log.
     * Has a 75% chance to give sap and a 50% chance to convert to normal log.
     */
    public InteractionResult onTapWithTreeTap(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand) {
        ItemStack itemInHand = player.getItemInHand(hand);
        
        // Only on server side
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
            
            // Get the block entity
            boolean convertToNormalLog = false;
            if (level.getBlockEntity(pos) instanceof RubberLogEmptyBlockEntity blockEntity) {
                // Increase the refill timer by the same random amount as when creating a new one
                RandomSource random = level.getRandom();
                int additionalTime = Config.MIN_SAP_REFILL_TIME.get() + 
                        random.nextInt(Config.MAX_SAP_REFILL_TIME.get() - Config.MIN_SAP_REFILL_TIME.get());
                
                int currentTime = blockEntity.getRefillTime();
                blockEntity.setRefillTime(currentTime + additionalTime);
                
                // 75% chance to get sap
                if (random.nextFloat() < 0.75f) {
                    ItemStack sapStack = new ItemStack(ModItems.SAP.get());
                    if (!player.getInventory().add(sapStack)) {
                        player.drop(sapStack, false);
                    }
                }
                
                // 50% chance to convert to normal log
                if (random.nextFloat() < 0.5f) {
                    convertToNormalLog = true;
                }
            }
            
            // Convert to normal log if needed
            if (convertToNormalLog) {
                level.setBlock(pos, ModBlocks.RUBBER_LOG.get().defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
    
    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof RubberLogEmptyBlockEntity blockEntity) {
            // Check if refill time has expired
            if (blockEntity.shouldRefill()) {
                // Convert to a filled block using the BlockEntity method
                blockEntity.fillWithSap(level, pos, state);
            } else {
                // Schedule next tick
                level.scheduleTick(pos, this, 20); // Check every second
            }
        }
    }
    
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        // Schedule first tick
        if (!level.isClientSide()) {
            level.scheduleTick(pos, this, 20); // Check after 1 second
        }
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RubberLogEmptyBlockEntity(pos, state);
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter world, BlockPos pos, Direction face) {
        return 5;
    }
} 
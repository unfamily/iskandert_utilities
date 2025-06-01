package net.unfamily.iskautils.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.block.entity.RubberLogBlockEntity;
import net.unfamily.iskautils.item.ModItems;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class RubberLogBlock extends RotatedPillarBlock implements EntityBlock {
    private static final Logger LOGGER = LoggerFactory.getLogger(RubberLogBlock.class);
    public static final MapCodec<RubberLogBlock> CODEC = simpleCodec(RubberLogBlock::new);
    
    // Property to determine if this log has sap (25% chance during tree generation)
    public static final BooleanProperty HAS_SAP = BooleanProperty.create("has_sap");
    
    // Property to determine if the sap is filled or empty
    public static final BooleanProperty SAP_FILLED = BooleanProperty.create("sap_filled");
    
    // Direction where the sap tap is facing (if has_sap is true)
    public static final EnumProperty<Direction> TAP_FACING = BlockStateProperties.HORIZONTAL_FACING;

    @Override
    public @NotNull MapCodec<? extends RotatedPillarBlock> codec() {
        return CODEC;
    }

    public RubberLogBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(AXIS, Direction.Axis.Y)
                .setValue(HAS_SAP, false)
                .setValue(SAP_FILLED, false)
                .setValue(TAP_FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HAS_SAP, SAP_FILLED, TAP_FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Set random tap facing direction when placed and determine if it has sap based on chance
        boolean hasSap = context.getLevel().getRandom().nextFloat() < Config.rubberSapLogChance;
        Direction tapFacing = Direction.fromYRot(context.getRotation());
        
        return super.getStateForPlacement(context)
                .setValue(HAS_SAP, hasSap)
                .setValue(SAP_FILLED, hasSap) // If it has sap, it starts filled
                .setValue(TAP_FACING, tapFacing);
    }
    
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        
        // If the block has sap and is filled, guarantee a sap drop
        if (state.getValue(HAS_SAP) && state.getValue(SAP_FILLED)) {
            drops.add(new ItemStack(ModItems.SAP.get()));
        } 
        // Otherwise check for random sap drop based on config chance
        else if (params.getLevel().getRandom().nextFloat() < Config.rubberSapDropChance) {
            drops.add(new ItemStack(ModItems.SAP.get()));
        }
        
        return drops;
    }
    
    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Only refill sap if the block has sap capability but is empty
        if (state.getValue(HAS_SAP) && !state.getValue(SAP_FILLED)) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof RubberLogBlockEntity rubberLog) {
                rubberLog.tickSapRefill(level, pos, state, random);
            } else {
                // Se il blockEntity non esiste ma dovrebbe esserci, crealo
                LOGGER.warn("Missing BlockEntity for rubber log with has_sap=true at {}, creating one", pos);
                RubberLogBlockEntity newEntity = new RubberLogBlockEntity(pos, state);
                level.setBlockEntity(newEntity);
            }
        }
    }
    
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        
        // Assicurati che il BlockEntity esista se has_sap è true
        if (state.getValue(HAS_SAP) && level instanceof ServerLevel serverLevel) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity == null) {
                LOGGER.debug("Creating missing BlockEntity for rubber log at {}", pos);
                RubberLogBlockEntity newEntity = new RubberLogBlockEntity(pos, state);
                serverLevel.setBlockEntity(newEntity);
            }
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        // Only create a block entity if the log has sap
        if (state.getValue(HAS_SAP)) {
            return new RubberLogBlockEntity(pos, state);
        }
        return null;
    }
} 
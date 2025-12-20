package net.unfamily.iskautils.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.iskautils.block.entity.DeepDrawerInterfaceBlockEntity;
import net.unfamily.iskautils.block.entity.ModBlockEntities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Blocco interface per Deep Drawer
 * Exposes the content of adjacent Deep Drawer to other mods via IItemHandler capability
 */
public class DeepDrawerInterfaceBlock extends BaseEntityBlock {
    
    public static final MapCodec<DeepDrawerInterfaceBlock> CODEC = simpleCodec(DeepDrawerInterfaceBlock::new);
    
    public DeepDrawerInterfaceBlock(Properties properties) {
        super(properties);
    }
    
    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new DeepDrawerInterfaceBlockEntity(pos, state);
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(blockEntityType, ModBlockEntities.DEEP_DRAWER_INTERFACE.get(), 
                DeepDrawerInterfaceBlockEntity::serverTick);
    }
    
    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }
}

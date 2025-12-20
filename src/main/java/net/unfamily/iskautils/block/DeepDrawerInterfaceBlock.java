package net.unfamily.iskautils.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.iskautils.block.entity.DeepDrawerInterfaceBlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Blocco interface per Deep Drawer
 * Base block entity block without functionality (to be implemented later)
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
    
    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }
}

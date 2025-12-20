package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * BlockEntity for Deep Drawer Interface
 * Base block entity without functionality (to be implemented later)
 */
public class DeepDrawerInterfaceBlockEntity extends BlockEntity {
    
    public DeepDrawerInterfaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DEEP_DRAWER_INTERFACE.get(), pos, state);
    }
}

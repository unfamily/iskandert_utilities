package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * BlockEntity that does nothing. Used for filled blocks (rubber_log_filled, dye_bush_filled)
 * so that the block position always has a block entity and tick accelerators (e.g. Time in a Bottle)
 * continue to affect the area consistently.
 */
public class PassiveFilledBlockEntity extends BlockEntity {

    public PassiveFilledBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PASSIVE_FILLED.get(), pos, state);
    }
}

package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.iskautils.util.BlockCraftOwner;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * BlockEntity that does nothing. Used for filled blocks (rubber_log_filled, dye_bush_filled)
 * so that the block position always has a block entity and tick accelerators (e.g. Time in a Bottle)
 * continue to affect the area consistently.
 */
public class PassiveFilledBlockEntity extends BlockEntity implements net.unfamily.iskautils.util.BlockCraftOwner.BlockCraftOwnerHolder {

    public PassiveFilledBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PASSIVE_FILLED.get(), pos, state);
    }

    private java.util.UUID ownerPlayerUuid;

    @Override
    public @org.jetbrains.annotations.Nullable java.util.UUID getOwnerPlayerUuid() {
        return ownerPlayerUuid;
    }

    @Override
    public void setOwnerPlayerUuid(@org.jetbrains.annotations.Nullable java.util.UUID uuid) {
        this.ownerPlayerUuid = uuid;
    }

    public void claimOwner(net.minecraft.server.level.ServerPlayer player) {
        net.unfamily.iskautils.util.BlockCraftOwner.claimOnFirstOpen(this, player, ownerPlayerUuid);
    }

    public @org.jetbrains.annotations.Nullable net.minecraft.server.level.ServerPlayer resolveOwnerPlayer() {
        return net.unfamily.iskautils.util.BlockCraftOwner.resolve(level, ownerPlayerUuid);
    }


    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        BlockCraftOwner.write(output, ownerPlayerUuid);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ownerPlayerUuid = BlockCraftOwner.read(input);
    }
}

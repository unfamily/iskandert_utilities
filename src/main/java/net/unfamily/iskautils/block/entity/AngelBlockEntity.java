package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Block entity for the Angel Block
 * Used to store the "no_drop" tag
 */
public class AngelBlockEntity extends BlockEntity {
    
    public AngelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ANGEL_BLOCK_ENTITY.get(), pos, state);
    }
    
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (this.getPersistentData().contains("no_drop")) {
            boolean noDrop = this.getPersistentData().getBoolean("no_drop").orElse(false);
            output.putBoolean("no_drop", noDrop);
        }
    }
    
    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        boolean noDrop = input.getBooleanOr("no_drop", false);
        if (noDrop) {
            this.getPersistentData().putBoolean("no_drop", true);
        }
    }
    
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
    
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        return saveWithoutMetadata(provider);
    }
} 

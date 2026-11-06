package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.HolderLookup;
import net.unfamily.iskautils.util.BlockCraftOwner;

/**
 * Block entity for the Angel Block
 * Used to store the "no_drop" tag
 */
public class AngelBlockEntity extends BlockEntity implements net.unfamily.iskautils.util.BlockCraftOwner.BlockCraftOwnerHolder {
    
    public AngelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ANGEL_BLOCK_ENTITY.get(), pos, state);
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
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        BlockCraftOwner.write(tag, ownerPlayerUuid);
        
        // Save the "no_drop" tag if present
        if (this.getPersistentData().contains("no_drop")) {
            boolean noDrop = this.getPersistentData().getBoolean("no_drop");
            tag.putBoolean("no_drop", noDrop);
        }
    }
    
    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        ownerPlayerUuid = BlockCraftOwner.read(tag);
        
        // Load the "no_drop" tag if present
        if (tag.contains("no_drop")) {
            boolean noDrop = tag.getBoolean("no_drop");
            this.getPersistentData().putBoolean("no_drop", noDrop);
        }
    }
    
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
    
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, provider);
        return tag;
    }
    
    public CompoundTag getUpdateTag(CompoundTag tag, HolderLookup.Provider provider) {
        saveAdditional(tag, provider);
        return tag;
    }
} 

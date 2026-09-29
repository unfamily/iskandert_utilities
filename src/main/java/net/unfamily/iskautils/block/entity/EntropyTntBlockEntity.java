package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.iskautils.util.EntropyExplosionHelper;

public class EntropyTntBlockEntity extends BlockEntity {
    private EntropyExplosionHelper.Params params = EntropyExplosionHelper.Params.entropyTntDefaults();
    private boolean customized;

    public EntropyTntBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENTROPY_TNT_BE.get(), pos, state);
    }

    public EntropyExplosionHelper.Params getParams() {
        return customized ? params : EntropyExplosionHelper.Params.entropyTntDefaults();
    }

    public void setParams(EntropyExplosionHelper.Params params) {
        this.params = params;
        this.customized = true;
        setChanged();
    }

    public void loadFromItem(ItemStack stack) {
        EntropyExplosionHelper.Params defaults = EntropyExplosionHelper.Params.entropyTntDefaults();
        EntropyExplosionHelper.Params fromItem = EntropyExplosionHelper.Params.fromItemStack(stack, defaults);
        if (fromItem.equals(defaults)) {
            this.params = defaults;
            this.customized = false;
        } else {
            setParams(fromItem);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (customized) {
            params.writeToTag(tag);
            tag.putBoolean("Customized", true);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.getBoolean("Customized")
                || tag.contains(EntropyExplosionHelper.TAG_OR)
                || tag.contains(EntropyExplosionHelper.TAG_VR)
                || tag.contains(EntropyExplosionHelper.TAG_DAMAGE)) {
            this.params = EntropyExplosionHelper.Params.fromTag(tag, EntropyExplosionHelper.Params.entropyTntDefaults());
            this.customized = true;
        } else {
            this.params = EntropyExplosionHelper.Params.entropyTntDefaults();
            this.customized = false;
        }
    }
}

package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
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
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (customized) {
            output.putInt(EntropyExplosionHelper.TAG_OR, params.or());
            output.putInt(EntropyExplosionHelper.TAG_VR, params.vr());
            output.putInt(EntropyExplosionHelper.TAG_TICK_INTERVAL, params.tickInterval());
            output.putFloat(EntropyExplosionHelper.TAG_DAMAGE, params.damage());
            output.putBoolean(EntropyExplosionHelper.TAG_BREAK_UNBREAKABLE, params.breakUnbreakable());
            output.putBoolean("Customized", true);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        boolean customizedFlag = input.getBooleanOr("Customized", false);
        if (customizedFlag
                || input.getInt(EntropyExplosionHelper.TAG_OR).isPresent()
                || input.getInt(EntropyExplosionHelper.TAG_VR).isPresent()) {
            EntropyExplosionHelper.Params defaults = EntropyExplosionHelper.Params.entropyTntDefaults();
            this.params = new EntropyExplosionHelper.Params(
                    input.getIntOr(EntropyExplosionHelper.TAG_OR, defaults.or()),
                    input.getIntOr(EntropyExplosionHelper.TAG_VR, defaults.vr()),
                    input.getIntOr(EntropyExplosionHelper.TAG_TICK_INTERVAL, defaults.tickInterval()),
                    input.getFloatOr(EntropyExplosionHelper.TAG_DAMAGE, defaults.damage()),
                    input.getBooleanOr(EntropyExplosionHelper.TAG_BREAK_UNBREAKABLE, defaults.breakUnbreakable()));
            this.customized = true;
        } else {
            this.params = EntropyExplosionHelper.Params.entropyTntDefaults();
            this.customized = false;
        }
    }
}

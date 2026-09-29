package net.unfamily.iskautils.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.util.EntropyExplosionHelper;
import org.jetbrains.annotations.Nullable;

/**
 * Primed Entropy TNT with configurable fuse and ExplosionSystem detonation.
 */
public class PrimedEntropyTnt extends PrimedTnt {
    private static final EntityDataAccessor<Integer> DATA_OR =
            SynchedEntityData.defineId(PrimedEntropyTnt.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_VR =
            SynchedEntityData.defineId(PrimedEntropyTnt.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TICK_INTERVAL =
            SynchedEntityData.defineId(PrimedEntropyTnt.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_DAMAGE =
            SynchedEntityData.defineId(PrimedEntropyTnt.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_BREAK_UNBREAKABLE =
            SynchedEntityData.defineId(PrimedEntropyTnt.class, EntityDataSerializers.BOOLEAN);

    public PrimedEntropyTnt(EntityType<? extends PrimedEntropyTnt> type, Level level) {
        super(type, level);
        this.setBlockState(ModBlocks.ENTROPY_TNT.get().defaultBlockState());
        applyParams(EntropyExplosionHelper.Params.entropyTntDefaults());
        this.setFuse(Config.entropyTntFuseTicks);
    }

    public PrimedEntropyTnt(Level level, double x, double y, double z, @Nullable LivingEntity owner,
                            EntropyExplosionHelper.Params params) {
        this(ModEntities.PRIMED_ENTROPY_TNT.get(), level);
        this.setPos(x, y, z);
        double angle = level.getRandom().nextDouble() * (float) (Math.PI * 2);
        this.setDeltaMovement(-Math.sin(angle) * 0.02, 0.2F, -Math.cos(angle) * 0.02);
        this.setFuse(Config.entropyTntFuseTicks);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        applyParams(params);
        this.setBlockState(ModBlocks.ENTROPY_TNT.get().defaultBlockState());
    }

    public void applyParams(EntropyExplosionHelper.Params params) {
        this.entityData.set(DATA_OR, params.or());
        this.entityData.set(DATA_VR, params.vr());
        this.entityData.set(DATA_TICK_INTERVAL, params.tickInterval());
        this.entityData.set(DATA_DAMAGE, params.damage());
        this.entityData.set(DATA_BREAK_UNBREAKABLE, params.breakUnbreakable());
    }

    public EntropyExplosionHelper.Params getParams() {
        return new EntropyExplosionHelper.Params(
                this.entityData.get(DATA_OR),
                this.entityData.get(DATA_VR),
                this.entityData.get(DATA_TICK_INTERVAL),
                this.entityData.get(DATA_DAMAGE),
                this.entityData.get(DATA_BREAK_UNBREAKABLE));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        // Hardcoded registry defaults (config may not be baked yet); constructors call applyParams().
        builder.define(DATA_OR, 250);
        builder.define(DATA_VR, 50);
        builder.define(DATA_TICK_INTERVAL, 1);
        builder.define(DATA_DAMAGE, 1000.0F);
        builder.define(DATA_BREAK_UNBREAKABLE, true);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        EntropyExplosionHelper.Params params = getParams();
        output.putInt(EntropyExplosionHelper.TAG_OR, params.or());
        output.putInt(EntropyExplosionHelper.TAG_VR, params.vr());
        output.putInt(EntropyExplosionHelper.TAG_TICK_INTERVAL, params.tickInterval());
        output.putFloat(EntropyExplosionHelper.TAG_DAMAGE, params.damage());
        output.putBoolean(EntropyExplosionHelper.TAG_BREAK_UNBREAKABLE, params.breakUnbreakable());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        EntropyExplosionHelper.Params defaults = EntropyExplosionHelper.Params.entropyTntDefaults();
        applyParams(new EntropyExplosionHelper.Params(
                input.getIntOr(EntropyExplosionHelper.TAG_OR, defaults.or()),
                input.getIntOr(EntropyExplosionHelper.TAG_VR, defaults.vr()),
                input.getIntOr(EntropyExplosionHelper.TAG_TICK_INTERVAL, defaults.tickInterval()),
                input.getFloatOr(EntropyExplosionHelper.TAG_DAMAGE, defaults.damage()),
                input.getBooleanOr(EntropyExplosionHelper.TAG_BREAK_UNBREAKABLE, defaults.breakUnbreakable())));
    }

    @Override
    protected void explode() {
        BlockPos pos = BlockPos.containing(this.getX(), this.getY(0.0625), this.getZ());
        EntropyExplosionHelper.create(this.level(), pos, getParams());
    }
}

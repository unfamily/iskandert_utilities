package net.unfamily.iskautils.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.unfamily.iskautils.block.TheDeceptionBlock;
import org.jetbrains.annotations.Nullable;

/**
 * Invisible zero-size seat entity (Refurbished Furniture {@code Seat} pattern).
 */
public class DeceptionSeatEntity extends Entity {
    private static final EntityDataAccessor<Boolean> DATA_LOCK_YAW =
            SynchedEntityData.defineId(DeceptionSeatEntity.class, EntityDataSerializers.BOOLEAN);

    public DeceptionSeatEntity(EntityType<? extends DeceptionSeatEntity> type, Level level) {
        super(type, level);
    }

    private DeceptionSeatEntity(Level level, BlockPos pos, double seatHeight, float seatYaw, boolean lockYaw) {
        this(ModEntities.DECEPTION_SEAT.get(), level);
        this.setPos(Vec3.atBottomCenterOf(pos).add(0.0D, seatHeight, 0.0D));
        this.setRot(seatYaw, 0.0F);
        this.entityData.set(DATA_LOCK_YAW, lockYaw);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_LOCK_YAW, false);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public void tick() {
        super.tick();
        Level level = this.level();
        if (!level.isClientSide()) {
            BlockPos pos = this.blockPosition();
            if (this.getPassengers().isEmpty()
                    || !(level.getBlockState(pos).getBlock() instanceof TheDeceptionBlock)) {
                this.discard();
            }
        }
    }

    @Override
    protected void addPassenger(Entity entity) {
        super.addPassenger(entity);
        entity.setYRot(this.getYRot());
    }

    @Override
    public void onPassengerTurned(Entity entity) {
        this.clampPassengerYaw(entity);
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity entity) {
        Direction front = this.entityData.get(DATA_LOCK_YAW) ? this.getDirection() : entity.getDirection();
        Direction[] sides = {front, front.getClockWise(), front.getCounterClockWise(), front.getOpposite()};
        for (Direction side : sides) {
            Vec3 pos = DismountHelper.findSafeDismountLocation(
                    entity.getType(), this.level(), this.blockPosition().relative(side), false);
            if (pos != null) {
                return pos.add(0.0D, 0.25D, 0.0D);
            }
        }
        return super.getDismountLocationForPassenger(entity);
    }

    private void clampPassengerYaw(Entity entity) {
        if (!this.entityData.get(DATA_LOCK_YAW)) {
            return;
        }
        entity.setYBodyRot(this.getYRot());
        float wrappedYaw = Mth.wrapDegrees(entity.getYRot() - this.getYRot());
        float clampedYaw = Mth.clamp(wrappedYaw, -120.0F, 120.0F);
        entity.yRotO += clampedYaw - wrappedYaw;
        entity.setYRot(entity.getYRot() + clampedYaw - wrappedYaw);
        entity.setYHeadRot(entity.getYRot());
    }

    public static boolean sit(Player player, BlockPos pos, double seatHeight, @Nullable Direction direction) {
        Level level = player.level();
        if (!level.isClientSide() && availableAt(level, pos)) {
            float seatYaw = direction != null ? direction.toYRot() : player.getYRot();
            DeceptionSeatEntity seat = new DeceptionSeatEntity(level, pos, seatHeight, seatYaw, direction != null);
            level.addFreshEntity(seat);
            if (player.startRiding(seat)) {
                return true;
            }
            seat.discard();
        }
        return false;
    }

    public static boolean availableAt(Level level, BlockPos pos) {
        return level.getEntitiesOfClass(DeceptionSeatEntity.class, new AABB(pos)).isEmpty();
    }
}

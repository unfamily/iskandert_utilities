package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.util.DruidicPodzolSpawnRules;
import net.unfamily.iskautils.util.DruidicPodzolUtil;
import net.unfamily.iskautils.util.SoilAcceleratedSpawn;

import java.util.Optional;

public class DruidicPodzolBlockEntity extends BlockEntity {
    private static final String TAG_COOLDOWN = "spawn_cooldown";
    private static final String TAG_ACCEL_COOLDOWN = "accel_spawn_cooldown";
    private static final String TAG_PATCH_REDSTONE = "patch_redstone";

    private int spawnCooldownTicks = -1;
    private int accelSpawnCooldownTicks = -1;
    private boolean patchRedstoneActive;

    public DruidicPodzolBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DRUIDIC_PODZOL_BE.get(), pos, state);
    }

    public static void tickServer(Level level, BlockPos pos, BlockState state, DruidicPodzolBlockEntity blockEntity) {
        if (level.isClientSide() || !(level instanceof ServerLevel server)) {
            return;
        }
        blockEntity.tickSpawn(server);
    }

    public static void requestAcceleratedWave(ServerLevel level, BlockPos soilPos) {
        if (!Config.druidicPodzolRedstoneAccelEnabled || !Config.druidicPodzolSpawnEnabled) {
            return;
        }
        DruidicPodzolUtil.queueImmediateAccelSpawn(level, soilPos);
    }

    private void tickSpawn(ServerLevel level) {
        if (spawnCooldownTicks < 0) {
            DruidicPodzolUtil.refreshPatchRedstoneState(level, worldPosition);
        }
        if (patchRedstoneActive) {
            tickAcceleratedSpawn(level);
        } else {
            tickSlowSpawn(level);
        }
    }

    private void tickSlowSpawn(ServerLevel level) {
        if (spawnCooldownTicks < 0) {
            rollNormalCooldown(level.getRandom());
            setChanged();
        }
        if (spawnCooldownTicks > 0) {
            spawnCooldownTicks--;
            return;
        }
        if (canSpawnHere(level)) {
            trySpawnAtPodzol(level, worldPosition, false);
        }
        rollNormalCooldown(level.getRandom());
        setChanged();
    }

    private void tickAcceleratedSpawn(ServerLevel level) {
        if (accelSpawnCooldownTicks < 0) {
            accelSpawnCooldownTicks = 0;
            setChanged();
        }
        if (accelSpawnCooldownTicks > 0) {
            accelSpawnCooldownTicks--;
            return;
        }
        if (canSpawnHere(level)) {
            trySpawnAtPodzol(level, worldPosition, true);
        }
        accelSpawnCooldownTicks = DruidicPodzolUtil.rollInclusive(
                Config.druidicPodzolRedstoneAccelMinTicks,
                Config.druidicPodzolRedstoneAccelMaxTicks,
                level.getRandom());
        setChanged();
    }

    private boolean canSpawnHere(ServerLevel level) {
        return Config.druidicPodzolSpawnEnabled
                && DruidicPodzolUtil.isLitSpawnCandidate(level, worldPosition);
    }

    private void rollNormalCooldown(RandomSource random) {
        spawnCooldownTicks = DruidicPodzolUtil.rollInclusive(
                Config.druidicPodzolSpawnIntervalMinTicks,
                Config.druidicPodzolSpawnIntervalMaxTicks,
                random);
    }

    private static boolean trySpawnAtPodzol(ServerLevel level, BlockPos spawnSoil, boolean accelerated) {
        int cap = accelerated ? Config.druidicPodzolAccelMobCap : SoilAcceleratedSpawn.PER_BLOCK_MOB_CAP;
        if (!SoilAcceleratedSpawn.isUnderCreatureCap(level, spawnSoil, cap, accelerated)) {
            return false;
        }
        BlockPos spawnPos = DruidicPodzolUtil.findMobSpawnPos(level, spawnSoil);
        if (spawnPos == null) {
            return false;
        }
        RandomSource random = level.getRandom();
        Optional<MobSpawnSettings.SpawnerData> pick = DruidicPodzolSpawnRules.pickSpawnEntry(level, spawnSoil, random);
        if (pick.isEmpty()) {
            return false;
        }
        EntityType<?> spawnType = pick.get().type();
        if (spawnType == null) {
            return false;
        }
        if (!DruidicPodzolSpawnRules.isValidSpawnContext(level, spawnType, spawnPos, spawnSoil)) {
            return false;
        }
        if (!(spawnType.create(level, EntitySpawnReason.MOB_SUMMONED) instanceof Mob mob)) {
            return false;
        }
        mob.setPos(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D);
        mob.setYRot(random.nextFloat() * 360.0F);
        if (!mob.checkSpawnObstruction(level)) {
            mob.discard();
            return false;
        }
        if (!DruidicPodzolSpawnRules.isWithinSpawnMaxHealth(mob)) {
            mob.discard();
            return false;
        }
        if (mob instanceof AgeableMob ageableMob) {
            ageableMob.setAge(0);
        }
        if (level.addFreshEntity(mob)) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, spawnPos.getX() + 0.5D, spawnPos.getY() + 0.2D, spawnPos.getZ() + 0.5D,
                    6, 0.25D, 0.1D, 0.25D, 0.01D);
            return true;
        }
        return false;
    }

    public static void onPodzolPlaced(ServerLevel level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof DruidicPodzolBlockEntity be) {
            be.spawnCooldownTicks = -1;
            be.accelSpawnCooldownTicks = -1;
            be.setChanged();
            DruidicPodzolUtil.refreshPatchRedstoneState(level, pos);
        }
    }

    public void applyPatchRedstoneState(boolean active) {
        patchRedstoneActive = active;
    }

    public void queueImmediateAccelSpawn() {
        accelSpawnCooldownTicks = 0;
        setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt(TAG_COOLDOWN, spawnCooldownTicks);
        output.putInt(TAG_ACCEL_COOLDOWN, accelSpawnCooldownTicks);
        output.putBoolean(TAG_PATCH_REDSTONE, patchRedstoneActive);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        spawnCooldownTicks = input.getInt(TAG_COOLDOWN).orElse(-1);
        accelSpawnCooldownTicks = input.getInt(TAG_ACCEL_COOLDOWN)
                .or(() -> input.getInt("accel_wave_cooldown"))
                .orElse(-1);
        patchRedstoneActive = input.getBooleanOr(TAG_PATCH_REDSTONE, false);
    }
}

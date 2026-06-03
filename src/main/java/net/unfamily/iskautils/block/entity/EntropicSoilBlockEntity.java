package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.effect.ModMobEffects;
import net.unfamily.iskautils.util.EntropicSoilSpawnRules;
import net.unfamily.iskautils.util.EntropicSoilUtil;
import net.unfamily.iskautils.util.SoilAcceleratedSpawn;

import java.util.Optional;

public class EntropicSoilBlockEntity extends BlockEntity {
    private static final String TAG_COOLDOWN = "spawn_cooldown";
    private static final String TAG_ACCEL_COOLDOWN = "accel_spawn_cooldown";
    private static final String TAG_PATCH_REDSTONE = "patch_redstone";
    private static final String TAG_LIGHT_DECAY = "light_decay_ticks";
    private static final String TAG_LIGHT_DECAY_TARGET = "light_decay_target";

    private static final int LIGHT_DECAY_INTERVAL = 4;

    private int spawnCooldownTicks = -1;
    private int accelSpawnCooldownTicks = -1;
    private boolean patchRedstoneActive;
    private int lightDecayTicks;
    private int lightDecayTarget = -1;

    public EntropicSoilBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENTROPIC_SOIL_BE.get(), pos, state);
    }

    public static void tickServer(Level level, BlockPos pos, BlockState state, EntropicSoilBlockEntity blockEntity) {
        if (level.isClientSide() || !(level instanceof ServerLevel server)) {
            return;
        }
        long schedule = server.getGameTime() + pos.asLong();
        if ((schedule & (LIGHT_DECAY_INTERVAL - 1L)) == 0L) {
            blockEntity.tickLightDecay(server, LIGHT_DECAY_INTERVAL);
        }
        blockEntity.tickSpawn(server);
    }

    public static void requestAcceleratedWave(ServerLevel level, BlockPos soilPos) {
        if (!Config.entropicSoilRedstoneAccelEnabled || !Config.entropicSoilSpawnEnabled) {
            return;
        }
        EntropicSoilUtil.queueImmediateAccelSpawn(level, soilPos);
    }

    private void tickSpawn(ServerLevel level) {
        if (spawnCooldownTicks < 0) {
            EntropicSoilUtil.refreshPatchRedstoneState(level, worldPosition);
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
            trySpawnAtSoil(level, worldPosition);
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
            trySpawnAtSoil(level, worldPosition);
        }
        accelSpawnCooldownTicks = EntropicSoilUtil.rollInclusive(
                Config.entropicSoilRedstoneAccelMinTicks,
                Config.entropicSoilRedstoneAccelMaxTicks,
                level.getRandom());
        setChanged();
    }

    private boolean canSpawnHere(ServerLevel level) {
        return Config.entropicSoilSpawnEnabled
                && level.getDifficulty() != Difficulty.PEACEFUL
                && EntropicSoilUtil.isDarkSpawnCandidate(level, worldPosition);
    }

    private void tickLightDecay(ServerLevel level, int elapsed) {
        if (!EntropicSoilUtil.isIlluminated(level, worldPosition)) {
            if (lightDecayTicks != 0 || lightDecayTarget != -1) {
                lightDecayTicks = 0;
                lightDecayTarget = -1;
                setChanged();
            }
            return;
        }
        if (lightDecayTarget < 0) {
            lightDecayTarget = EntropicSoilUtil.rollInclusive(
                    Config.entropicSoilToDirtMinTicks,
                    Config.entropicSoilToDirtMaxTicks,
                    level.getRandom());
            setChanged();
        }
        lightDecayTicks += elapsed;
        if (lightDecayTicks >= lightDecayTarget) {
            level.setBlock(worldPosition, ModBlocks.ENTROPIC_DIRT.get().defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    private void rollNormalCooldown(RandomSource random) {
        spawnCooldownTicks = EntropicSoilUtil.rollInclusive(
                Config.entropicSoilSpawnIntervalMinTicks,
                Config.entropicSoilSpawnIntervalMaxTicks,
                random);
    }

    private static boolean trySpawnAtSoil(ServerLevel level, BlockPos spawnSoil) {
        if (!SoilAcceleratedSpawn.isUnderHostileCap(level, spawnSoil, Config.entropicSoilAccelMobCap)) {
            return false;
        }
        BlockPos spawnPos = EntropicSoilUtil.findMobSpawnPos(level, spawnSoil);
        if (spawnPos == null) {
            return false;
        }
        RandomSource random = level.getRandom();
        Optional<MobSpawnSettings.SpawnerData> pick = EntropicSoilSpawnRules.pickSpawnEntry(level, spawnSoil, random);
        if (pick.isEmpty()) {
            return false;
        }
        EntityType<?> spawnType = pick.get().type();
        if (spawnType == null) {
            return false;
        }
        if (!EntropicSoilSpawnRules.isValidSpawnContext(level, spawnType, spawnPos, spawnSoil)) {
            return false;
        }
        if (!(spawnType.create(level, EntitySpawnReason.MOB_SUMMONED) instanceof Mob mob)) {
            return false;
        }
        mob.setPos(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D);
        mob.setYRot(random.nextFloat() * 360.0F);
        if (!mob.checkSpawnObstruction(level)) {
            return false;
        }
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), EntitySpawnReason.MOB_SUMMONED, null);
        if (!EntropicSoilSpawnRules.isWithinSpawnMaxHealth(mob)) {
            mob.discard();
            return false;
        }
        mob.addEffect(new MobEffectInstance(
                ModMobEffects.ENTROPIC_EMPOWERMENT,
                MobEffectInstance.INFINITE_DURATION,
                0,
                true,
                true,
                true));
        if (level.addFreshEntity(mob)) {
            level.sendParticles(ParticleTypes.SMOKE, spawnPos.getX() + 0.5D, spawnPos.getY() + 0.2D, spawnPos.getZ() + 0.5D,
                    8, 0.25D, 0.1D, 0.25D, 0.01D);
            return true;
        }
        return false;
    }

    public static void onSoilPlaced(ServerLevel level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof EntropicSoilBlockEntity be) {
            be.spawnCooldownTicks = -1;
            be.accelSpawnCooldownTicks = -1;
            be.lightDecayTicks = 0;
            be.lightDecayTarget = -1;
            be.setChanged();
            EntropicSoilUtil.refreshPatchRedstoneState(level, pos);
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
        output.putInt(TAG_LIGHT_DECAY, lightDecayTicks);
        output.putInt(TAG_LIGHT_DECAY_TARGET, lightDecayTarget);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        spawnCooldownTicks = input.getInt(TAG_COOLDOWN).orElse(-1);
        accelSpawnCooldownTicks = input.getInt(TAG_ACCEL_COOLDOWN)
                .or(() -> input.getInt("accel_wave_cooldown"))
                .orElse(-1);
        patchRedstoneActive = input.getBooleanOr(TAG_PATCH_REDSTONE, false);
        lightDecayTicks = input.getInt(TAG_LIGHT_DECAY).orElse(0);
        lightDecayTarget = input.getInt(TAG_LIGHT_DECAY_TARGET).orElse(-1);
    }
}

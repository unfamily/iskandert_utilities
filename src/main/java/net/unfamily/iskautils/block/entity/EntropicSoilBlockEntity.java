package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.effect.ModMobEffects;
import net.unfamily.iskautils.util.EntropicSoilSpawnRules;
import net.unfamily.iskautils.util.EntropicSoilUtil;

import java.util.List;
import java.util.Optional;

public class EntropicSoilBlockEntity extends BlockEntity {
    private static final String TAG_COOLDOWN = "spawn_cooldown";
    private static final String TAG_REDSTONE_ACCEL = "redstone_accel";
    private static final String TAG_LIGHT_DECAY = "light_decay_ticks";
    private static final String TAG_LIGHT_DECAY_TARGET = "light_decay_target";

    private static final int LIGHT_DECAY_INTERVAL = 4;
    private static final int REDSTONE_CHECK_INTERVAL = 10;
    private static final int SPAWN_ATTEMPTS = 8;

    private int spawnCooldownTicks = -1;
    private boolean redstoneAccelerating;
    private int lightDecayTicks;
    private int lightDecayTarget = -1;
    private int redstoneCheckCooldown;

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

        if (!EntropicSoilUtil.isNetworkLeader(server, pos)) {
            return;
        }

        List<BlockPos> component = EntropicSoilUtil.collectConnectedSoil(server, pos);
        if (component.isEmpty()) {
            return;
        }
        blockEntity.tickNetwork(server, component);
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

    private void tickNetwork(ServerLevel level, List<BlockPos> component) {
        if (spawnCooldownTicks < 0) {
            rollNormalCooldown(level.getRandom());
            setChanged();
        }

        boolean spawnAllowed = Config.entropicSoilSpawnEnabled
                && level.getDifficulty() != Difficulty.PEACEFUL
                && EntropicSoilUtil.hasAnyDarkSpawnCandidate(level, component);

        if (!redstoneAccelerating
                && Config.entropicSoilRedstoneAccelEnabled
                && Config.entropicSoilSpawnEnabled
                && level.getDifficulty() != Difficulty.PEACEFUL) {
            if (redstoneCheckCooldown <= 0) {
                redstoneCheckCooldown = REDSTONE_CHECK_INTERVAL;
                if (EntropicSoilUtil.hasRedstoneSignal(level, component)) {
                    int accelTicks = EntropicSoilUtil.rollInclusive(
                            Config.entropicSoilRedstoneAccelMinTicks,
                            Config.entropicSoilRedstoneAccelMaxTicks,
                            level.getRandom());
                    if (spawnCooldownTicks > accelTicks) {
                        spawnCooldownTicks = accelTicks;
                        redstoneAccelerating = true;
                        setChanged();
                    }
                }
            } else {
                redstoneCheckCooldown--;
            }
        }

        if (spawnCooldownTicks > 0) {
            spawnCooldownTicks--;
            return;
        }

        if (spawnAllowed) {
            trySpawnMob(level, component);
        }
        rollNormalCooldown(level.getRandom());
        redstoneAccelerating = false;
        setChanged();
    }

    private void rollNormalCooldown(RandomSource random) {
        spawnCooldownTicks = EntropicSoilUtil.rollInclusive(
                Config.entropicSoilSpawnIntervalMinTicks,
                Config.entropicSoilSpawnIntervalMaxTicks,
                random);
    }

    private void trySpawnMob(ServerLevel level, List<BlockPos> component) {
        List<BlockPos> candidates = EntropicSoilUtil.filterDarkSpawnCandidates(level, component);
        if (candidates.isEmpty()) {
            return;
        }
        RandomSource random = level.getRandom();
        for (int attempt = 0; attempt < SPAWN_ATTEMPTS; attempt++) {
            BlockPos spawnSoil = candidates.get(random.nextInt(candidates.size()));
            BlockPos spawnPos = EntropicSoilUtil.findMobSpawnPos(level, spawnSoil);
            if (spawnPos == null) {
                continue;
            }
            Optional<MobSpawnSettings.SpawnerData> pick = EntropicSoilSpawnRules.pickSpawnEntry(level, spawnSoil, random);
            if (pick.isEmpty()) {
                continue;
            }
            EntityType<?> spawnType = pick.get().type;
            if (spawnType == null) {
                continue;
            }
            if (!EntropicSoilSpawnRules.isValidSpawnContext(level, spawnType, spawnPos, spawnSoil)) {
                continue;
            }
            if (!(spawnType.create(level) instanceof Mob mob)) {
                continue;
            }
            mob.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D,
                    random.nextFloat() * 360.0F, 0.0F);
            if (!mob.checkSpawnObstruction(level)) {
                continue;
            }
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.MOB_SUMMONED, null);
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
                return;
            }
        }
    }

    public static void onSoilPlaced(ServerLevel level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof EntropicSoilBlockEntity be) {
            be.spawnCooldownTicks = -1;
            be.lightDecayTicks = 0;
            be.lightDecayTarget = -1;
            be.redstoneCheckCooldown = 0;
            be.setChanged();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt(TAG_COOLDOWN, spawnCooldownTicks);
        tag.putBoolean(TAG_REDSTONE_ACCEL, redstoneAccelerating);
        tag.putInt(TAG_LIGHT_DECAY, lightDecayTicks);
        tag.putInt(TAG_LIGHT_DECAY_TARGET, lightDecayTarget);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        spawnCooldownTicks = tag.getInt(TAG_COOLDOWN);
        redstoneAccelerating = tag.getBoolean(TAG_REDSTONE_ACCEL);
        lightDecayTicks = tag.getInt(TAG_LIGHT_DECAY);
        lightDecayTarget = tag.contains(TAG_LIGHT_DECAY_TARGET) ? tag.getInt(TAG_LIGHT_DECAY_TARGET) : -1;
        redstoneCheckCooldown = 0;
    }
}

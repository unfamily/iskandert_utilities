package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.util.DruidicPodzolSpawnRules;
import net.unfamily.iskautils.util.DruidicPodzolUtil;

import java.util.List;
import java.util.Optional;

public class DruidicPodzolBlockEntity extends BlockEntity {
    private static final String TAG_COOLDOWN = "spawn_cooldown";
    private static final String TAG_REDSTONE_ACCEL = "redstone_accel";

    private static final int REDSTONE_CHECK_INTERVAL = 10;
    private static final int SPAWN_ATTEMPTS = 8;

    private int spawnCooldownTicks = -1;
    private boolean redstoneAccelerating;
    private int redstoneCheckCooldown;

    public DruidicPodzolBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DRUIDIC_PODZOL_BE.get(), pos, state);
    }

    public static void tickServer(Level level, BlockPos pos, BlockState state, DruidicPodzolBlockEntity blockEntity) {
        if (level.isClientSide() || !(level instanceof ServerLevel server)) {
            return;
        }
        if (!DruidicPodzolUtil.isNetworkLeader(server, pos)) {
            return;
        }

        List<BlockPos> component = DruidicPodzolUtil.collectConnectedPodzol(server, pos);
        if (component.isEmpty()) {
            return;
        }
        blockEntity.tickNetwork(server, component);
    }

    private void tickNetwork(ServerLevel level, List<BlockPos> component) {
        if (spawnCooldownTicks < 0) {
            rollNormalCooldown(level.getRandom());
            setChanged();
        }

        boolean spawnAllowed = Config.druidicPodzolSpawnEnabled
                && DruidicPodzolUtil.hasAnyLitSpawnCandidate(level, component);

        if (!redstoneAccelerating
                && Config.druidicPodzolRedstoneAccelEnabled
                && Config.druidicPodzolSpawnEnabled) {
            if (redstoneCheckCooldown <= 0) {
                redstoneCheckCooldown = REDSTONE_CHECK_INTERVAL;
                if (DruidicPodzolUtil.hasRedstoneSignal(level, component)) {
                    int accelTicks = DruidicPodzolUtil.rollInclusive(
                            Config.druidicPodzolRedstoneAccelMinTicks,
                            Config.druidicPodzolRedstoneAccelMaxTicks,
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
            trySpawnAnimal(level, component);
        }
        rollNormalCooldown(level.getRandom());
        redstoneAccelerating = false;
        setChanged();
    }

    private void rollNormalCooldown(RandomSource random) {
        spawnCooldownTicks = DruidicPodzolUtil.rollInclusive(
                Config.druidicPodzolSpawnIntervalMinTicks,
                Config.druidicPodzolSpawnIntervalMaxTicks,
                random);
    }

    private void trySpawnAnimal(ServerLevel level, List<BlockPos> component) {
        List<BlockPos> candidates = DruidicPodzolUtil.filterLitSpawnCandidates(level, component);
        if (candidates.isEmpty()) {
            return;
        }
        RandomSource random = level.getRandom();
        for (int attempt = 0; attempt < SPAWN_ATTEMPTS; attempt++) {
            BlockPos spawnSoil = candidates.get(random.nextInt(candidates.size()));
            BlockPos spawnPos = DruidicPodzolUtil.findMobSpawnPos(level, spawnSoil);
            if (spawnPos == null) {
                continue;
            }
            Optional<MobSpawnSettings.SpawnerData> pick = DruidicPodzolSpawnRules.pickSpawnEntry(level, spawnSoil, random);
            if (pick.isEmpty()) {
                continue;
            }
            EntityType<?> spawnType = pick.get().type();
            if (spawnType == null) {
                continue;
            }
            if (!DruidicPodzolSpawnRules.isValidSpawnContext(level, spawnType, spawnPos, spawnSoil)) {
                continue;
            }
            if (!(spawnType.create(level, EntitySpawnReason.MOB_SUMMONED) instanceof Mob mob)) {
                continue;
            }
            mob.setPos(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D);
            mob.setYRot(random.nextFloat() * 360.0F);
            if (!mob.checkSpawnObstruction(level)) {
                continue;
            }
            if (mob instanceof AgeableMob ageableMob) {
                ageableMob.setAge(0);
            }
            if (level.addFreshEntity(mob)) {
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, spawnPos.getX() + 0.5D, spawnPos.getY() + 0.2D, spawnPos.getZ() + 0.5D,
                        6, 0.25D, 0.1D, 0.25D, 0.01D);
                return;
            }
        }
    }

    public static void onPodzolPlaced(ServerLevel level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof DruidicPodzolBlockEntity be) {
            be.spawnCooldownTicks = -1;
            be.redstoneCheckCooldown = 0;
            be.setChanged();
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt(TAG_COOLDOWN, spawnCooldownTicks);
        output.putBoolean(TAG_REDSTONE_ACCEL, redstoneAccelerating);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        spawnCooldownTicks = input.getInt(TAG_COOLDOWN).orElse(-1);
        redstoneAccelerating = input.getBooleanOr(TAG_REDSTONE_ACCEL, false);
        redstoneCheckCooldown = 0;
    }
}

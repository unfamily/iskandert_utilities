package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.util.RandomRollUtil;

import java.util.List;

public class GraveyardSoilBlockEntity extends BlockEntity {
    private static final String TAG_HEAL_COOLDOWN = "heal_cooldown";
    private static final int TICK_INTERVAL = 5;

    private int healCooldownTicks = -1;

    public GraveyardSoilBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GRAVEYARD_SOIL_BE.get(), pos, state);
    }

    public static void tickServer(Level level, BlockPos pos, BlockState state, GraveyardSoilBlockEntity blockEntity) {
        if (level.isClientSide() || !(level instanceof ServerLevel server)) {
            return;
        }
        if ((server.getGameTime() + pos.asLong()) % TICK_INTERVAL != 0L) {
            return;
        }
        blockEntity.advance(server, pos, TICK_INTERVAL);
    }

    private void advance(ServerLevel level, BlockPos pos, int elapsed) {
        if (healCooldownTicks < 0) {
            rollHealCooldown(level);
            setChanged();
        }
        if (healCooldownTicks > 0) {
            healCooldownTicks = Math.max(0, healCooldownTicks - elapsed);
            return;
        }
        healUndead(level, pos);
        rollHealCooldown(level);
        setChanged();
    }

    private void rollHealCooldown(ServerLevel level) {
        healCooldownTicks = RandomRollUtil.rollInclusive(
                Config.graveyardSoilHealIntervalMinTicks,
                Config.graveyardSoilHealIntervalMaxTicks,
                level.getRandom());
    }

    private void healUndead(ServerLevel level, BlockPos pos) {
        AABB box = new AABB(pos).inflate(0.0D, 0.1D, 0.0D);
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, box,
                entity -> entity.isAlive() && entity.getType().is(EntityTypeTags.UNDEAD));
        if (entities.isEmpty()) {
            return;
        }
        float amount = (float) Config.graveyardSoilHealAmount;
        for (LivingEntity entity : entities) {
            if (entity.getHealth() < entity.getMaxHealth()) {
                entity.heal(amount);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt(TAG_HEAL_COOLDOWN, healCooldownTicks);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        healCooldownTicks = tag.getInt(TAG_HEAL_COOLDOWN);
    }
}

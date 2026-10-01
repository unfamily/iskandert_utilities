package net.unfamily.iskautils.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.effect.ModMobEffects;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.util.EntropyExplosionHelper;
import net.unfamily.iskautils.util.EntropicEmpowermentUtil;

/**
 * Creeper variant that detonates via {@link net.unfamily.iskalib.explosion.ExplosionSystem}.
 * Always carries infinite Entropic Empowerment (same as soil-spawned mobs); never spawns from entropic soil.
 */
public class EntropicCreeper extends Creeper {
    public EntropicCreeper(EntityType<? extends Creeper> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Creeper.createAttributes();
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide() && !this.hasEffect(ModMobEffects.ENTROPIC_EMPOWERMENT)) {
            EntropicEmpowermentUtil.apply(this);
        }
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return true;
    }

    @Override
    public void checkDespawn() {
        if (this.level().getDifficulty() == Difficulty.PEACEFUL) {
            this.discard();
            return;
        }
        super.checkDespawn();
    }

    /**
     * Called when the vanilla creeper explosion is cancelled for this entity.
     */
    public void createEntropyExplosion(ServerLevel level) {
        EntropyExplosionHelper.Params params = isPowered()
                ? EntropyExplosionHelper.Params.creeperCharged()
                : EntropyExplosionHelper.Params.creeperNormal();
        EntropyExplosionHelper.create(level, blockPosition(), params);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, damageSource, recentlyHit);
        // Loot table handles drop_of_entropy (3-8); crystal chance is config-driven.
        if (level.getRandom().nextDouble() < Config.entropicCreeperCrystalChance) {
            this.spawnAtLocation(new ItemStack(ModItems.ENTROPY_CRYSTAL.get()));
        }
    }
}

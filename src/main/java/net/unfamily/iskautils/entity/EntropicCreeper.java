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
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.util.EntropyExplosionHelper;

/**
 * Creeper variant that detonates via ExplosionSystem.
 */
public class EntropicCreeper extends Creeper {
    public EntropicCreeper(EntityType<? extends Creeper> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Creeper.createAttributes();
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

    public void createEntropyExplosion(ServerLevel level) {
        EntropyExplosionHelper.Params params = isPowered()
                ? EntropyExplosionHelper.Params.creeperCharged()
                : EntropyExplosionHelper.Params.creeperNormal();
        EntropyExplosionHelper.create(level, blockPosition(), params);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, damageSource, killedByPlayer);
        if (level.getRandom().nextDouble() < Config.entropicCreeperCrystalChance) {
            this.spawnAtLocation(level, new ItemStack(ModItems.ENTROPY_CRYSTAL.get()));
        }
    }
}

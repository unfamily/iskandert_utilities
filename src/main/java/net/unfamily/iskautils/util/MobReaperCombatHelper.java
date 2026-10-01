package net.unfamily.iskautils.util;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.neoforged.neoforge.common.util.FakePlayer;

/**
 * Clears combat memory on mobs after Mob Reaper {@link FakePlayer} hits so they do not stay
 * aggroed (notably creepers targeting the fake profile).
 */
public final class MobReaperCombatHelper {

    private MobReaperCombatHelper() {}

    public static void clearFakePlayerAggro(Mob mob, FakePlayer fakePlayer) {
        if (mob.getTarget() == fakePlayer) {
            mob.setTarget(null);
        }
        LivingEntity lastHurt = mob.getLastHurtByMob();
        if (lastHurt == fakePlayer) {
            mob.setLastHurtByMob(null);
        }
        // 26.x has getLastHurtByPlayer but no null-clear setter; target/lastHurtByMob clear is enough for creepers.
        if (mob instanceof Creeper creeper && creeper.getTarget() == null && creeper.getLastHurtByMob() == null) {
            creeper.setSwellDir(-1);
        }
    }
}

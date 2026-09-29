package net.unfamily.iskautils.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.explosion.ExplosionSystem;

/**
 * Shared Entropy explosion trigger: config/NBT params, with or=0 and vr=0 meaning no block break.
 */
public final class EntropyExplosionHelper {
    public static final String TAG_OR = "or";
    public static final String TAG_VR = "vr";
    public static final String TAG_TICK_INTERVAL = "tickInterval";
    public static final String TAG_DAMAGE = "damage";
    public static final String TAG_BREAK_UNBREAKABLE = "breakUnbreakable";

    private EntropyExplosionHelper() {}

    public record Params(int or, int vr, int tickInterval, float damage, boolean breakUnbreakable) {
        public static Params creeperNormal() {
            return new Params(
                    Config.entropicCreeperOr,
                    Config.entropicCreeperVr,
                    Config.entropicCreeperTickInterval,
                    Config.entropicCreeperDamage,
                    Config.entropicCreeperBreakUnbreakable);
        }

        public static Params creeperCharged() {
            return new Params(
                    Config.entropicCreeperChargedOr,
                    Config.entropicCreeperChargedVr,
                    Config.entropicCreeperChargedTickInterval,
                    Config.entropicCreeperChargedDamage,
                    Config.entropicCreeperChargedBreakUnbreakable);
        }

        public static Params entropyTntDefaults() {
            return new Params(
                    Config.entropyTntOr,
                    Config.entropyTntVr,
                    Config.entropyTntTickInterval,
                    Config.entropyTntDamage,
                    Config.entropyTntBreakUnbreakable);
        }

        public static Params fromTag(CompoundTag tag, Params defaults) {
            return new Params(
                    tag.getIntOr(TAG_OR, defaults.or()),
                    tag.getIntOr(TAG_VR, defaults.vr()),
                    tag.getIntOr(TAG_TICK_INTERVAL, defaults.tickInterval()),
                    tag.getFloatOr(TAG_DAMAGE, defaults.damage()),
                    tag.getBooleanOr(TAG_BREAK_UNBREAKABLE, defaults.breakUnbreakable()));
        }

        public static Params fromItemStack(ItemStack stack, Params defaults) {
            CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
            if (custom == null || custom.isEmpty()) {
                return defaults;
            }
            return fromTag(custom.copyTag(), defaults);
        }

        public void writeToTag(CompoundTag tag) {
            tag.putInt(TAG_OR, or);
            tag.putInt(TAG_VR, vr);
            tag.putInt(TAG_TICK_INTERVAL, tickInterval);
            tag.putFloat(TAG_DAMAGE, damage);
            tag.putBoolean(TAG_BREAK_UNBREAKABLE, breakUnbreakable);
        }
    }

    public static void create(Level level, BlockPos pos, Params params) {
        if (level.isClientSide() || !(level instanceof ServerLevel server)) {
            return;
        }
        if (params.or() == 0 && params.vr() == 0) {
            if (params.damage() > 0.0F) {
                damageEntitiesOnly(server, pos, params.damage());
            }
            return;
        }
        ExplosionSystem.createExplosion(
                server,
                pos,
                params.or(),
                params.vr(),
                params.tickInterval(),
                params.damage(),
                params.breakUnbreakable());
    }

    private static void damageEntitiesOnly(ServerLevel level, BlockPos pos, float damage) {
        AABB box = new AABB(pos).inflate(4.0D);
        DamageSource source = level.damageSources().explosion(null, null);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box)) {
            entity.hurtServer(level, source, damage);
        }
    }
}

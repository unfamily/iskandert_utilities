package net.unfamily.iskautils.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;

import java.util.EnumSet;
import java.util.Set;

/**
 * Mob scan modes for scanner generic target strings ({@code mobs}, {@code mobs:all}, {@code mobs:peaceful}, ...).
 */
public final class ScannerMobCategories {
    public static final String LEGACY_ALL = "mobs";
    public static final String ALL = "mobs:all";
    public static final String PEACEFUL = "mobs:peaceful";
    public static final String HOSTILE = "mobs:hostile";
    public static final String NEUTRAL = "mobs:neutral";
    public static final String INDEFINITE = "mobs:indefinite";

    private static final Set<MobCategory> PEACEFUL_CATEGORIES = EnumSet.of(
            MobCategory.CREATURE,
            MobCategory.AXOLOTLS,
            MobCategory.WATER_CREATURE,
            MobCategory.WATER_AMBIENT,
            MobCategory.UNDERGROUND_WATER_CREATURE,
            MobCategory.AMBIENT
    );

    private ScannerMobCategories() {}

    public static boolean isMobScanTarget(String genericTarget) {
        return genericTarget != null
                && (LEGACY_ALL.equals(genericTarget)
                || ALL.equals(genericTarget)
                || genericTarget.startsWith("mobs:"));
    }

    /**
     * Normalized mode key: all, peaceful, hostile, neutral, indefinite.
     */
    public static String normalizedMode(String genericTarget) {
        if (genericTarget == null) {
            return "all";
        }
        if (LEGACY_ALL.equals(genericTarget) || ALL.equals(genericTarget)) {
            return "all";
        }
        if (!genericTarget.startsWith("mobs:")) {
            return "all";
        }
        String suffix = genericTarget.substring("mobs:".length());
        if (suffix.isEmpty()) {
            return "all";
        }
        return suffix;
    }

    public static String cycleMobTarget(String current) {
        String mode = normalizedMode(current);
        return switch (mode) {
            case "peaceful" -> HOSTILE;
            case "hostile" -> NEUTRAL;
            case "neutral" -> INDEFINITE;
            case "indefinite" -> ALL;
            default -> PEACEFUL;
        };
    }

    public static boolean matches(LivingEntity entity, String genericTarget) {
        String mode = normalizedMode(genericTarget);
        MobCategory cat = entity.getType().getCategory();
        Identifier typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());

        return switch (mode) {
            case "all" -> true;
            case "peaceful" -> PEACEFUL_CATEGORIES.contains(cat);
            case "hostile" -> cat == MobCategory.MONSTER;
            case "neutral" -> cat == MobCategory.MISC;
            case "indefinite" -> !"minecraft".equals(typeId.getNamespace());
            default -> true;
        };
    }
}

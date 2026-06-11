package net.unfamily.iskautils.integration.apotheosis;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;
import net.unfamily.iskautils.Config;
import org.jetbrains.annotations.Nullable;

/**
 * Optional integration with Apotheosis world tiers.
 * <p>
 * Compile-time dependency on Apotheosis is provided via {@code compileOnly}; at runtime this mod
 * remains optional and all entry points guard on {@link #isLoaded()} without linking Apotheosis types.
 */
public final class ApotheosisCompat {

    public static final String MOD_ID = "apotheosis";
    private static final String WORLD_TIER_CLASS = "dev.shadowsoffire.apotheosis.tiers.WorldTier";

    private ApotheosisCompat() {}

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    /**
     * Ring damage multiplier from the player's Apotheosis world tier.
     * Returns {@code 1.0} when Apotheosis is absent.
     */
    public static double getWorldTierMultiplier(Player player) {
        if (!isLoaded() || player == null) {
            return 1.0D;
        }
        return multiplierForOrdinal(getTierOrdinal(player));
    }

    /**
     * Damage per 100 HP gap shown in tooltips and used for combat when Apotheosis is present.
     */
    public static double getEffectiveDamagePer100Hp(Player player) {
        double base = Config.entropicRingDamagePer100Hp;
        if (!isLoaded() || player == null) {
            return base;
        }
        return base * getWorldTierMultiplier(player);
    }

    /**
     * Localized tier label and ring power multiplier for tooltip display, or {@code null} when Apotheosis
     * is absent or {@code player} is {@code null}.
     */
    public static @Nullable WorldTierInfo getWorldTierInfo(Player player) {
        if (!isLoaded() || player == null) {
            return null;
        }
        Component displayName = getTierDisplayName(player);
        if (displayName == null) {
            return null;
        }
        return new WorldTierInfo(displayName, getWorldTierMultiplier(player));
    }

    public record WorldTierInfo(Component displayName, double ringPowerMultiplier) {}

    /** Haven = 0 … Pinnacle = 4. Returns 0 when Apotheosis is absent. */
    public static int getWorldTierIndex(Player player) {
        if (!isLoaded() || player == null) {
            return 0;
        }
        return getTierOrdinal(player);
    }

    private static int getTierOrdinal(Player player) {
        try {
            Class<?> tierClass = Class.forName(WORLD_TIER_CLASS);
            Object tier = tierClass.getMethod("getTier", Player.class).invoke(null, player);
            if (tier instanceof Enum<?> enumTier) {
                return enumTier.ordinal();
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }

    private static @Nullable Component getTierDisplayName(Player player) {
        try {
            Class<?> tierClass = Class.forName(WORLD_TIER_CLASS);
            Object tier = tierClass.getMethod("getTier", Player.class).invoke(null, player);
            if (tier == null) {
                return null;
            }
            Object component = tier.getClass().getMethod("toComponent").invoke(tier);
            return component instanceof Component c ? c : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static double multiplierForOrdinal(int ordinal) {
        return switch (ordinal) {
            case 0 -> Config.entropicRingApotheosisHavenMult;
            case 1 -> Config.entropicRingApotheosisFrontierMult;
            case 2 -> Config.entropicRingApotheosisAscentMult;
            case 3 -> Config.entropicRingApotheosisSummitMult;
            case 4 -> Config.entropicRingApotheosisPinnacleMult;
            default -> 1.0D;
        };
    }
}

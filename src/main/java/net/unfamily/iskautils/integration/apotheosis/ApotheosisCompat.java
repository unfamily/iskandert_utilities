package net.unfamily.iskautils.integration.apotheosis;

import dev.shadowsoffire.apotheosis.tiers.WorldTier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;
import net.unfamily.iskautils.Config;

/**
 * Optional integration with Apotheosis world tiers.
 * <p>
 * Compile-time dependency on Apotheosis is provided via {@code compileOnly}; at runtime this mod
 * remains optional and all entry points guard on {@link #isLoaded()}.
 */
public final class ApotheosisCompat {

    public static final String MOD_ID = "apotheosis";

    private ApotheosisCompat() {}

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    /**
     * Ring damage multiplier from the player's Apotheosis world tier.
     * Returns {@code 1.0} when Apotheosis is absent.
     */
    public static double getWorldTierMultiplier(Player player) {
        if (!isLoaded()) {
            return 1.0D;
        }
        return switch (WorldTier.getTier(player)) {
            case HAVEN -> Config.entropicRingApotheosisHavenMult;
            case FRONTIER -> Config.entropicRingApotheosisFrontierMult;
            case ASCENT -> Config.entropicRingApotheosisAscentMult;
            case SUMMIT -> Config.entropicRingApotheosisSummitMult;
            case PINNACLE -> Config.entropicRingApotheosisPinnacleMult;
        };
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
    public static WorldTierInfo getWorldTierInfo(Player player) {
        if (!isLoaded() || player == null) {
            return null;
        }
        WorldTier tier = WorldTier.getTier(player);
        return new WorldTierInfo(tier.toComponent(), getWorldTierMultiplier(player));
    }

    public record WorldTierInfo(Component displayName, double ringPowerMultiplier) {}
}

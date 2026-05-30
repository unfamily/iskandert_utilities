package net.unfamily.iskautils.arcane;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.unfamily.iskautils.Config;

import java.util.ArrayList;
import java.util.List;

public final class ArcaneDictionaryXpRoll {
    private ArcaneDictionaryXpRoll() {}

    public record TraitRoll(int count, int levelPerTrait) {}

    public record RollContext(double quality, int consumedXp, int capXp) {}

    public static int xpCapForLevels(int levels) {
        if (levels <= 0) {
            return 0;
        }
        int total = 0;
        for (int i = 0; i < levels; i++) {
            total += xpNeededForLevel(i);
        }
        return total;
    }

    public static int getTotalExperience(ServerPlayer player) {
        int total = 0;
        for (int i = 0; i < player.experienceLevel; i++) {
            total += xpNeededForLevel(i);
        }
        total += Math.round(player.experienceProgress * xpNeededForLevel(player.experienceLevel));
        return total;
    }

    /** Subtracts only up to {@code capXp} from the player; returns amount actually removed. */
    public static int consumeUpToCap(ServerPlayer player, int capXp) {
        if (capXp <= 0) {
            return 0;
        }
        int before = getTotalExperience(player);
        int toConsume = Math.min(before, capXp);
        if (toConsume > 0) {
            player.giveExperiencePoints(-toConsume);
        }
        return before - getTotalExperience(player);
    }

    public static double qualityFromConsumed(int consumedXp, int capXp) {
        if (capXp <= 0 || consumedXp <= 0) {
            return 0.0D;
        }
        return Mth.clamp((double) consumedXp / capXp, 0.0D, 1.0D);
    }

    /** Roll quality as if the player spent {@code levels} worth of XP on a reroll, without consuming XP. */
    public static double qualityForRollLevels(int levels) {
        int capLevels = Config.arcaneDictionaryMaxRollLevels;
        int capXp = xpCapForLevels(capLevels);
        int consumedXp = xpCapForLevels(Mth.clamp(levels, 1, capLevels));
        return qualityFromConsumed(consumedXp, capXp);
    }

    public static RollContext rollContextFromPlayer(ServerPlayer player) {
        int capXp = xpCapForLevels(Config.arcaneDictionaryMaxRollLevels);
        int before = getTotalExperience(player);
        int consumedXp = consumeUpToCap(player, capXp);
        double quality = before >= capXp ? 1.0D : qualityFromConsumed(consumedXp, capXp);
        return new RollContext(quality, consumedXp, capXp);
    }

    public static TraitRoll resolveTraitRoll(double quality, RandomSource random) {
        int minTraits = Config.arcaneDictionaryMinTraits;
        int maxTraits = Config.arcaneDictionaryMaxTraits;
        int minLevel = Config.arcaneDictionaryMinLevel;
        int maxLevel = Config.arcaneDictionaryMaxLevel;

        if (quality >= 1.0D) {
            return new TraitRoll(maxTraits, maxLevel);
        }

        double budgetPoints = quality * maxTraits * maxLevel;
        if (budgetPoints < minTraits * minLevel) {
            budgetPoints = minTraits * minLevel;
        }

        float countT = (float) Math.pow(Mth.clamp(quality, 0.0D, 1.0D), Config.arcaneDictionaryRollCountExponent);
        int maxTraitCount = Mth.clamp(
                Math.round(Mth.lerp(countT, minTraits, maxTraits)),
                minTraits,
                maxTraits);

        float levelT = (float) Math.pow(Mth.clamp(quality, 0.0D, 1.0D), Config.arcaneDictionaryRollLevelExponent);
        int maxLevelCap = Mth.clamp(
                Math.round(Mth.lerp(levelT, minLevel, maxLevel)),
                minLevel,
                maxLevel);

        List<TraitRoll> valid = new ArrayList<>();
        for (int count = minTraits; count <= maxTraitCount; count++) {
            int level = (int) Math.floor(budgetPoints / count);
            level = Mth.clamp(level, minLevel, Math.min(maxLevel, maxLevelCap));
            if (count * level <= budgetPoints + 0.001D) {
                valid.add(new TraitRoll(count, level));
            }
        }

        if (valid.isEmpty()) {
            int count = minTraits;
            int level = Mth.clamp((int) Math.floor(budgetPoints / count), minLevel, maxLevelCap);
            return new TraitRoll(count, Math.max(minLevel, level));
        }

        return valid.get(random.nextInt(valid.size()));
    }

    /** XP to advance from {@code level} to {@code level + 1}; matches {@link net.minecraft.world.entity.player.Player#getXpNeededForNextLevel()}. */
    static int xpNeededForLevel(int level) {
        if (level >= 30) {
            return 112 + (level - 30) * 9;
        }
        if (level >= 15) {
            return 37 + (level - 15) * 5;
        }
        return 7 + level * 2;
    }
}

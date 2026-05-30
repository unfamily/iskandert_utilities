package net.unfamily.iskautils.arcane;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.unfamily.iskautils.arcane.jei.ArcaneDictionaryJeiContext;

import java.util.List;

public interface ArcaneDictionaryEffect {
    Identifier id();

    /** Fallback entropy charge per level when JSON omits {@code ent_cha}. */
    default int defaultUpkeepPerLevel() {
        return 0;
    }

    default void onPlayerTick(ArcaneDictionaryEffectContext ctx) {}

    default void onVictimDamaged(ArcaneDictionaryEffectContext ctx, LivingIncomingDamageEvent event) {}

    default void onPlayerAttack(ArcaneDictionaryEffectContext ctx, LivingIncomingDamageEvent event) {}

    /** Return adjusted XP amount after this trait is applied. */
    default int adjustExperiencePickup(ArcaneDictionaryEffectContext ctx, int xpAmount) {
        return xpAmount;
    }

    /** Append trait-specific JEI lines (effect mechanics, config-scaled I/V). */
    default void appendJeiDescription(ArcaneDictionaryJeiContext ctx, List<Component> lines) {}
}

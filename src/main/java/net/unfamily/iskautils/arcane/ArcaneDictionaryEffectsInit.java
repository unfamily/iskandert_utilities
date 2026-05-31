package net.unfamily.iskautils.arcane;

import net.unfamily.iskautils.arcane.effects.AgilityEffect;
import net.unfamily.iskautils.arcane.effects.BloodLedgerEffect;
import net.unfamily.iskautils.arcane.effects.EntropyBladeEffect;
import net.unfamily.iskautils.arcane.effects.EntropyFunnelEffect;
import net.unfamily.iskautils.arcane.effects.GraveDebtEffect;
import net.unfamily.iskautils.arcane.effects.IronRootEffect;
import net.unfamily.iskautils.arcane.effects.LastStandEffect;
import net.unfamily.iskautils.arcane.effects.LifeSiphonEffect;
import net.unfamily.iskautils.arcane.effects.QuickHandsEffect;
import net.unfamily.iskautils.arcane.effects.TierResonanceEffect;
import net.unfamily.iskautils.arcane.effects.EntropyOverflowEffect;
import net.unfamily.iskautils.arcane.effects.EntropyShellEffect;
import net.unfamily.iskautils.arcane.effects.ExecutionLineEffect;
import net.unfamily.iskautils.arcane.effects.GlassSkinEffect;
import net.unfamily.iskautils.arcane.effects.LuckyEffect;
import net.unfamily.iskautils.arcane.effects.MartyrScriptEffect;
import net.unfamily.iskautils.arcane.effects.PhaseMismatchEffect;
import net.unfamily.iskautils.arcane.effects.RecallOfKnowledgeEffect;
import net.unfamily.iskautils.arcane.effects.ShiftingPowerEffect;
import net.unfamily.iskautils.arcane.effects.StoneSkinEffect;
import net.unfamily.iskautils.arcane.effects.UnluckyEffect;
import net.unfamily.iskautils.arcane.effects.VoidThornsEffect;

public final class ArcaneDictionaryEffectsInit {
    private ArcaneDictionaryEffectsInit() {}

    public static void registerBuiltins() {
        register(new GlassSkinEffect(ArcaneDictionaryTraitIds.GLASS_SKIN));
        register(new LuckyEffect(ArcaneDictionaryTraitIds.LUCKY));
        register(new UnluckyEffect(ArcaneDictionaryTraitIds.UNLUCKY));
        register(new StoneSkinEffect());
        register(new EntropyShellEffect());
        register(new AgilityEffect());
        register(new BloodLedgerEffect());
        register(new VoidThornsEffect());
        register(new ExecutionLineEffect());
        register(new EntropyBladeEffect());
        register(new MartyrScriptEffect());
        register(new RecallOfKnowledgeEffect());
        register(new PhaseMismatchEffect());
        register(new EntropyOverflowEffect());
        register(new ShiftingPowerEffect());
        register(new LifeSiphonEffect());
        register(new IronRootEffect());
        register(new QuickHandsEffect());
        register(new EntropyFunnelEffect());
        register(new LastStandEffect());
        register(new GraveDebtEffect());
        register(new TierResonanceEffect());
    }

    private static void register(ArcaneDictionaryEffect effect) {
        ArcaneDictionaryEffectRegistry.register(effect);
    }
}

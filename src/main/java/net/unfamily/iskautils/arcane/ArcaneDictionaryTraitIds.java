package net.unfamily.iskautils.arcane;

import net.minecraft.resources.Identifier;
import net.unfamily.iskautils.IskaUtils;

public final class ArcaneDictionaryTraitIds {
    public static final Identifier GLASS_SKIN = id("glass_skin");
    public static final Identifier LUCKY = id("lucky");
    public static final Identifier UNLUCKY = id("unlucky");
    public static final Identifier STONE_SKIN = id("stone_skin");
    public static final Identifier ENTROPY_SHELL = id("entropy_shell");
    public static final Identifier PHASE_MISMATCH = id("phase_mismatch");
    public static final Identifier BLOOD_LEDGER = id("blood_ledger");
    public static final Identifier MARTYR_SCRIPT = id("martyr_script");
    public static final Identifier EXECUTION_LINE = id("execution_line");
    public static final Identifier VOID_THORNS = id("void_thorns");
    public static final Identifier ENTROPY_BLADE = id("entropy_blade");
    public static final Identifier AGILITY = id("agility");
    public static final Identifier RECALL_OF_KNOWLEDGE = id("recall_of_knowledge");
    public static final Identifier SHIFTING_POWER = id("shifting_power");
    public static final Identifier ENTROPY_OVERFLOW = id("entropy_overflow");
    public static final Identifier LIFE_SIPHON = id("life_siphon");
    public static final Identifier IRON_ROOT = id("iron_root");
    public static final Identifier QUICK_HANDS = id("quick_hands");
    public static final Identifier ENTROPY_FUNNEL = id("entropy_funnel");
    public static final Identifier LAST_STAND = id("last_stand");
    public static final Identifier GRAVE_DEBT = id("grave_debt");
    public static final Identifier TIER_RESONANCE = id("tier_resonance");

    private ArcaneDictionaryTraitIds() {}

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, path);
    }
}

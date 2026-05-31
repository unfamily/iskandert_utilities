package net.unfamily.iskautils.arcane;

import net.minecraft.resources.ResourceLocation;
import net.unfamily.iskautils.IskaUtils;

public final class ArcaneDictionaryTraitIds {
    public static final ResourceLocation GLASS_SKIN = id("glass_skin");
    public static final ResourceLocation LUCKY = id("lucky");
    public static final ResourceLocation UNLUCKY = id("unlucky");
    public static final ResourceLocation STONE_SKIN = id("stone_skin");
    public static final ResourceLocation ENTROPY_SHELL = id("entropy_shell");
    public static final ResourceLocation PHASE_MISMATCH = id("phase_mismatch");
    public static final ResourceLocation BLOOD_LEDGER = id("blood_ledger");
    public static final ResourceLocation MARTYR_SCRIPT = id("martyr_script");
    public static final ResourceLocation EXECUTION_LINE = id("execution_line");
    public static final ResourceLocation VOID_THORNS = id("void_thorns");
    public static final ResourceLocation ENTROPY_BLADE = id("entropy_blade");
    public static final ResourceLocation AGILITY = id("agility");
    public static final ResourceLocation RECALL_OF_KNOWLEDGE = id("recall_of_knowledge");
    public static final ResourceLocation SHIFTING_POWER = id("shifting_power");
    public static final ResourceLocation ENTROPY_OVERFLOW = id("entropy_overflow");
    public static final ResourceLocation LIFE_SIPHON = id("life_siphon");
    public static final ResourceLocation IRON_ROOT = id("iron_root");
    public static final ResourceLocation QUICK_HANDS = id("quick_hands");
    public static final ResourceLocation ENTROPY_FUNNEL = id("entropy_funnel");
    public static final ResourceLocation LAST_STAND = id("last_stand");
    public static final ResourceLocation GRAVE_DEBT = id("grave_debt");
    public static final ResourceLocation TIER_RESONANCE = id("tier_resonance");

    private ArcaneDictionaryTraitIds() {}

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, path);
    }
}

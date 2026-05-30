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

    private ArcaneDictionaryTraitIds() {}

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, path);
    }
}

package net.unfamily.iskautils.data.load.ancienttablet;

import net.unfamily.iskautils.util.ModLogger;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.data.load.RecipeManagerRecipes;
import net.unfamily.iskautils.crafting.AncientTabRecipe;
import net.unfamily.iskautils.crafting.ModAncientTabRecipes;
import org.jetbrains.annotations.Nullable;

public final class AncientTabletRecipeLoader {
    private static final ModLogger LOGGER = ModLogger.of(AncientTabletRecipeLoader.class);

    private static volatile List<AncientTabletRecipeEntry> ENTRIES = List.of();

    private AncientTabletRecipeLoader() {}

    public static void loadFromRecipeManager(RecipeManager recipeManager) {
        List<AncientTabletRecipeEntry> out = new ArrayList<>();
        var type = ModAncientTabRecipes.ANCIENT_TAB_TYPE.get();
        for (RecipeHolder<AncientTabRecipe> holder : RecipeManagerRecipes.holdersOfType(recipeManager, type)) {
            for (AncientTabletRecipeEntry entry : holder.value().compiledEntries()) {
                AncientTabletRecipeEntry withId = withSourceId(entry, holder.id().identifier());
                if (withId.hasIfVariants() || isAncientTabOutputEnabled(withId.produce())) {
                    out.add(withId);
                } else {
                    LOGGER.debug("Ancient Tablet recipe {} skipped (disabled by config)", holder.id());
                }
            }
        }
        ENTRIES = List.copyOf(out);
        LOGGER.info("Loaded {} Ancient Tablet recipe entries from RecipeManager", ENTRIES.size());
    }

    /** @deprecated Use {@link #loadFromRecipeManager(RecipeManager)}. */
    @Deprecated
    public static void loadAll(ResourceManager rm) {
        LOGGER.warn("AncientTabletRecipeLoader.loadAll(ResourceManager) ignored; RecipeManager load required");
    }

    /** @deprecated Use {@link #loadFromRecipeManager(RecipeManager)}. */
    @Deprecated
    public static void loadAllMerged(java.util.Map<Identifier, com.google.gson.JsonElement> merged) {
        LOGGER.warn("AncientTabletRecipeLoader.loadAllMerged ignored; RecipeManager load required");
        ENTRIES = List.of();
    }

    public static List<AncientTabletRecipeEntry> getEntries() {
        return ENTRIES;
    }

    public static List<ItemStack> exampleInputsForJei(AncientTabletRecipeEntry entry) {
        return AncientTabletRecipeMatcher.expandToExampleStacks(entry.require());
    }

    private static AncientTabletRecipeEntry withSourceId(AncientTabletRecipeEntry entry, Identifier recipeId) {
        return new AncientTabletRecipeEntry(
                recipeId,
                entry.mustOrdered(),
                entry.destroyIfWrong(),
                entry.fuelCost(),
                entry.gateHost(),
                entry.require(),
                entry.produce(),
                entry.ifVariants());
    }

    private static boolean isAncientTabOutputEnabled(List<AncientTabletRequirement> produce) {
        Identifier outputId = primaryOutputId(produce);
        if (outputId == null) {
            return true;
        }
        if ("iska_utils".equals(outputId.getNamespace())) {
            return switch (outputId.getPath()) {
                case "entropy_crystal" -> Config.balanceAncientTabEntropyCrystal;
                case "unstable_entropy_catalyst" -> Config.balanceAncientTabUnstableCatalyst;
                default -> true;
            };
        }
        if ("minecraft".equals(outputId.getNamespace()) && "spawner".equals(outputId.getPath())) {
            return Config.balanceAncientTabSpawner;
        }
        return true;
    }

    private static @Nullable Identifier primaryOutputId(List<AncientTabletRequirement> produce) {
        for (AncientTabletRequirement requirement : produce) {
            if (requirement instanceof AncientTabletRequirement.ItemRequirement itemRequirement) {
                return BuiltInRegistries.ITEM.getKey(itemRequirement.item());
            }
        }
        return null;
    }
}

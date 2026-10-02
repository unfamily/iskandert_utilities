package net.unfamily.iskautils.integration.jei;

import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.unfamily.iskautils.data.load.CraftingEntryPools;
import net.unfamily.iskautils.data.load.ancienttablet.AncientTabletRecipeEntry;
import net.unfamily.iskautils.data.load.ancienttablet.AncientTabletRecipeLoader;
import net.unfamily.iskautils.data.load.ancienttablet.AncientTabletRecipeMatcher;
import net.unfamily.iskautils.data.load.ancienttablet.AncientTabletRequirement;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class AncientTabletJeiRecipes {

    private static volatile List<AncientTabletJeiRecipe> CACHE = List.of();

    private AncientTabletJeiRecipes() {}

    public static void reloadForClient(Minecraft mc) {
        ensureLoaded(mc);
        CACHE = buildAll(mc);
    }

    public static List<AncientTabletJeiRecipe> buildAll() {
        return buildAll(Minecraft.getInstance());
    }

    public static List<AncientTabletJeiRecipe> buildAll(Minecraft mc) {
        ensureLoaded(mc);
        ServerPlayer player = CraftingEntryPools.resolveJeiPlayer();
        List<AncientTabletJeiRecipe> out = new ArrayList<>();
        for (AncientTabletRecipeEntry entry : AncientTabletRecipeLoader.getEntries()) {
            if (entry.hasIfVariants()) {
                for (net.unfamily.iskautils.data.load.ancienttablet.AncientTabIfVariant variant : entry.ifVariants()) {
                    if (player != null) {
                        if (!variant.matches(player, entry.gateHost())) {
                            continue;
                        }
                    } else if (entry.hasGate()) {
                        continue;
                    }
                    out.add(buildRecipe(entry, variant.require(), variant.produce()));
                }
            } else {
                Optional<AncientTabletRecipeEntry.ResolvedCraft> resolved = entry.resolveForPlayer(player);
                if (resolved.isPresent()) {
                    out.add(buildRecipe(
                            entry, resolved.get().require(), resolved.get().produce()));
                } else if (player == null && !entry.hasGate()) {
                    out.add(buildRecipe(entry, entry.require(), entry.produce()));
                }
            }
        }
        return List.copyOf(out);
    }

    private static AncientTabletJeiRecipe buildRecipe(
            AncientTabletRecipeEntry entry,
            List<AncientTabletRequirement> require,
            List<AncientTabletRequirement> produce) {
        return new AncientTabletJeiRecipe(
                entry.sourceId(),
                groupedStacks(require),
                groupedStacks(produce),
                entry.mustOrdered(),
                entry.destroyIfWrong(),
                entry.fuelCost());
    }

    public static List<AncientTabletJeiRecipe> cached() {
        return CACHE;
    }

    private static void ensureLoaded(Minecraft mc) {
        if (!AncientTabletRecipeLoader.getEntries().isEmpty()) {
            return;
        }
        RecipeManager recipes = null;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            recipes = server.getRecipeManager();
        } else if (mc != null && mc.level != null) {
            recipes = mc.level.getRecipeManager();
        }
        if (recipes != null) {
            AncientTabletRecipeLoader.loadFromRecipeManager(recipes);
        }
    }

    private static List<ItemStack> groupedStacks(List<AncientTabletRequirement> flat) {
        List<ItemStack> stacks = new ArrayList<>();
        for (AncientTabletRecipeMatcher.GroupedRequirement g : AncientTabletRecipeMatcher.groupConsecutive(flat)) {
            ItemStack stack = exampleStack(g.requirement());
            if (!stack.isEmpty()) {
                stack.setCount(Math.min(64, g.count()));
                stacks.add(stack);
            }
        }
        return stacks;
    }

    private static ItemStack exampleStack(AncientTabletRequirement req) {
        return switch (req) {
            case AncientTabletRequirement.ItemRequirement ir -> new ItemStack(ir.item());
            case AncientTabletRequirement.TagRequirement tr ->
                    AncientTabletRecipeMatcher.exampleStackFromTag(tr);
        };
    }
}

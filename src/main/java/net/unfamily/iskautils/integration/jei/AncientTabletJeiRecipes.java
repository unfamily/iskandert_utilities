package net.unfamily.iskautils.integration.jei;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.unfamily.iskautils.data.load.IskaUtilsLoadJson;
import net.unfamily.iskautils.data.load.IskaUtilsLoadPaths;
import net.unfamily.iskautils.data.load.ancienttablet.AncientTabletRecipeEntry;
import net.unfamily.iskautils.data.load.ancienttablet.AncientTabletRecipeLoader;
import net.unfamily.iskautils.data.load.ancienttablet.AncientTabletRecipeMatcher;
import net.unfamily.iskautils.data.load.ancienttablet.AncientTabletRequirement;

import java.util.ArrayList;
import java.util.List;

public final class AncientTabletJeiRecipes {

    private static volatile List<AncientTabletJeiRecipe> CACHE = List.of();

    private AncientTabletJeiRecipes() {}

    public static void reloadForClient(Minecraft mc) {
        ensureLoaded(mc);
        CACHE = buildAll();
    }

    public static List<AncientTabletJeiRecipe> buildAll() {
        var mc = Minecraft.getInstance();
        if (mc != null) {
            ensureLoaded(mc);
        }
        List<AncientTabletJeiRecipe> out = new ArrayList<>();
        for (AncientTabletRecipeEntry entry : AncientTabletRecipeLoader.getEntries()) {
            out.add(new AncientTabletJeiRecipe(
                    groupedStacks(entry.require()),
                    groupedStacks(entry.produce()),
                    entry.mustOrdered(),
                    entry.destroyIfWrong()));
        }
        return List.copyOf(out);
    }

    public static List<AncientTabletJeiRecipe> cached() {
        return CACHE;
    }

    private static void ensureLoaded(Minecraft mc) {
        if (!AncientTabletRecipeLoader.getEntries().isEmpty()) {
            return;
        }
        MinecraftServer server = mc.getSingleplayerServer();
        if (server != null) {
            AncientTabletRecipeLoader.loadAll(server.getResourceManager());
            return;
        }
        // Client boot (JEI registers before integrated server exists): fall back to mod jar defaults.
        AncientTabletRecipeLoader.loadAllMerged(
                IskaUtilsLoadJson.collectFromModJarOnlyUnderDataDir("recipe", IskaUtilsLoadPaths::isJsonUnderRecipeTree)
        );
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

    private static ItemStack exampleFromTag(AncientTabletRequirement.TagRequirement tr) {
        var it = BuiltInRegistries.ITEM.getTagOrEmpty(tr.tag()).iterator();
        return it.hasNext() ? new ItemStack(it.next().value()) : ItemStack.EMPTY;
    }

    private static ItemStack exampleStack(AncientTabletRequirement req) {
        return switch (req) {
            case AncientTabletRequirement.ItemRequirement ir -> new ItemStack(ir.item());
            case AncientTabletRequirement.TagRequirement tr -> exampleFromTag(tr);
        };
    }
}

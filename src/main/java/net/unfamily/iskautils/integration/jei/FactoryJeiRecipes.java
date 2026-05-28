package net.unfamily.iskautils.integration.jei;

import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.unfamily.iskautils.data.load.FactoryLoader;

import java.util.ArrayList;
import java.util.List;

public final class FactoryJeiRecipes {
    private static final int OUTPUTS_PER_PAGE = 27;

    private FactoryJeiRecipes() {}

    public static void reloadForClient(Minecraft mc) {
        ensureSourcesLoaded();
    }

    private static void ensureSourcesLoaded() {
        if (!FactoryLoader.getSources().isEmpty()) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            FactoryLoader.loadFromRecipeManager(server.getRecipeManager(), server.getResourceManager());
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) {
            FactoryLoader.loadFromMergedRecipeResources(mc.getResourceManager());
        }
    }

    public static List<FactoryJeiRecipe> buildAll() {
        ensureSourcesLoaded();

        List<FactoryJeiRecipe> out = new ArrayList<>();
        for (FactoryLoader.Source src : FactoryLoader.getSources()) {
            List<ItemStack> inputs = FactoryLoader.expandInputForJei(src);
            if (inputs.isEmpty()) continue;

            List<ItemStack> fullOutputs = new ArrayList<>();
            for (FactoryLoader.Output o : src.outputs()) {
                FactoryLoader.resolveOutputStack(o).ifPresent(fullOutputs::add);
            }
            if (fullOutputs.isEmpty()) continue;

            for (int p = 0; p < fullOutputs.size(); p += OUTPUTS_PER_PAGE) {
                int end = Math.min(p + OUTPUTS_PER_PAGE, fullOutputs.size());
                out.add(new FactoryJeiRecipe(src.inputAmount(), inputs, new ArrayList<>(fullOutputs.subList(p, end))));
            }
        }
        return out;
    }
}

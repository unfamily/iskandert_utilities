package net.unfamily.iskautils.integration.jei;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.unfamily.iskautils.data.load.CraftingEntryPools;
import net.unfamily.iskautils.data.load.FactoryLoader;

public final class FactoryJeiRecipes {

    private FactoryJeiRecipes() {}

    public static void reloadForClient(Minecraft mc) {
        if (!FactoryLoader.getSources().isEmpty()) {
            return;
        }
        var server = mc.getSingleplayerServer();
        if (server != null) {
            FactoryLoader.loadFromRecipeManager(server.getRecipeManager(), server.getResourceManager());
            return;
        }
        FactoryLoader.loadFromMergedRecipeResources(mc.getResourceManager());
    }

    public static List<FactoryJeiRecipe> buildAll() {
        ServerPlayer player = CraftingEntryPools.resolveJeiPlayer();
        final int pageSize = FactoryJeiBackgroundDrawable.GRID_COLS * FactoryJeiBackgroundDrawable.GRID_ROWS;
        List<FactoryJeiRecipe> out = new ArrayList<>();
        for (FactoryLoader.Source src : FactoryLoader.getSources()) {
            if (!src.gateHost().checkAllMods()) {
                continue;
            }
            List<FactoryLoader.Output> outputs = src.resolveOutputs(player);
            if (outputs.isEmpty() && src.hasGate() && player == null) {
                continue;
            }
            if (outputs.isEmpty() && !src.hasIfBranches() && src.flatOutputs().isEmpty()) {
                continue;
            }
            if (outputs.isEmpty()) {
                continue;
            }
            List<ItemStack> inputs = FactoryLoader.expandInputForJei(src);
            List<ItemStack> fullOutputs = new ArrayList<>();
            for (FactoryLoader.Output o : outputs) {
                FactoryLoader.resolveOutputStack(o).ifPresent(fullOutputs::add);
            }
            if (fullOutputs.isEmpty()) {
                continue;
            }
            for (int p = 0; p < fullOutputs.size(); p += pageSize) {
                int end = Math.min(p + pageSize, fullOutputs.size());
                out.add(new FactoryJeiRecipe(src.inputAmount(), inputs, new ArrayList<>(fullOutputs.subList(p, end))));
            }
        }
        return out;
    }
}

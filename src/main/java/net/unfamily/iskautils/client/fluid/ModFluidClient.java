package net.unfamily.iskautils.client.fluid;

import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.fluid.FluidTintSources;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.fluid.ModFluids;
import net.unfamily.iskautils.fluid.ModFluids.FluidColors;

public final class ModFluidClient {

    private static final Material STILL = new Material(
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "block/fluid/still"));
    private static final Material FLOW = new Material(
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "block/fluid/flow"));

    private ModFluidClient() {}

    public static void registerFluidModels(RegisterFluidModelsEvent event) {
        event.register(
                new FluidModel.Unbaked(STILL, FLOW, null, FluidTintSources.constant(FluidColors.CONDENSED_KNOWLEDGE)),
                ModFluids.CONDENSED_KNOWLEDGE::getSource,
                ModFluids.CONDENSED_KNOWLEDGE::getFlowing);
    }
}

package net.unfamily.iskautils.client.fluid;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.unfamily.iskautils.fluid.ModFluids;

public final class ModFluidClient {
    private static final int CONDENSED_KNOWLEDGE_TINT = 0xFF55FF88;

    private ModFluidClient() {}

    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return ModFluids.STILL_TEXTURE;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return ModFluids.FLOWING_TEXTURE;
            }

            @Override
            public int getTintColor() {
                return CONDENSED_KNOWLEDGE_TINT;
            }
        }, ModFluids.CONDENSED_KNOWLEDGE_TYPE.get());
    }
}

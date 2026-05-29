package net.unfamily.iskautils.client.entropic;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.unfamily.iskautils.IskaUtils;

@OnlyIn(Dist.CLIENT)
public final class EntropicAnimatedArmorTextures {
    private static final ResourceLocation LAYER_1 = ResourceLocation.fromNamespaceAndPath(
            IskaUtils.MOD_ID, "textures/models/armor/entropic_layer_1.png");
    private static final ResourceLocation LAYER_2 = ResourceLocation.fromNamespaceAndPath(
            IskaUtils.MOD_ID, "textures/models/armor/entropic_layer_2.png");

    private EntropicAnimatedArmorTextures() {}

    public static void register() {
        register(Minecraft.getInstance().getTextureManager());
    }

    public static void register(TextureManager textureManager) {
        registerAnimated(textureManager, LAYER_1);
        registerAnimated(textureManager, LAYER_2);
    }

    private static void registerAnimated(TextureManager textureManager, ResourceLocation location) {
        textureManager.release(location);
        textureManager.register(location, new AnimatedArmorTexture(location));
    }
}

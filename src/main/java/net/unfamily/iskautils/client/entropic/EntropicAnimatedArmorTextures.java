package net.unfamily.iskautils.client.entropic;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.unfamily.iskautils.IskaUtils;

public final class EntropicAnimatedArmorTextures {
    private static final Identifier HUMANOID = Identifier.fromNamespaceAndPath(
            IskaUtils.MOD_ID, "textures/entity/equipment/humanoid/entropic.png");
    private static final Identifier HUMANOID_LEGGINGS = Identifier.fromNamespaceAndPath(
            IskaUtils.MOD_ID, "textures/entity/equipment/humanoid_leggings/entropic.png");
    private static final Identifier HUMANOID_BABY = Identifier.fromNamespaceAndPath(
            IskaUtils.MOD_ID, "textures/entity/equipment/humanoid_baby/entropic.png");

    private EntropicAnimatedArmorTextures() {}

    public static void register() {
        register(Minecraft.getInstance().getTextureManager());
    }

    public static void register(TextureManager textureManager) {
        registerAnimated(textureManager, HUMANOID);
        registerAnimated(textureManager, HUMANOID_LEGGINGS);
        registerAnimated(textureManager, HUMANOID_BABY);
    }

    private static void registerAnimated(TextureManager textureManager, Identifier location) {
        textureManager.release(location);
        textureManager.registerAndLoad(location, new AnimatedArmorTexture(location));
    }
}

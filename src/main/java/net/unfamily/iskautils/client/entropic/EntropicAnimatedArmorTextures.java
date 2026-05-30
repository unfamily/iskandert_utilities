package net.unfamily.iskautils.client.entropic;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.unfamily.iskautils.IskaUtils;

@EventBusSubscriber(value = Dist.CLIENT, modid = IskaUtils.MOD_ID)
public final class EntropicAnimatedArmorTextures {
    private static boolean initialized;
    private static final Identifier HUMANOID = Identifier.fromNamespaceAndPath(
            IskaUtils.MOD_ID, "textures/entity/equipment/humanoid/entropic.png");
    private static final Identifier HUMANOID_LEGGINGS = Identifier.fromNamespaceAndPath(
            IskaUtils.MOD_ID, "textures/entity/equipment/humanoid_leggings/entropic.png");
    private static final Identifier HUMANOID_BABY = Identifier.fromNamespaceAndPath(
            IskaUtils.MOD_ID, "textures/entity/equipment/humanoid_baby/entropic.png");

    private EntropicAnimatedArmorTextures() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!initialized) {
            initialized = true;
            register();
        }
    }

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

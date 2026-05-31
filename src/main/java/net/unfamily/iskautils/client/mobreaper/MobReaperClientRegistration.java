package net.unfamily.iskautils.client.mobreaper;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.entity.ModBlockEntities;

@EventBusSubscriber(modid = IskaUtils.MOD_ID, value = Dist.CLIENT)
public final class MobReaperClientRegistration {

    public static final ResourceLocation ROTOR_MODEL_ID =
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "block/mob_reaper_rotor");

    public static final ModelResourceLocation ROTOR_MODEL =
            ModelResourceLocation.standalone(ROTOR_MODEL_ID);

    private MobReaperClientRegistration() {}

    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(ROTOR_MODEL);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                ModBlockEntities.MOB_REAPER_BE.get(),
                MobReaperRenderer::new);
    }

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        wrapModel(event, ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "block/mob_reaper"));
        wrapModel(event, ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "block/mob_reaper_base"));
    }

    private static void wrapModel(ModelEvent.ModifyBakingResult event, ResourceLocation modelId) {
        ModelResourceLocation key = ModelResourceLocation.standalone(modelId);
        BakedModel base = event.getModels().get(key);
        if (base != null) {
            event.getModels().put(key, new MobReaperBladeHidingBakedModel(base));
        }
    }
}

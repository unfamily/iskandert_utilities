package net.unfamily.iskautils.client.mobreaper;

import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.block.entity.ModBlockEntities;

@EventBusSubscriber(modid = IskaUtils.MOD_ID, value = Dist.CLIENT)
public final class MobReaperClientRegistration {

    private static final Identifier ROTOR_MODEL_ID = Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "block/mob_reaper_rotor");

    public static final StandaloneModelKey<BlockStateModel> ROTOR_MODEL_KEY =
            new StandaloneModelKey<>(() -> "mob_reaper_rotor");

    private MobReaperClientRegistration() {}

    @SubscribeEvent
    public static void registerStandaloneModels(ModelEvent.RegisterStandalone event) {
        event.register(ROTOR_MODEL_KEY, SimpleUnbakedStandaloneModel.blockStateModel(ROTOR_MODEL_ID));
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                ModBlockEntities.MOB_REAPER_BE.get(),
                MobReaperBlockEntityRenderer::new);
    }

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        var blockModels = event.getBakingResult().blockStateModels();
        for (BlockState state : blockModels.keySet().toArray(new BlockState[0])) {
            if (state.is(ModBlocks.MOB_REAPER.get())) {
                BlockStateModel base = blockModels.get(state);
                blockModels.put(state, new MobReaperBladeHidingBlockStateModel(base));
            }
        }
    }
}

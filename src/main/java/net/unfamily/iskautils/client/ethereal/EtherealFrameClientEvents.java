package net.unfamily.iskautils.client.ethereal;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.EtherealFrameBlock;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side handler for Ethereal Frame camouflage rendering.
 * Tracks camouflaged frames and injects their mimic geometry via AddSectionGeometryEvent.
 */
@EventBusSubscriber(modid = IskaUtils.MOD_ID, value = Dist.CLIENT)
public final class EtherealFrameClientEvents {

    private static final float Z_FIGHT_SCALE = 0.99995F;
    private static final long RENDER_SEED = 42L;
    private static final ThreadLocal<RandomSource> RANDOM = ThreadLocal.withInitial(RandomSource::create);

    /** Map from absolute BlockPos to camouflage BlockState. Populated by BE sync. */
    private static final Map<BlockPos, BlockState> CAMOUFLAGE_MAP = new ConcurrentHashMap<>();

    // Called from EtherealFrameBlockEntity.notifyClientCamouflageChange() via the registered callback
    public static void onCamouflageUpdated(BlockPos pos, BlockState camouflageState) {
        if (camouflageState != null) {
            CAMOUFLAGE_MAP.put(pos.immutable(), camouflageState);
        } else {
            CAMOUFLAGE_MAP.remove(pos);
        }
        dirtySectionFor(pos);
    }

    public static void onDimensionChanged() {
        CAMOUFLAGE_MAP.clear();
    }

    private static void dirtySectionFor(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.levelRenderer != null) {
            mc.levelRenderer.setSectionDirty(
                    SectionPos.blockToSectionCoord(pos.getX()),
                    SectionPos.blockToSectionCoord(pos.getY()),
                    SectionPos.blockToSectionCoord(pos.getZ()));
        }
    }

    @SubscribeEvent
    public static void onAddSectionGeometry(AddSectionGeometryEvent event) {
        if (CAMOUFLAGE_MAP.isEmpty()) return;

        SectionPos section = SectionPos.of(event.getSectionOrigin());

        // Collect entries that belong to this section
        Map<BlockPos, BlockState> sectionEntries = new ConcurrentHashMap<>();
        CAMOUFLAGE_MAP.forEach((pos, camouflage) -> {
            if (SectionPos.of(pos).equals(section)) {
                sectionEntries.put(pos, camouflage);
            }
        });

        if (sectionEntries.isEmpty()) return;

        event.addRenderer(ctx -> {
            BlockRenderDispatcher brd = Minecraft.getInstance().getBlockRenderer();
            RandomSource random = RANDOM.get();

            for (Map.Entry<BlockPos, BlockState> entry : sectionEntries.entrySet()) {
                BlockPos pos = entry.getKey();
                BlockState camouflageState = entry.getValue();

                // Only render if the block there is still an EtherealFrameBlock and is camouflaged
                BlockState actualState = ctx.getRegion().getBlockState(pos);
                if (!(actualState.getBlock() instanceof EtherealFrameBlock)
                        || !actualState.getValue(EtherealFrameBlock.CAMOUFLAGED)) {
                    continue;
                }

                random.setSeed(RENDER_SEED);
                BakedModel model = brd.getBlockModel(camouflageState);
                ModelData modelData = model.getModelData(ctx.getRegion(), pos, camouflageState, ModelData.EMPTY);

                ctx.getPoseStack().pushPose();
                ctx.getPoseStack().translate(
                        SectionPos.sectionRelative(pos.getX()),
                        SectionPos.sectionRelative(pos.getY()),
                        SectionPos.sectionRelative(pos.getZ()));

                // Scale slightly inward to avoid z-fighting with the frame box
                ctx.getPoseStack().translate(0.5, 0.5, 0.5);
                ctx.getPoseStack().scale(Z_FIGHT_SCALE, Z_FIGHT_SCALE, Z_FIGHT_SCALE);
                ctx.getPoseStack().translate(-0.5, -0.5, -0.5);

                for (RenderType renderType : model.getRenderTypes(camouflageState, random, ModelData.EMPTY)) {
                    brd.renderBatched(camouflageState, pos, ctx.getRegion(),
                            ctx.getPoseStack(),
                            ctx.getOrCreateChunkBuffer(renderType),
                            true, random, modelData, renderType);
                }

                ctx.getPoseStack().popPose();
            }
        });
    }
}

package net.unfamily.iskautils.client.ethereal;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.EtherealFrameBlock;
import net.unfamily.iskautils.block.entity.EtherealFrameBlockEntity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side camouflage rendering for Ethereal Frame (NeoForge 26.1 path).
 * Pattern aligned with Cable Facades: AddSectionGeometryEvent + ModelBlockRenderer.tesselateBlock.
 * Camouflaged frames use RenderShape.INVISIBLE, so this injected geometry is the visible mesh.
 */
@EventBusSubscriber(modid = IskaUtils.MOD_ID, value = Dist.CLIENT)
public final class EtherealFrameClientEvents {

    /** Absolute BlockPos → camouflage BlockState. Filled from BE client sync. */
    private static final Map<BlockPos, BlockState> CAMOUFLAGE_MAP = new ConcurrentHashMap<>();

    private EtherealFrameClientEvents() {}

    /** Called from EtherealFrameBlockEntity when camouflage NBT is applied on the client. */
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

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        CAMOUFLAGE_MAP.clear();
    }

    private static void dirtySectionFor(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.levelRenderer == null) {
            return;
        }
        int sx = SectionPos.blockToSectionCoord(pos.getX());
        int sy = SectionPos.blockToSectionCoord(pos.getY());
        int sz = SectionPos.blockToSectionCoord(pos.getZ());
        mc.levelRenderer.setSectionDirty(sx, sy, sz);
        int lx = pos.getX() & 15;
        int ly = pos.getY() & 15;
        int lz = pos.getZ() & 15;
        if (lx == 0)  mc.levelRenderer.setSectionDirty(sx - 1, sy, sz);
        if (lx == 15) mc.levelRenderer.setSectionDirty(sx + 1, sy, sz);
        if (ly == 0)  mc.levelRenderer.setSectionDirty(sx, sy - 1, sz);
        if (ly == 15) mc.levelRenderer.setSectionDirty(sx, sy + 1, sz);
        if (lz == 0)  mc.levelRenderer.setSectionDirty(sx, sy, sz - 1);
        if (lz == 15) mc.levelRenderer.setSectionDirty(sx, sy, sz + 1);
    }

    @SubscribeEvent
    public static void onAddSectionGeometry(AddSectionGeometryEvent event) {
        if (CAMOUFLAGE_MAP.isEmpty()) {
            return;
        }

        SectionPos section = SectionPos.of(event.getSectionOrigin());
        Map<BlockPos, BlockState> sectionEntries = new ConcurrentHashMap<>();
        CAMOUFLAGE_MAP.forEach((pos, camouflage) -> {
            if (camouflage != null && SectionPos.of(pos).equals(section)) {
                sectionEntries.put(pos, camouflage);
            }
        });
        if (sectionEntries.isEmpty()) {
            return;
        }

        event.addRenderer(ctx -> {
            BlockAndTintGetter level = ctx.getRegion();
            Minecraft mc = Minecraft.getInstance();
            BlockStateModelSet modelSet = mc.getModelManager().getBlockStateModelSet();
            ModelBlockRenderer blockRenderer = ctx.getBlockRenderer();

            for (Map.Entry<BlockPos, BlockState> entry : sectionEntries.entrySet()) {
                BlockPos pos = entry.getKey();
                BlockState actualState = level.getBlockState(pos);
                if (!(actualState.getBlock() instanceof EtherealFrameBlock)
                        || !actualState.getValue(EtherealFrameBlock.CAMOUFLAGED)) {
                    continue;
                }

                BlockState camouflageState = entry.getValue();
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof EtherealFrameBlockEntity frame && frame.getCamouflage() != null) {
                    camouflageState = frame.getCamouflage();
                    CAMOUFLAGE_MAP.put(pos.immutable(), camouflageState);
                }
                if (camouflageState == null) {
                    continue;
                }

                renderCamouflage(ctx, blockRenderer, modelSet, level, pos, camouflageState);
            }
        });
    }

    private static void renderCamouflage(AddSectionGeometryEvent.SectionRenderingContext ctx,
                                         ModelBlockRenderer blockRenderer,
                                         BlockStateModelSet modelSet,
                                         BlockAndTintGetter level,
                                         BlockPos pos,
                                         BlockState camouflageState) {
        BlockStateModel model = modelSet.get(camouflageState);
        float baseX = SectionPos.sectionRelative(pos.getX());
        float baseY = SectionPos.sectionRelative(pos.getY());
        float baseZ = SectionPos.sectionRelative(pos.getZ());

        BlockQuadOutput output = (x, y, z, quad, instance) -> {
            ChunkSectionLayer layer = quad.materialInfo().layer();
            ctx.getOrCreateChunkBuffer(layer).putBlockBakedQuad(x, y, z, quad, instance);
        };

        blockRenderer.tesselateBlock(
                output,
                baseX, baseY, baseZ,
                level, pos, camouflageState, model,
                camouflageState.getSeed(pos)
        );
    }
}

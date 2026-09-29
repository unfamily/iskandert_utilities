package net.minecraft.client.renderer;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.framegraph.FramePass;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.RenderTargetDescriptor;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.SortedSet;
import net.minecraft.SharedConstants;
import net.minecraft.client.Camera;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.PrioritizeChunkUpdates;
import net.minecraft.client.TextureFilteringMethod;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.chunk.TranslucencyPointOfView;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.client.renderer.debug.GameTestBlockHighlightRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.renderer.state.OptionsRenderState;
import net.minecraft.client.renderer.state.WindowRenderState;
import net.minecraft.client.renderer.state.level.BlockBreakingRenderState;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.SimpleGizmoCollector;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.Zone;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockAndLightGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class LevelRenderer implements ResourceManagerReloadListener, AutoCloseable {
    private static final Identifier TRANSPARENCY_POST_CHAIN_ID = Identifier.withDefaultNamespace("transparency");
    private static final Identifier ENTITY_OUTLINE_POST_CHAIN_ID = Identifier.withDefaultNamespace("entity_outline");
    public static final int SECTION_SIZE = 16;
    public static final int HALF_SECTION_SIZE = 8;
    public static final int NEARBY_SECTION_DISTANCE_IN_BLOCKS = 32;
    private static final int MINIMUM_TRANSPARENT_SORT_COUNT = 15;
    private static final float CHUNK_VISIBILITY_THRESHOLD = 0.3F;
    private final Minecraft minecraft;
    private final EntityRenderDispatcher entityRenderDispatcher;
    private final BlockEntityRenderDispatcher blockEntityRenderDispatcher;
    private final RenderBuffers renderBuffers;
    private @Nullable SkyRenderer skyRenderer;
    private final CloudRenderer cloudRenderer = new CloudRenderer();
    private final WorldBorderRenderer worldBorderRenderer = new WorldBorderRenderer();
    private final WeatherEffectRenderer weatherEffectRenderer = new WeatherEffectRenderer();
    public final DebugRenderer debugRenderer = new DebugRenderer();
    public final GameTestBlockHighlightRenderer gameTestBlockHighlightRenderer = new GameTestBlockHighlightRenderer();
    private @Nullable ClientLevel level;
    private final SectionOcclusionGraph sectionOcclusionGraph = new SectionOcclusionGraph();
    private final ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections = new ObjectArrayList<>(10000);
    private final ObjectArrayList<SectionRenderDispatcher.RenderSection> nearbyVisibleSections = new ObjectArrayList<>(50);
    private @Nullable ViewArea viewArea;
    private int ticks;
    private final Int2ObjectMap<BlockDestructionProgress> destroyingBlocks = new Int2ObjectOpenHashMap<>();
    private final Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress = new Long2ObjectOpenHashMap<>();
    private @Nullable RenderTarget entityOutlineTarget;
    private final LevelTargetBundle targets = new LevelTargetBundle();
    private int lastCameraSectionX = Integer.MIN_VALUE;
    private int lastCameraSectionY = Integer.MIN_VALUE;
    private int lastCameraSectionZ = Integer.MIN_VALUE;
    private double prevCamX = Double.MIN_VALUE;
    private double prevCamY = Double.MIN_VALUE;
    private double prevCamZ = Double.MIN_VALUE;
    private double prevCamRotX = Double.MIN_VALUE;
    private double prevCamRotY = Double.MIN_VALUE;
    private boolean lastSmartCull = true;
    private @Nullable SectionRenderDispatcher sectionRenderDispatcher;
    private int lastViewDistance = -1;
    private @Nullable BlockPos lastTranslucentSortBlockPos;
    private int translucencyResortIterationIndex;
    private final OptionsRenderState optionsRenderState;
    final LevelRenderState levelRenderState;
    private final SubmitNodeStorage submitNodeStorage;
    private final FeatureRenderDispatcher featureRenderDispatcher;
    private @Nullable GpuSampler chunkLayerSampler;
    private final SimpleGizmoCollector collectedGizmos = new SimpleGizmoCollector();
    private LevelRenderer.FinalizedGizmos finalizedGizmos = new LevelRenderer.FinalizedGizmos(new DrawableGizmoPrimitives(), new DrawableGizmoPrimitives());

    public LevelRenderer(
        Minecraft minecraft,
        EntityRenderDispatcher entityRenderDispatcher,
        BlockEntityRenderDispatcher blockEntityRenderDispatcher,
        RenderBuffers renderBuffers,
        GameRenderState gameRenderState,
        FeatureRenderDispatcher featureRenderDispatcher
    ) {
        this.minecraft = minecraft;
        this.entityRenderDispatcher = entityRenderDispatcher;
        this.blockEntityRenderDispatcher = blockEntityRenderDispatcher;
        this.renderBuffers = renderBuffers;
        this.submitNodeStorage = featureRenderDispatcher.getSubmitNodeStorage();
        this.optionsRenderState = gameRenderState.optionsRenderState;
        this.levelRenderState = gameRenderState.levelRenderState;
        this.featureRenderDispatcher = featureRenderDispatcher;
    }

    @Override
    public void close() {
        if (this.entityOutlineTarget != null) {
            this.entityOutlineTarget.destroyBuffers();
        }

        if (this.skyRenderer != null) {
            this.skyRenderer.close();
        }

        if (this.chunkLayerSampler != null) {
            this.chunkLayerSampler.close();
        }

        this.cloudRenderer.close();
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        this.initOutline();
        if (this.skyRenderer != null) {
            this.skyRenderer.close();
        }

        this.skyRenderer = new SkyRenderer(this.minecraft.getTextureManager(), this.minecraft.getAtlasManager());
    }

    public void initOutline() {
        if (this.entityOutlineTarget != null) {
            this.entityOutlineTarget.destroyBuffers();
        }

        WindowRenderState windowState = this.minecraft.gameRenderer.getGameRenderState().windowRenderState;
        this.entityOutlineTarget = new TextureTarget("Entity Outline", windowState.width, windowState.height, true);
    }

    private @Nullable PostChain getTransparencyChain() {
        return !Minecraft.useShaderTransparency()
            ? null
            : this.minecraft.getShaderManager().getPostChain(TRANSPARENCY_POST_CHAIN_ID, LevelTargetBundle.SORTING_TARGETS);
    }

    public void doEntityOutline() {
        if (this.shouldShowEntityOutlines()) {
            this.entityOutlineTarget.blitAndBlendToTexture(this.minecraft.getMainRenderTarget().getColorTextureView());
        }
    }

    public boolean shouldShowEntityOutlines() {
        return !this.levelRenderState.cameraRenderState.isPanoramicMode && this.entityOutlineTarget != null && this.minecraft.player != null;
    }

    public void setLevel(@Nullable ClientLevel level) {
        this.lastCameraSectionX = Integer.MIN_VALUE;
        this.lastCameraSectionY = Integer.MIN_VALUE;
        this.lastCameraSectionZ = Integer.MIN_VALUE;
        this.level = level;
        if (level != null) {
            this.allChanged();
        } else {
            this.entityRenderDispatcher.resetCamera();
            if (this.viewArea != null) {
                this.viewArea.releaseAllBuffers();
                this.viewArea = null;
            }

            if (this.sectionRenderDispatcher != null) {
                this.sectionRenderDispatcher.dispose();
            }

            this.sectionRenderDispatcher = null;
            this.sectionOcclusionGraph.waitAndReset(null);
            this.clearVisibleSections();
        }

        this.gameTestBlockHighlightRenderer.clear();
    }

    private void clearVisibleSections() {
        this.visibleSections.clear();
        this.nearbyVisibleSections.clear();
    }

    public void allChanged() {
        if (this.level != null) {
            this.level.clearTintCaches();
            Options options = this.minecraft.options;
            boolean ambientOcclusion = options.ambientOcclusion().get();
            boolean cutoutLeaves = options.cutoutLeaves().get();
            ModelManager modelManager = this.minecraft.getModelManager();
            SectionCompiler sectionCompiler = new SectionCompiler(
                ambientOcclusion,
                cutoutLeaves,
                modelManager.getBlockStateModelSet(),
                modelManager.getFluidStateModelSet(),
                this.minecraft.getBlockColors(),
                this.minecraft.getBlockEntityRenderDispatcher()
            );
            if (this.sectionRenderDispatcher == null) {
                this.sectionRenderDispatcher = new SectionRenderDispatcher(this.level, this, Util.backgroundExecutor(), this.renderBuffers, sectionCompiler);
            } else {
                this.sectionRenderDispatcher.setLevel(this.level, sectionCompiler);
            }

            this.cloudRenderer.markForRebuild();
            LeavesBlock.setCutoutLeaves(cutoutLeaves);
            this.lastViewDistance = options.getEffectiveRenderDistance();
            if (this.viewArea != null) {
                this.viewArea.releaseAllBuffers();
            }

            this.sectionRenderDispatcher.clearCompileQueue();
            this.viewArea = new ViewArea(this.sectionRenderDispatcher, this.level, options.getEffectiveRenderDistance(), this);
            this.sectionOcclusionGraph.waitAndReset(this.viewArea);
            this.clearVisibleSections();
            Camera camera = this.minecraft.gameRenderer.getMainCamera();
            this.viewArea.repositionCamera(SectionPos.of(camera.position()));
        }
    }

    public void resize(int width, int height) {
        this.needsUpdate();
        if (this.entityOutlineTarget != null) {
            this.entityOutlineTarget.resize(width, height);
        }
    }

    public @Nullable String getSectionStatistics() {
        if (this.viewArea == null) {
            return null;
        } else {
            int totalSections = this.viewArea.sections.length;
            int rendered = this.countRenderedSections();
            return String.format(
                Locale.ROOT,
                "C: %d/%d %sD: %d, %s",
                rendered,
                totalSections,
                this.minecraft.smartCull ? "(s) " : "",
                this.lastViewDistance,
                this.sectionRenderDispatcher == null ? "null" : this.sectionRenderDispatcher.getStats()
            );
        }
    }

    public @Nullable SectionRenderDispatcher getSectionRenderDispatcher() {
        return this.sectionRenderDispatcher;
    }

    public double getTotalSections() {
        return this.viewArea == null ? 0.0 : this.viewArea.sections.length;
    }

    public double getLastViewDistance() {
        return this.lastViewDistance;
    }

    public int countRenderedSections() {
        int rendered = 0;

        for (SectionRenderDispatcher.RenderSection section : this.visibleSections) {
            if (section.getSectionMesh().hasRenderableLayers()) {
                rendered++;
            }
        }

        return rendered;
    }

    public void resetSampler() {
        if (this.chunkLayerSampler != null) {
            this.chunkLayerSampler.close();
        }

        this.chunkLayerSampler = null;
    }

    public @Nullable String getEntityStatistics() {
        return this.level == null
            ? null
            : "E: "
                + this.levelRenderState.lastEntityRenderStateCount
                + "/"
                + this.level.getEntityCount()
                + ", SD: "
                + this.level.getServerSimulationDistance();
    }

    private void cullTerrain(Camera camera, Frustum frustum, boolean spectator) {
        Vec3 cameraPos = camera.position();
        if (this.minecraft.options.getEffectiveRenderDistance() != this.lastViewDistance) {
            this.allChanged();
        }

        ProfilerFiller profiler = Profiler.get();
        profiler.push("repositionCamera");
        int cameraSectionX = SectionPos.posToSectionCoord(cameraPos.x());
        int cameraSectionY = SectionPos.posToSectionCoord(cameraPos.y());
        int cameraSectionZ = SectionPos.posToSectionCoord(cameraPos.z());
        if (this.lastCameraSectionX != cameraSectionX || this.lastCameraSectionY != cameraSectionY || this.lastCameraSectionZ != cameraSectionZ) {
            this.lastCameraSectionX = cameraSectionX;
            this.lastCameraSectionY = cameraSectionY;
            this.lastCameraSectionZ = cameraSectionZ;
            this.viewArea.repositionCamera(SectionPos.of(cameraPos));
            this.worldBorderRenderer.invalidate();
        }

        this.sectionRenderDispatcher.setCameraPosition(cameraPos);
        double camX = Math.floor(cameraPos.x / 8.0);
        double camY = Math.floor(cameraPos.y / 8.0);
        double camZ = Math.floor(cameraPos.z / 8.0);
        if (camX != this.prevCamX || camY != this.prevCamY || camZ != this.prevCamZ) {
            this.sectionOcclusionGraph.invalidate();
        }

        this.prevCamX = camX;
        this.prevCamY = camY;
        this.prevCamZ = camZ;
        profiler.pop();
        if (camera.getCapturedFrustum() == null) {
            boolean smartCull = this.minecraft.smartCull;
            if (spectator && this.level.getBlockState(camera.blockPosition()).isSolidRender()) {
                smartCull = false;
            }

            if (smartCull != this.lastSmartCull) {
                this.sectionOcclusionGraph.invalidate();
            }

            this.lastSmartCull = smartCull;
            profiler.push("updateSOG");
            this.sectionOcclusionGraph.update(smartCull, camera, frustum, this.visibleSections, this.level.getChunkSource().getLoadedEmptySections());
            profiler.pop();
            double camRotX = Math.floor(camera.xRot() / 2.0F);
            double camRotY = Math.floor(camera.yRot() / 2.0F);
            if (this.sectionOcclusionGraph.consumeFrustumUpdate() || camRotX != this.prevCamRotX || camRotY != this.prevCamRotY) {
                profiler.push("applyFrustum");
                this.applyFrustum(offsetFrustum(frustum));
                profiler.pop();
                this.prevCamRotX = camRotX;
                this.prevCamRotY = camRotY;
            }
        }
    }

    public static Frustum offsetFrustum(Frustum frustum) {
        return new Frustum(frustum).offsetToFullyIncludeCameraCube(8);
    }

    private void applyFrustum(Frustum frustum) {
        if (!Minecraft.getInstance().isSameThread()) {
            throw new IllegalStateException("applyFrustum called from wrong thread: " + Thread.currentThread().getName());
        } else {
            this.clearVisibleSections();
            this.sectionOcclusionGraph.addSectionsInFrustum(frustum, this.visibleSections, this.nearbyVisibleSections);
        }
    }

    public void addRecentlyCompiledSection(SectionRenderDispatcher.RenderSection section) {
        this.sectionOcclusionGraph.schedulePropagationFrom(section);
    }

    public void update(Camera camera) {
        ProfilerFiller profiler = Profiler.get();
        profiler.push("cullTerrain");
        this.cullTerrain(camera, camera.getCullFrustum(), this.minecraft.player.isSpectator());
        profiler.popPush("compileSections");
        this.compileSections(camera);
        profiler.pop();
    }

    public void renderLevel(
        GraphicsResourceAllocator resourceAllocator,
        DeltaTracker deltaTracker,
        boolean renderOutline,
        CameraRenderState cameraState,
        Matrix4fc modelViewMatrix,
        GpuBufferSlice terrainFog,
        Vector4f fogColor,
        boolean shouldRenderSky,
        ChunkSectionsToRender chunkSectionsToRender
    ) {
        float deltaPartialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
        this.levelRenderState.gameTime = this.level.getGameTime();
        final ProfilerFiller profiler = Profiler.get();
        Frustum cullFrustum = cameraState.cullFrustum;
        profiler.push("setupFrameGraph");
        Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix();
        modelViewStack.mul(modelViewMatrix);
        FrameGraphBuilder frame = new FrameGraphBuilder();
        this.targets.main = frame.importExternal("main", this.minecraft.getMainRenderTarget());
        int screenWidth = this.minecraft.getMainRenderTarget().width;
        int screenHeight = this.minecraft.getMainRenderTarget().height;
        RenderTargetDescriptor screenSizeTargetDescriptor = new RenderTargetDescriptor(screenWidth, screenHeight, true, 0, this.minecraft.getMainRenderTarget().useStencil);
        PostChain transparencyChain = this.getTransparencyChain();
        if (transparencyChain != null) {
            this.targets.translucent = frame.createInternal("translucent", screenSizeTargetDescriptor);
            this.targets.itemEntity = frame.createInternal("item_entity", screenSizeTargetDescriptor);
            this.targets.particles = frame.createInternal("particles", screenSizeTargetDescriptor);
            this.targets.weather = frame.createInternal("weather", screenSizeTargetDescriptor);
            this.targets.clouds = frame.createInternal("clouds", screenSizeTargetDescriptor);
        }

        if (this.entityOutlineTarget != null) {
            this.targets.entityOutline = frame.importExternal("entity_outline", this.entityOutlineTarget);
        }

        var setupEvent = net.neoforged.neoforge.client.ClientHooks.fireFrameGraphSetup(frame, this.targets, screenSizeTargetDescriptor, cameraState, modelViewMatrix, deltaTracker, profiler);
        this.levelRenderState.haveGlowingEntities |= setupEvent.isOutlineProcessingEnabled();

        FramePass clearPass = frame.addPass("clear");
        this.targets.main = clearPass.readsAndWrites(this.targets.main);
        clearPass.executes(
            () -> {
                RenderTarget mainRenderTarget = this.minecraft.getMainRenderTarget();
                RenderSystem.getDevice()
                    .createCommandEncoder()
                    .clearColorAndDepthTextures(
                        mainRenderTarget.getColorTexture(),
                        ARGB.colorFromFloat(0.0F, fogColor.x, fogColor.y, fogColor.z),
                        mainRenderTarget.getDepthTexture(),
                        1.0
                    );
            }
        );
        if (shouldRenderSky) {
            this.addSkyPass(frame, cameraState, terrainFog, modelViewMatrix);
        }

        this.addMainPass(frame, cullFrustum, modelViewMatrix, terrainFog, renderOutline, this.levelRenderState, deltaTracker, profiler, chunkSectionsToRender);
        PostChain entityOutlineChain = this.minecraft.getShaderManager().getPostChain(ENTITY_OUTLINE_POST_CHAIN_ID, LevelTargetBundle.OUTLINE_TARGETS);
        if (this.levelRenderState.haveGlowingEntities && entityOutlineChain != null) {
            entityOutlineChain.addToFrame(frame, screenWidth, screenHeight, this.targets);
        }

        CloudStatus cloudStatus = this.optionsRenderState.cloudStatus;
        if (cloudStatus != CloudStatus.OFF && ARGB.alpha(this.levelRenderState.cloudColor) > 0) {
            this.addCloudsPass(
                frame,
                cloudStatus,
                this.levelRenderState.cameraRenderState.pos,
                this.levelRenderState.gameTime,
                deltaPartialTick,
                this.levelRenderState.cloudColor,
                this.levelRenderState.cloudHeight,
                this.optionsRenderState.cloudRange,
                modelViewMatrix
            );
        }

        this.addWeatherPass(frame, terrainFog, modelViewMatrix);
        if (transparencyChain != null) {
            transparencyChain.addToFrame(frame, screenWidth, screenHeight, this.targets);
        }

        this.addLateDebugPass(frame, this.levelRenderState.cameraRenderState, terrainFog, modelViewMatrix);
        profiler.popPush("executeFrameGraph");
        frame.execute(resourceAllocator, new FrameGraphBuilder.Inspector() {
            {
                Objects.requireNonNull(LevelRenderer.this);
            }

            @Override
            public void beforeExecutePass(String name) {
                profiler.push(name);
            }

            @Override
            public void afterExecutePass(String name) {
                profiler.pop();
            }
        });
        profiler.pop();
        this.targets.clear();
        modelViewStack.popMatrix();
        this.levelRenderState.reset();
    }

    public void extractLevel(DeltaTracker deltaTracker, Camera camera, float deltaPartialTick) {
        ProfilerFiller profiler = Profiler.get();
        profiler.push("level");
        Vec3 cameraPos = camera.position();
        Frustum cullFrustum = camera.getCullFrustum();
        profiler.push("prepareDispatchers");
        this.blockEntityRenderDispatcher.prepare(cameraPos);
        this.entityRenderDispatcher.prepare(camera, this.minecraft.crosshairPickEntity);
        profiler.popPush("prepareChunkDraws");
        Matrix4f modelViewMatrix = new Matrix4f();
        camera.getViewRotationMatrix(modelViewMatrix);
        this.levelRenderState.chunkSectionsToRender = this.prepareChunkRenders(modelViewMatrix);
        profiler.popPush("entities");
        this.extractVisibleEntities(camera, cullFrustum, deltaTracker, this.levelRenderState);
        profiler.popPush("blockEntities");
        this.extractVisibleBlockEntities(camera, deltaPartialTick, this.levelRenderState, cullFrustum);
        profiler.popPush("blockOutline");
        this.extractBlockOutline(camera, this.levelRenderState);
        profiler.popPush("blockBreaking");
        this.extractBlockDestroyAnimation(camera, this.levelRenderState);
        profiler.popPush("weather");
        this.weatherEffectRenderer.extractRenderState(this.level, this.ticks, deltaPartialTick, cameraPos, this.levelRenderState.weatherRenderState);
        profiler.popPush("sky");
        this.skyRenderer.extractRenderState(this.level, deltaPartialTick, camera, this.levelRenderState.skyRenderState);
        profiler.popPush("border");
        this.worldBorderRenderer
            .extract(
                this.level.getWorldBorder(),
                deltaPartialTick,
                cameraPos,
                this.minecraft.options.getEffectiveRenderDistance() * 16,
                this.levelRenderState.worldBorderRenderState
            );
        profiler.popPush("particles");
        this.minecraft.particleEngine.extract(this.levelRenderState.particlesRenderState, new Frustum(cullFrustum).offset(-3.0F), camera, deltaPartialTick);
        profiler.popPush("cloud");
        this.levelRenderState.cloudColor = camera.attributeProbe().getValue(EnvironmentAttributes.CLOUD_COLOR, deltaPartialTick);
        if (ARGB.alpha(this.levelRenderState.cloudColor) > 0) {
            this.levelRenderState.cloudHeight = camera.attributeProbe().getValue(EnvironmentAttributes.CLOUD_HEIGHT, deltaPartialTick);
        }

        profiler.popPush("debug");
        this.debugRenderer.emitGizmos(cullFrustum, cameraPos.x, cameraPos.y, cameraPos.z, deltaTracker.getGameTimeDeltaPartialTick(false));
        this.gameTestBlockHighlightRenderer.emitGizmos();
        profiler.popPush("neoforge_custom");
        this.levelRenderState.customWeatherEffectRenderer = net.neoforged.neoforge.client.CustomEnvironmentEffectsRendererManager.getCustomWeatherEffectRenderer(this.level, camera.position());
        this.levelRenderState.customSkyboxRenderer = net.neoforged.neoforge.client.CustomEnvironmentEffectsRendererManager.getCustomSkyboxRenderer(this.level, camera.position());
        this.levelRenderState.customCloudsRenderer = net.neoforged.neoforge.client.CustomEnvironmentEffectsRendererManager.getCustomCloudsRenderer(this.level, camera.position());
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent(
                this, this.levelRenderState, this.level, camera, cullFrustum, deltaTracker, this.ticks
        ));
        profiler.pop();
        profiler.pop();
    }

    private void addMainPass(
        FrameGraphBuilder frame,
        Frustum frustum,
        Matrix4fc modelViewMatrix,
        GpuBufferSlice terrainFog,
        boolean renderOutline,
        LevelRenderState levelRenderState,
        DeltaTracker deltaTracker,
        ProfilerFiller profiler,
        ChunkSectionsToRender chunkSectionsToRender
    ) {
        FramePass pass = frame.addPass("main");
        this.targets.main = pass.readsAndWrites(this.targets.main);
        if (this.targets.translucent != null) {
            this.targets.translucent = pass.readsAndWrites(this.targets.translucent);
        }

        if (this.targets.itemEntity != null) {
            this.targets.itemEntity = pass.readsAndWrites(this.targets.itemEntity);
        }

        if (this.targets.weather != null) {
            this.targets.weather = pass.readsAndWrites(this.targets.weather);
        }

        if (this.targets.particles != null) {
            this.targets.particles = pass.readsAndWrites(this.targets.particles);
        }

        if (levelRenderState.haveGlowingEntities && this.targets.entityOutline != null) {
            this.targets.entityOutline = pass.readsAndWrites(this.targets.entityOutline);
        }

        ResourceHandle<RenderTarget> mainTarget = this.targets.main;
        ResourceHandle<RenderTarget> translucentTarget = this.targets.translucent;
        ResourceHandle<RenderTarget> itemEntityTarget = this.targets.itemEntity;
        ResourceHandle<RenderTarget> entityOutlineTarget = this.targets.entityOutline;
        ResourceHandle<RenderTarget> particleTarget = this.targets.particles;
        pass.executes(
            () -> {
                RenderSystem.setShaderFog(terrainFog);
                Vec3 cameraPos = levelRenderState.cameraRenderState.pos;
                double camX = cameraPos.x();
                double camY = cameraPos.y();
                double camZ = cameraPos.z();
                profiler.push("solidTerrain");
                if (this.chunkLayerSampler == null) {
                    int maxAnisotropy = this.optionsRenderState.textureFiltering == TextureFilteringMethod.ANISOTROPIC
                        ? this.optionsRenderState.maxAnisotropyValue
                        : 1;
                    this.chunkLayerSampler = RenderSystem.getDevice()
                        .createSampler(
                            AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.LINEAR, FilterMode.LINEAR, maxAnisotropy, OptionalDouble.empty()
                        );
                }

                chunkSectionsToRender.renderGroup(ChunkSectionLayerGroup.OPAQUE, this.chunkLayerSampler);
                profiler.popPush("neoforge_render_after_opaque_blocks");
                net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.client.event.RenderLevelStageEvent.AfterOpaqueBlocks(this, this.levelRenderState, null, modelViewMatrix, this.getRenderableSections()));
                this.minecraft.gameRenderer.getLighting().setupFor(Lighting.Entry.LEVEL);
                if (this.shouldShowEntityOutlines() && entityOutlineTarget != null) {
                    RenderTarget outlineTarget = entityOutlineTarget.get();
                    RenderSystem.getDevice()
                        .createCommandEncoder()
                        .clearColorAndDepthTextures(outlineTarget.getColorTexture(), 0, outlineTarget.getDepthTexture(), 1.0);
                }

                PoseStack poseStack = new PoseStack();
                MultiBufferSource.BufferSource bufferSource = this.renderBuffers.bufferSource();
                MultiBufferSource.BufferSource crumblingBufferSource = this.renderBuffers.crumblingBufferSource();
                profiler.popPush("submitFeatures");
                this.submitEntities(poseStack, levelRenderState, this.submitNodeStorage);
                this.submitBlockEntities(poseStack, levelRenderState, this.submitNodeStorage);
                levelRenderState.particlesRenderState.submit(this.submitNodeStorage, levelRenderState.cameraRenderState);
                this.submitBlockDestroyAnimation(poseStack, this.submitNodeStorage, levelRenderState);
                profiler.popPush("neoforge_submit_custom");
                net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent(levelRenderState, this.submitNodeStorage, poseStack, this.getRenderableSections()));
                profiler.popPush("renderSolidFeatures");
                this.featureRenderDispatcher.renderSolidFeatures();
                bufferSource.endBatch();
                profiler.popPush("neoforge_render_after_opaque_features");
                net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.client.event.RenderLevelStageEvent.AfterOpaqueFeatures(this, levelRenderState, poseStack, modelViewMatrix, this.getRenderableSections()));
                profiler.pop();
                this.checkPoseStack(poseStack);
                if (translucentTarget != null) {
                    translucentTarget.get().copyDepthFrom(mainTarget.get());
                }

                if (itemEntityTarget != null) {
                    itemEntityTarget.get().copyDepthFrom(mainTarget.get());
                }

                if (particleTarget != null) {
                    particleTarget.get().copyDepthFrom(mainTarget.get());
                }

                profiler.push("renderTranslucentFeatures");
                this.featureRenderDispatcher.renderTranslucentFeatures();
                bufferSource.endBatch();
                crumblingBufferSource.endBatch();
                profiler.popPush("neoforge_render_after_translucent_features");
                net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.client.event.RenderLevelStageEvent.AfterTranslucentFeatures(this, levelRenderState, poseStack, modelViewMatrix, this.getRenderableSections()));
                profiler.pop();
                this.renderBuffers.outlineBufferSource().endOutlineBatch();
                if (renderOutline) {
                    this.renderBlockOutline(bufferSource, poseStack, false, levelRenderState);
                    bufferSource.endBatch();
                }

                this.finalizeGizmoCollection();
                this.finalizedGizmos.standardPrimitives().render(poseStack, bufferSource, levelRenderState.cameraRenderState, modelViewMatrix);
                bufferSource.endBatch();
                this.checkPoseStack(poseStack);
                profiler.push("translucentTerrain");
                chunkSectionsToRender.renderGroup(ChunkSectionLayerGroup.TRANSLUCENT, this.chunkLayerSampler);
                profiler.popPush("neoforge_render_after_translucent_blocks");
                net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.client.event.RenderLevelStageEvent.AfterTranslucentBlocks(this, levelRenderState, poseStack, modelViewMatrix, this.getRenderableSections()));
                if (renderOutline) {
                    this.renderBlockOutline(bufferSource, poseStack, true, levelRenderState);
                }

                bufferSource.endBatch();
                profiler.pop();
                this.featureRenderDispatcher.renderTranslucentParticles();
                bufferSource.endBatch();
                this.featureRenderDispatcher.clearSubmitNodes();
                levelRenderState.particlesRenderState.reset();
                profiler.push("neoforge_render_after_translucent_particles");
                net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.client.event.RenderLevelStageEvent.AfterTranslucentParticles(this, levelRenderState, poseStack, modelViewMatrix, this.getRenderableSections()));
                profiler.pop();
            }
        );
    }

    /// @deprecated Neo: use [#addCloudsPass(FrameGraphBuilder, CloudStatus, Vec3, long, float, int, float, int, Matrix4fc)] instead
    @Deprecated
    private void addCloudsPass(
        FrameGraphBuilder frame,
        CloudStatus cloudStatus,
        Vec3 cameraPosition,
        long gameTime,
        float partialTicks,
        int cloudColor,
        float cloudHeight,
        int cloudRange
    ) {
        addCloudsPass(frame, cloudStatus, cameraPosition, gameTime, partialTicks, cloudColor, cloudHeight, cloudRange, RenderSystem.getModelViewMatrix());
    }

    // Neo: Add modelViewMatrix for use in our event
    private void addCloudsPass(
        FrameGraphBuilder frame,
        CloudStatus cloudStatus,
        Vec3 cameraPosition,
        long gameTime,
        float partialTicks,
        int cloudColor,
        float cloudHeight,
        int cloudRange,
        Matrix4fc modelViewMatrix
    ) {
        FramePass pass = frame.addPass("clouds");
        if (this.targets.clouds != null) {
            this.targets.clouds = pass.readsAndWrites(this.targets.clouds);
        } else {
            this.targets.main = pass.readsAndWrites(this.targets.main);
        }

        pass.executes(() -> {
            if (this.levelRenderState.customCloudsRenderer == null || !this.levelRenderState.customCloudsRenderer.renderClouds(this.levelRenderState, cameraPosition, cloudStatus, cloudColor, cloudHeight, cloudRange, modelViewMatrix)) {
                this.cloudRenderer.render(cloudColor, cloudStatus, cloudHeight, cloudRange, cameraPosition, gameTime, partialTicks);
            }
        });
    }

    /**
     * @deprecated Neo: use {@link #addWeatherPass(FrameGraphBuilder, GpuBufferSlice, Matrix4fc)} instead
     */
    @Deprecated
    private void addWeatherPass(FrameGraphBuilder frame, GpuBufferSlice fog) {
        addWeatherPass(frame, fog, RenderSystem.getModelViewMatrix());
    }

    private void addWeatherPass(FrameGraphBuilder frame, GpuBufferSlice fog, Matrix4fc modelViewMatrix) {
        int renderDistance = this.optionsRenderState.renderDistance * 16;
        FramePass pass = frame.addPass("weather");
        if (this.targets.weather != null) {
            this.targets.weather = pass.readsAndWrites(this.targets.weather);
        } else {
            this.targets.main = pass.readsAndWrites(this.targets.main);
        }

        pass.executes(
            () -> {
                RenderSystem.setShaderFog(fog);
                CameraRenderState cameraState = this.levelRenderState.cameraRenderState;
                this.weatherEffectRenderer.render(cameraState.pos, this.levelRenderState.weatherRenderState, this.levelRenderState);
                Profiler.get().push("neoforge_render_after_weather");
                net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.client.event.RenderLevelStageEvent.AfterWeather(this, this.levelRenderState, null, modelViewMatrix, this.getRenderableSections()));
                Profiler.get().pop();
                this.worldBorderRenderer
                    .render(this.levelRenderState.worldBorderRenderState, cameraState.pos, renderDistance, this.levelRenderState.cameraRenderState.depthFar);
            }
        );
    }

    private void addLateDebugPass(FrameGraphBuilder frame, CameraRenderState camera, GpuBufferSlice fog, Matrix4fc modelViewMatrix) {
        FramePass pass = frame.addPass("late_debug");
        this.targets.main = pass.readsAndWrites(this.targets.main);
        if (this.targets.itemEntity != null) {
            this.targets.itemEntity = pass.readsAndWrites(this.targets.itemEntity);
        }

        ResourceHandle<RenderTarget> mainTarget = this.targets.main;
        pass.executes(() -> {
            RenderSystem.setShaderFog(fog);
            PoseStack poseStack = new PoseStack();
            MultiBufferSource.BufferSource bufferSource = this.renderBuffers.bufferSource();
            RenderSystem.outputColorTextureOverride = mainTarget.get().getColorTextureView();
            RenderSystem.outputDepthTextureOverride = mainTarget.get().getDepthTextureView();
            if (!this.finalizedGizmos.alwaysOnTopPrimitives().isEmpty()) {
                RenderTarget mainRenderTarget = Minecraft.getInstance().getMainRenderTarget();
                RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(mainRenderTarget.getDepthTexture(), 1.0);
                this.finalizedGizmos.alwaysOnTopPrimitives().render(poseStack, bufferSource, camera, modelViewMatrix);
                bufferSource.endLastBatch();
            }

            RenderSystem.outputColorTextureOverride = null;
            RenderSystem.outputDepthTextureOverride = null;
            this.checkPoseStack(poseStack);
        });
    }

    private void extractVisibleEntities(Camera camera, Frustum frustum, DeltaTracker deltaTracker, LevelRenderState output) {
        Vec3 cameraPos = camera.position();
        double camX = cameraPos.x();
        double camY = cameraPos.y();
        double camZ = cameraPos.z();
        TickRateManager tickRateManager = this.minecraft.level.tickRateManager();
        boolean shouldShowEntityOutlines = this.shouldShowEntityOutlines();
        Entity.setViewScale(
            Mth.clamp(this.minecraft.options.getEffectiveRenderDistance() / 8.0, 1.0, 2.5) * this.minecraft.options.entityDistanceScaling().get()
        );

        for (Entity entity : this.level.entitiesForRendering()) {
            if (this.entityRenderDispatcher.shouldRender(entity, frustum, camX, camY, camZ) || entity.hasIndirectPassenger(this.minecraft.player)) {
                BlockPos blockPos = entity.blockPosition();
                if ((this.level.isOutsideBuildHeight(blockPos.getY()) || this.isSectionCompiledAndVisible(blockPos))
                    && (
                        entity != camera.entity()
                            || camera.isDetached()
                            || camera.entity() instanceof LivingEntity && ((LivingEntity)camera.entity()).isSleeping()
                    )
                    && (!(entity instanceof LocalPlayer) || camera.entity() == entity || (entity == minecraft.player && !minecraft.player.isSpectator()))) { // Neo: render local player entity when it is not the camera entity
                    if (entity.tickCount == 0) {
                        entity.xOld = entity.getX();
                        entity.yOld = entity.getY();
                        entity.zOld = entity.getZ();
                    }

                    float partialEntity = deltaTracker.getGameTimeDeltaPartialTick(!tickRateManager.isEntityFrozen(entity));
                    EntityRenderState state = this.extractEntity(entity, partialEntity);
                    output.entityRenderStates.add(state);
                    if (state.appearsGlowing() && shouldShowEntityOutlines) {
                        output.haveGlowingEntities = true;
                    }
                    else if (shouldShowEntityOutlines && entity.hasCustomOutlineRendering(this.minecraft.player)) { // FORGE: allow custom outline rendering
                        output.haveGlowingEntities = true;
                    }
                }
            }
        }

        output.lastEntityRenderStateCount = output.entityRenderStates.size();
    }

    private void submitEntities(PoseStack poseStack, LevelRenderState levelRenderState, SubmitNodeCollector output) {
        Vec3 cameraPos = levelRenderState.cameraRenderState.pos;
        double camX = cameraPos.x();
        double camY = cameraPos.y();
        double camZ = cameraPos.z();

        for (EntityRenderState state : levelRenderState.entityRenderStates) {
            if (!levelRenderState.haveGlowingEntities) {
                state.outlineColor = 0;
            }

            this.entityRenderDispatcher.submit(state, levelRenderState.cameraRenderState, state.x - camX, state.y - camY, state.z - camZ, poseStack, output);
        }
    }

    /**
     * @deprecated Neo: use {@link #extractVisibleBlockEntities(Camera, float, LevelRenderState, Frustum)} instead
     */
    @Deprecated
    private void extractVisibleBlockEntities(Camera camera, float deltaPartialTick, LevelRenderState levelRenderState) {
        extractVisibleBlockEntities(camera, deltaPartialTick, levelRenderState, null);
    }
    private void extractVisibleBlockEntities(Camera camera, float deltaPartialTick, LevelRenderState levelRenderState, @Nullable Frustum frustum) {
        Vec3 cameraPos = camera.position();
        double camX = cameraPos.x();
        double camY = cameraPos.y();
        double camZ = cameraPos.z();
        PoseStack poseStack = new PoseStack();
        boolean shouldShowEntityOutlines = this.shouldShowEntityOutlines();

        for (SectionRenderDispatcher.RenderSection section : this.visibleSections) {
            List<BlockEntity> renderableBlockEntities = section.getSectionMesh().getRenderableBlockEntities();
            if (!renderableBlockEntities.isEmpty() && !(section.getVisibility(Util.getMillis()) < 0.3F)) {
                for (BlockEntity blockEntity : renderableBlockEntities) {
                    BlockPos blockPos = blockEntity.getBlockPos();
                    SortedSet<BlockDestructionProgress> progresses = this.destructionProgress.get(blockPos.asLong());
                    ModelFeatureRenderer.CrumblingOverlay breakProgress;
                    if (progresses != null && !progresses.isEmpty()) {
                        poseStack.pushPose();
                        poseStack.translate(blockPos.getX() - camX, blockPos.getY() - camY, blockPos.getZ() - camZ);
                        breakProgress = new ModelFeatureRenderer.CrumblingOverlay(progresses.last().getProgress(), poseStack.last());
                        poseStack.popPose();
                    } else {
                        breakProgress = null;
                    }

                    BlockEntityRenderState state = this.blockEntityRenderDispatcher.tryExtractRenderState(blockEntity, deltaPartialTick, breakProgress, frustum);
                    if (state != null) {
                        levelRenderState.blockEntityRenderStates.add(state);
                        if (shouldShowEntityOutlines && blockEntity.hasCustomOutlineRendering(this.minecraft.player)) { // Neo: allow custom outline rendering
                            levelRenderState.haveGlowingEntities = true;
                        }
                    }
                }
            }
        }

        Iterator<BlockEntity> iterator = this.level.getGloballyRenderedBlockEntities().iterator();

        while (iterator.hasNext()) {
            BlockEntity blockEntity = iterator.next();
            if (blockEntity.isRemoved()) {
                iterator.remove();
            } else {
                BlockEntityRenderState state = this.blockEntityRenderDispatcher.tryExtractRenderState(blockEntity, deltaPartialTick, null, frustum);
                if (state != null) {
                    levelRenderState.blockEntityRenderStates.add(state);
                    if (shouldShowEntityOutlines && blockEntity.hasCustomOutlineRendering(this.minecraft.player)) { // Neo: allow custom outline rendering
                        levelRenderState.haveGlowingEntities = true;
                    }
                }
            }
        }
    }

    private void submitBlockEntities(PoseStack poseStack, LevelRenderState levelRenderState, SubmitNodeStorage submitNodeStorage) {
        Vec3 cameraPos = levelRenderState.cameraRenderState.pos;
        double camX = cameraPos.x();
        double camY = cameraPos.y();
        double camZ = cameraPos.z();

        for (BlockEntityRenderState renderState : levelRenderState.blockEntityRenderStates) {
            BlockPos blockPos = renderState.blockPos;
            poseStack.pushPose();
            poseStack.translate(blockPos.getX() - camX, blockPos.getY() - camY, blockPos.getZ() - camZ);
            this.blockEntityRenderDispatcher.submit(renderState, poseStack, submitNodeStorage, levelRenderState.cameraRenderState);
            poseStack.popPose();
        }
    }

    private void extractBlockDestroyAnimation(Camera camera, LevelRenderState levelRenderState) {
        Vec3 cameraPos = camera.position();
        double camX = cameraPos.x();
        double camY = cameraPos.y();
        double camZ = cameraPos.z();
        levelRenderState.blockBreakingRenderStates.clear();

        for (Entry<SortedSet<BlockDestructionProgress>> entry : this.destructionProgress.long2ObjectEntrySet()) {
            BlockPos pos = BlockPos.of(entry.getLongKey());
            if (!(pos.distToCenterSqr(camX, camY, camZ) > 1024.0)) {
                SortedSet<BlockDestructionProgress> progresses = entry.getValue();
                if (progresses != null && !progresses.isEmpty()) {
                    int progress = progresses.last().getProgress();
                    levelRenderState.blockBreakingRenderStates.add(new BlockBreakingRenderState(pos, this.level.getBlockState(pos), progress));
                }
            }
        }
    }

    private void submitBlockDestroyAnimation(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, LevelRenderState levelRenderState) {
        Vec3 cameraPos = levelRenderState.cameraRenderState.pos;
        double camX = cameraPos.x();
        double camY = cameraPos.y();
        double camZ = cameraPos.z();

        for (BlockBreakingRenderState state : levelRenderState.blockBreakingRenderStates) {
            if (state.blockState().getRenderShape() == RenderShape.MODEL) {
                BlockPos pos = state.blockPos();
                poseStack.pushPose();
                poseStack.translate(pos.getX() - camX, pos.getY() - camY, pos.getZ() - camZ);
                BlockStateModel model = this.minecraft.getModelManager().getBlockStateModelSet().get(state.blockState());
                submitNodeCollector.submitBreakingBlockModel(poseStack, model, state.blockState().getSeed(pos), state.progress());
                poseStack.popPose();
            }
        }
    }

    private void extractBlockOutline(Camera camera, LevelRenderState levelRenderState) {
        levelRenderState.blockOutlineRenderState = null;
        if (this.minecraft.hitResult instanceof BlockHitResult blockHitResult) {
            if (blockHitResult.getType() != HitResult.Type.MISS) {
                BlockPos pos = blockHitResult.getBlockPos();
                BlockState state = this.level.getBlockState(pos);
                if (!state.isAir() && this.level.getWorldBorder().isWithinBounds(pos)) {
                    BlockStateModel blockStateModel = this.minecraft.getModelManager().getBlockStateModelSet().get(state);
                    boolean isBlockTranslucent = blockStateModel.hasMaterialFlag(this.level, pos, state, 1);
                    boolean highContrast = this.minecraft.options.highContrastBlockOutline().get();
                    CollisionContext context = CollisionContext.of(camera.entity());
                    var event = new net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent(this, this.level, pos, state, blockHitResult, context, isBlockTranslucent, highContrast, camera, levelRenderState);
                    if (net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event).isCanceled()) {
                        return;
                    }
                    VoxelShape shape = state.getShape(this.level, pos, context);
                    if (SharedConstants.DEBUG_SHAPES) {
                        VoxelShape collisionShape = state.getCollisionShape(this.level, pos, context);
                        VoxelShape occlusionShape = state.getOcclusionShape();
                        VoxelShape interactionShape = state.getInteractionShape(this.level, pos);
                        levelRenderState.blockOutlineRenderState = new BlockOutlineRenderState(
                            pos, isBlockTranslucent, highContrast, shape, collisionShape, occlusionShape, interactionShape, event.getCustomRenderers()
                        );
                    } else {
                        levelRenderState.blockOutlineRenderState = new BlockOutlineRenderState(pos, isBlockTranslucent, highContrast, shape, event.getCustomRenderers());
                    }
                }
            }
        }
    }

    private void renderBlockOutline(
        MultiBufferSource.BufferSource bufferSource, PoseStack poseStack, boolean onlyTranslucentBlocks, LevelRenderState levelRenderState
    ) {
        BlockOutlineRenderState state = levelRenderState.blockOutlineRenderState;
        if (state != null) {
            boolean cancel = false;
            for (net.neoforged.neoforge.client.CustomBlockOutlineRenderer customRenderer : state.customRenderers()) {
                cancel |= customRenderer.render(state, bufferSource, poseStack, onlyTranslucentBlocks, levelRenderState);
            }
            if (cancel) {
                return;
            }
            if (state.isTranslucent() == onlyTranslucentBlocks) {
                Vec3 cameraPos = levelRenderState.cameraRenderState.pos;
                if (state.highContrast()) {
                    VertexConsumer buffer = bufferSource.getBuffer(RenderTypes.secondaryBlockOutline());
                    this.renderHitOutline(poseStack, buffer, cameraPos.x, cameraPos.y, cameraPos.z, state, -16777216, 7.0F);
                }

                VertexConsumer buffer = bufferSource.getBuffer(RenderTypes.lines());
                int outlineColor = state.highContrast() ? -11010079 : ARGB.black(102);
                this.renderHitOutline(
                    poseStack,
                    buffer,
                    cameraPos.x,
                    cameraPos.y,
                    cameraPos.z,
                    state,
                    outlineColor,
                    this.minecraft.gameRenderer.getGameRenderState().windowRenderState.appropriateLineWidth
                );
                bufferSource.endLastBatch();
            }
        }
    }

    private void checkPoseStack(PoseStack poseStack) {
        if (!poseStack.isEmpty()) {
            throw new IllegalStateException("Pose stack not empty");
        }
    }

    private EntityRenderState extractEntity(Entity entity, float partialTickTime) {
        return this.entityRenderDispatcher.extractEntity(entity, partialTickTime);
    }

    private void scheduleTranslucentSectionResort(Vec3 cameraPos) {
        if (!this.visibleSections.isEmpty()) {
            BlockPos cameraBlockPos = BlockPos.containing(cameraPos);
            boolean blockPosChanged = !cameraBlockPos.equals(this.lastTranslucentSortBlockPos);
            TranslucencyPointOfView pointOfView = new TranslucencyPointOfView();

            for (SectionRenderDispatcher.RenderSection section : this.nearbyVisibleSections) {
                this.scheduleResort(section, pointOfView, cameraPos, blockPosChanged, true);
            }

            this.translucencyResortIterationIndex = this.translucencyResortIterationIndex % this.visibleSections.size();
            int resortsLeft = Math.max(this.visibleSections.size() / 8, 15);

            while (resortsLeft-- > 0) {
                int index = this.translucencyResortIterationIndex++ % this.visibleSections.size();
                this.scheduleResort(this.visibleSections.get(index), pointOfView, cameraPos, blockPosChanged, false);
            }

            this.lastTranslucentSortBlockPos = cameraBlockPos;
        }
    }

    private void scheduleResort(
        SectionRenderDispatcher.RenderSection section, TranslucencyPointOfView pointOfView, Vec3 cameraPos, boolean blockPosChanged, boolean isNearby
    ) {
        pointOfView.set(cameraPos, section.getSectionNode());
        boolean pointOfViewChanged = section.getSectionMesh().isDifferentPointOfView(pointOfView);
        boolean resortBecauseBlockPosChanged = blockPosChanged && (pointOfView.isAxisAligned() || isNearby);
        if ((resortBecauseBlockPosChanged || pointOfViewChanged) && !section.transparencyResortingScheduled() && section.hasTranslucentGeometry()) {
            section.resortTransparency(this.sectionRenderDispatcher);
        }
    }

    public ChunkSectionsToRender prepareChunkRenders(Matrix4fc modelViewMatrix) {
        ObjectListIterator<SectionRenderDispatcher.RenderSection> iterator = this.visibleSections.listIterator(0);
        EnumMap<ChunkSectionLayer, Int2ObjectOpenHashMap<List<RenderPass.Draw<GpuBufferSlice[]>>>> drawGroups = new EnumMap<>(ChunkSectionLayer.class);
        int largestIndexCount = 0;

        for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
            drawGroups.put(layer, new Int2ObjectOpenHashMap<>());
        }

        List<DynamicUniforms.ChunkSectionInfo> sectionInfos = new ArrayList<>();
        GpuTextureView blockAtlas = this.minecraft.getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS).getTextureView();
        int textureAtlasWidth = blockAtlas.getWidth(0);
        int textureAtlasHeight = blockAtlas.getHeight(0);
        if (this.sectionRenderDispatcher != null) {
            this.sectionRenderDispatcher.lock();

            try {
                try (Zone ignored = Profiler.get().zone("Upload Global Buffers")) {
                    this.sectionRenderDispatcher.uploadGlobalGeomBuffersToGPU();
                }

                while (iterator.hasNext()) {
                    SectionRenderDispatcher.RenderSection section = iterator.next();
                    SectionMesh sectionMesh = section.getSectionMesh();
                    BlockPos renderOffset = section.getRenderOrigin();
                    long now = Util.getMillis();
                    int uboIndex = -1;

                    for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
                        SectionMesh.SectionDraw draw = sectionMesh.getSectionDraw(layer);
                        SectionRenderDispatcher.RenderSectionBufferSlice slice = this.sectionRenderDispatcher.getRenderSectionSlice(sectionMesh, layer);
                        if (slice != null && draw != null && (!draw.hasCustomIndexBuffer() || slice.indexBuffer() != null)) {
                            if (uboIndex == -1) {
                                uboIndex = sectionInfos.size();
                                sectionInfos.add(
                                    new DynamicUniforms.ChunkSectionInfo(
                                        new Matrix4f(modelViewMatrix),
                                        renderOffset.getX(),
                                        renderOffset.getY(),
                                        renderOffset.getZ(),
                                        section.getVisibility(now),
                                        textureAtlasWidth,
                                        textureAtlasHeight
                                    )
                                );
                            }

                            int combinedHash = 173;
                            VertexFormat vertexFormat = layer.pipeline().getVertexFormat();
                            GpuBuffer vertexBuffer = slice.vertexBuffer();
                            if (layer != ChunkSectionLayer.TRANSLUCENT) {
                                combinedHash = 31 * combinedHash + vertexBuffer.hashCode();
                            }

                            int firstIndex = 0;
                            GpuBuffer indexBuffer;
                            VertexFormat.IndexType indexType;
                            if (!draw.hasCustomIndexBuffer()) {
                                if (draw.indexCount() > largestIndexCount) {
                                    largestIndexCount = draw.indexCount();
                                }

                                indexBuffer = null;
                                indexType = null;
                            } else {
                                indexBuffer = slice.indexBuffer();
                                indexType = draw.indexType();
                                if (layer != ChunkSectionLayer.TRANSLUCENT) {
                                    combinedHash = 31 * combinedHash + indexBuffer.hashCode();
                                    combinedHash = 31 * combinedHash + indexType.hashCode();
                                }

                                firstIndex = (int)(slice.indexBufferOffset() / indexType.bytes);
                            }

                            int finalUboIndex = uboIndex;
                            int baseVertex = (int)(slice.vertexBufferOffset() / vertexFormat.getVertexSize());
                            List<RenderPass.Draw<GpuBufferSlice[]>> draws = drawGroups.get(layer).computeIfAbsent(combinedHash, var0 -> new ArrayList<>());
                            draws.add(
                                new RenderPass.Draw<>(
                                    0,
                                    vertexBuffer,
                                    indexBuffer,
                                    indexType,
                                    firstIndex,
                                    draw.indexCount(),
                                    baseVertex,
                                    (sectionUbos, uploader) -> uploader.upload("ChunkSection", sectionUbos[finalUboIndex])
                                )
                            );
                        }
                    }
                }
            } finally {
                this.sectionRenderDispatcher.unlock();
            }
        }

        GpuBufferSlice[] chunkSectionInfos = RenderSystem.getDynamicUniforms()
            .writeChunkSections(sectionInfos.toArray(new DynamicUniforms.ChunkSectionInfo[0]));
        return new ChunkSectionsToRender(blockAtlas, drawGroups, largestIndexCount, chunkSectionInfos);
    }

    public void endFrame() {
        this.cloudRenderer.endFrame();
    }

    public void tick(Camera camera) {
        if (this.level.tickRateManager().runsNormally()) {
            this.ticks++;
        }

        this.weatherEffectRenderer
            .tickRainParticles(this.level, camera, this.ticks, this.minecraft.options.particles().get(), this.minecraft.options.weatherRadius().get());
        this.removeBlockBreakingProgress();
    }

    private void removeBlockBreakingProgress() {
        if (this.ticks % 20 == 0) {
            Iterator<BlockDestructionProgress> iterator = this.destroyingBlocks.values().iterator();

            while (iterator.hasNext()) {
                BlockDestructionProgress block = iterator.next();
                int updatedRenderTick = block.getUpdatedRenderTick();
                if (this.ticks - updatedRenderTick > 400) {
                    iterator.remove();
                    this.removeProgress(block);
                }
            }
        }
    }

    private void removeProgress(BlockDestructionProgress block) {
        long pos = block.getPos().asLong();
        Set<BlockDestructionProgress> progresses = this.destructionProgress.get(pos);
        progresses.remove(block);
        if (progresses.isEmpty()) {
            this.destructionProgress.remove(pos);
        }
    }

    /**
     * @deprecated Neo: use {@link #addSkyPass(FrameGraphBuilder, CameraRenderState, GpuBufferSlice, Matrix4fc)} instead
     */
    @Deprecated
    private void addSkyPass(FrameGraphBuilder frame, CameraRenderState cameraState, GpuBufferSlice skyFog) {
        this.addSkyPass(frame, cameraState, skyFog, RenderSystem.getModelViewMatrix());
    }

    private void addSkyPass(FrameGraphBuilder frame, CameraRenderState cameraState, GpuBufferSlice skyFog, Matrix4fc modelViewMatrix) {
        FogType fogType = cameraState.fogType;
        if (fogType != FogType.POWDER_SNOW && fogType != FogType.LAVA && !cameraState.entityRenderState.doesMobEffectBlockSky) {
            SkyRenderState state = this.levelRenderState.skyRenderState;
            if (state.skybox != DimensionType.Skybox.NONE) {
                SkyRenderer skyRenderer = this.skyRenderer;
                if (skyRenderer != null) {
                    FramePass pass = frame.addPass("sky");
                    this.targets.main = pass.readsAndWrites(this.targets.main);
                    pass.executes(
                        () -> {
                            if (this.levelRenderState.customSkyboxRenderer == null || !this.levelRenderState.customSkyboxRenderer.renderSky(levelRenderState, state, modelViewMatrix, () -> RenderSystem.setShaderFog(skyFog))) {
                            RenderSystem.setShaderFog(skyFog);
                            if (state.skybox == DimensionType.Skybox.END) {
                                skyRenderer.renderEndSky();
                                if (state.endFlashIntensity > 1.0E-5F) {
                                    PoseStack poseStack = new PoseStack();
                                    skyRenderer.renderEndFlash(poseStack, state.endFlashIntensity, state.endFlashXAngle, state.endFlashYAngle);
                                }
                            } else {
                                PoseStack poseStack = new PoseStack();
                                skyRenderer.renderSkyDisc(state.skyColor);
                                skyRenderer.renderSunriseAndSunset(poseStack, state.sunAngle, state.sunriseAndSunsetColor);
                                skyRenderer.renderSunMoonAndStars(
                                    poseStack, state.sunAngle, state.moonAngle, state.starAngle, state.moonPhase, state.rainBrightness, state.starBrightness
                                );
                                if (state.shouldRenderDarkDisc) {
                                    skyRenderer.renderDarkDisc();
                                }
                            }
                            } // Neo: End of skip if custom sky is in effect
                            Profiler.get().push("neoforge_render_after_sky");
                            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.client.event.RenderLevelStageEvent.AfterSky(this, this.levelRenderState, null, modelViewMatrix, this.getRenderableSections()));
                            Profiler.get().pop();
                        }
                    );
                }
            }
        }
    }

    private void compileSections(Camera camera) {
        ProfilerFiller profiler = Profiler.get();
        profiler.push("populateSectionsToCompile");
        RenderRegionCache cache = new RenderRegionCache();
        BlockPos cameraPosition = camera.blockPosition();
        List<SectionRenderDispatcher.RenderSection> sectionsToCompile = Lists.newArrayList();
        long fadeDuration = Mth.floor(this.minecraft.options.chunkSectionFadeInTime().get() * 1000.0);

        for (SectionRenderDispatcher.RenderSection section : this.visibleSections) {
            if (section.isDirty() && (section.getSectionMesh() != CompiledSectionMesh.UNCOMPILED || section.hasAllNeighbors())) {
                BlockPos center = SectionPos.of(section.getSectionNode()).center();
                double distSqr = center.distSqr(cameraPosition);
                boolean isNearby = distSqr < 768.0;
                boolean rebuildSync = false;
                if (this.minecraft.options.prioritizeChunkUpdates().get() == PrioritizeChunkUpdates.NEARBY) {
                    rebuildSync = isNearby || section.isDirtyFromPlayer();
                } else if (this.minecraft.options.prioritizeChunkUpdates().get() == PrioritizeChunkUpdates.PLAYER_AFFECTED) {
                    rebuildSync = section.isDirtyFromPlayer();
                }

                if (!isNearby && !section.wasPreviouslyEmpty()) {
                    section.setFadeDuration(fadeDuration);
                } else {
                    section.setFadeDuration(0L);
                }

                section.setWasPreviouslyEmpty(false);
                if (rebuildSync) {
                    profiler.push("compileSectionSynchronously");
                    this.sectionRenderDispatcher.rebuildSectionSync(section, cache);
                    section.setNotDirty();
                    profiler.pop();
                } else {
                    sectionsToCompile.add(section);
                }
            }
        }

        profiler.popPush("scheduleAsyncCompile");

        for (SectionRenderDispatcher.RenderSection renderSection : sectionsToCompile) {
            renderSection.rebuildSectionAsync(cache);
            renderSection.setNotDirty();
        }

        profiler.popPush("scheduleTranslucentResort");
        this.scheduleTranslucentSectionResort(camera.position());
        profiler.pop();
    }

    private void renderHitOutline(
        PoseStack poseStack, VertexConsumer builder, double camX, double camY, double camZ, BlockOutlineRenderState state, int color, float width
    ) {
        BlockPos pos = state.pos();
        if (SharedConstants.DEBUG_SHAPES) {
            ShapeRenderer.renderShape(
                poseStack, builder, state.shape(), pos.getX() - camX, pos.getY() - camY, pos.getZ() - camZ, ARGB.colorFromFloat(1.0F, 1.0F, 1.0F, 1.0F), width
            );
            if (state.collisionShape() != null) {
                ShapeRenderer.renderShape(
                    poseStack,
                    builder,
                    state.collisionShape(),
                    pos.getX() - camX,
                    pos.getY() - camY,
                    pos.getZ() - camZ,
                    ARGB.colorFromFloat(0.4F, 0.0F, 0.0F, 0.0F),
                    width
                );
            }

            if (state.occlusionShape() != null) {
                ShapeRenderer.renderShape(
                    poseStack,
                    builder,
                    state.occlusionShape(),
                    pos.getX() - camX,
                    pos.getY() - camY,
                    pos.getZ() - camZ,
                    ARGB.colorFromFloat(0.4F, 0.0F, 1.0F, 0.0F),
                    width
                );
            }

            if (state.interactionShape() != null) {
                ShapeRenderer.renderShape(
                    poseStack,
                    builder,
                    state.interactionShape(),
                    pos.getX() - camX,
                    pos.getY() - camY,
                    pos.getZ() - camZ,
                    ARGB.colorFromFloat(0.4F, 0.0F, 0.0F, 1.0F),
                    width
                );
            }
        } else {
            ShapeRenderer.renderShape(poseStack, builder, state.shape(), pos.getX() - camX, pos.getY() - camY, pos.getZ() - camZ, color, width);
        }
    }

    public void blockChanged(BlockGetter level, BlockPos pos, BlockState old, BlockState current, @Block.UpdateFlags int updateFlags) {
        this.setBlockDirty(pos, (updateFlags & 8) != 0);
    }

    private void setBlockDirty(BlockPos pos, boolean playerChanged) {
        for (int z = pos.getZ() - 1; z <= pos.getZ() + 1; z++) {
            for (int x = pos.getX() - 1; x <= pos.getX() + 1; x++) {
                for (int y = pos.getY() - 1; y <= pos.getY() + 1; y++) {
                    this.setSectionDirty(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(y), SectionPos.blockToSectionCoord(z), playerChanged);
                }
            }
        }
    }

    public void setBlocksDirty(int x0, int y0, int z0, int x1, int y1, int z1) {
        for (int z = z0 - 1; z <= z1 + 1; z++) {
            for (int x = x0 - 1; x <= x1 + 1; x++) {
                for (int y = y0 - 1; y <= y1 + 1; y++) {
                    this.setSectionDirty(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(y), SectionPos.blockToSectionCoord(z));
                }
            }
        }
    }

    public void setBlockDirty(BlockPos pos, BlockState oldState, BlockState newState) {
        if (this.minecraft.getModelManager().requiresRender(oldState, newState)) {
            this.setBlocksDirty(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ());
        }
    }

    public void setSectionDirtyWithNeighbors(int sectionX, int sectionY, int sectionZ) {
        this.setSectionRangeDirty(sectionX - 1, sectionY - 1, sectionZ - 1, sectionX + 1, sectionY + 1, sectionZ + 1);
    }

    public void setSectionRangeDirty(int minSectionX, int minSectionY, int minSectionZ, int maxSectionX, int maxSectionY, int maxSectionZ) {
        for (int z = minSectionZ; z <= maxSectionZ; z++) {
            for (int x = minSectionX; x <= maxSectionX; x++) {
                for (int y = minSectionY; y <= maxSectionY; y++) {
                    this.setSectionDirty(x, y, z);
                }
            }
        }
    }

    public void setSectionDirty(int sectionX, int sectionY, int sectionZ) {
        this.setSectionDirty(sectionX, sectionY, sectionZ, false);
    }

    private void setSectionDirty(int sectionX, int sectionY, int sectionZ, boolean playerChanged) {
        this.viewArea.setDirty(sectionX, sectionY, sectionZ, playerChanged);
    }

    public void onSectionBecomingNonEmpty(long sectionNode) {
        SectionRenderDispatcher.RenderSection section = this.viewArea.getRenderSection(sectionNode);
        if (section != null) {
            this.sectionOcclusionGraph.schedulePropagationFrom(section);
            section.setWasPreviouslyEmpty(true);
        }
    }

    public void destroyBlockProgress(int id, BlockPos pos, int progress) {
        if (progress >= 0 && progress < 10) {
            BlockDestructionProgress entry = this.destroyingBlocks.get(id);
            if (entry != null) {
                this.removeProgress(entry);
            }

            if (entry == null || entry.getPos().getX() != pos.getX() || entry.getPos().getY() != pos.getY() || entry.getPos().getZ() != pos.getZ()) {
                entry = new BlockDestructionProgress(id, pos);
                this.destroyingBlocks.put(id, entry);
            }

            entry.setProgress(progress);
            entry.updateTick(this.ticks);
            this.destructionProgress.computeIfAbsent(entry.getPos().asLong(), k -> Sets.newTreeSet()).add(entry);
        } else {
            BlockDestructionProgress removed = this.destroyingBlocks.remove(id);
            if (removed != null) {
                this.removeProgress(removed);
            }
        }
    }

    public boolean hasRenderedAllSections() {
        return this.sectionRenderDispatcher.isQueueEmpty();
    }

    public void onChunkReadyToRender(ChunkPos pos) {
        this.sectionOcclusionGraph.onChunkReadyToRender(pos);
    }

    public void needsUpdate() {
        this.sectionOcclusionGraph.invalidate();
        this.cloudRenderer.markForRebuild();
    }

    public static int getLightCoords(BlockAndLightGetter level, BlockPos pos) {
        return getLightCoords(LevelRenderer.BrightnessGetter.DEFAULT, level, level.getBlockState(pos), pos);
    }

    public static int getLightCoords(LevelRenderer.BrightnessGetter brightnessGetter, BlockAndLightGetter level, BlockState state, BlockPos pos) {
        if (state.emissiveRendering(level, pos)) {
            return 15728880;
        } else {
            int packedBrightness = brightnessGetter.packedBrightness(level, pos);
            int block = LightCoordsUtil.block(packedBrightness);
            int blockSelfEmission = state.getLightEmission(level, pos);
            return block < blockSelfEmission ? LightCoordsUtil.withBlock(packedBrightness, blockSelfEmission) : packedBrightness;
        }
    }

    public boolean isSectionCompiledAndVisible(BlockPos blockPos) {
        SectionRenderDispatcher.RenderSection renderSection = this.viewArea.getRenderSectionAt(blockPos);
        return renderSection != null && renderSection.sectionMesh.get() != CompiledSectionMesh.UNCOMPILED
            ? renderSection.getVisibility(Util.getMillis()) >= 0.3F
            : false;
    }

    public @Nullable RenderTarget entityOutlineTarget() {
        return this.targets.entityOutline != null ? this.targets.entityOutline.get() : null;
    }

    public @Nullable RenderTarget getTranslucentTarget() {
        return this.targets.translucent != null ? this.targets.translucent.get() : null;
    }

    public @Nullable RenderTarget getItemEntityTarget() {
        return this.targets.itemEntity != null ? this.targets.itemEntity.get() : null;
    }

    public @Nullable RenderTarget getParticlesTarget() {
        return this.targets.particles != null ? this.targets.particles.get() : null;
    }

    public @Nullable RenderTarget getWeatherTarget() {
        return this.targets.weather != null ? this.targets.weather.get() : null;
    }

    public @Nullable RenderTarget getCloudsTarget() {
        return this.targets.clouds != null ? this.targets.clouds.get() : null;
    }

    @VisibleForDebug
    public ObjectArrayList<SectionRenderDispatcher.RenderSection> getVisibleSections() {
        return this.visibleSections;
    }

    @VisibleForDebug
    public SectionOcclusionGraph getSectionOcclusionGraph() {
        return this.sectionOcclusionGraph;
    }

    public CloudRenderer getCloudRenderer() {
        return this.cloudRenderer;
    }

    public int getTicks() {
        return this.ticks;
    }

    public void iterateVisibleBlockEntities(java.util.function.Consumer<BlockEntity> blockEntityConsumer) {
        for (var chunkInfo : this.visibleSections) {
            chunkInfo.getSectionMesh().getRenderableBlockEntities().forEach(blockEntityConsumer);
        }
        this.level.getGloballyRenderedBlockEntities().forEach(blockEntityConsumer);
    }

    @org.jetbrains.annotations.ApiStatus.Internal
    public Iterable<? extends net.neoforged.neoforge.client.IRenderableSection> getRenderableSections() {
        return this.visibleSections;
    }

    public Gizmos.TemporaryCollection collectPerFrameGizmos() {
        return Gizmos.withCollector(this.collectedGizmos);
    }

    private void finalizeGizmoCollection() {
        DrawableGizmoPrimitives standardPrimitives = new DrawableGizmoPrimitives();
        DrawableGizmoPrimitives alwaysOnTopPrimitives = new DrawableGizmoPrimitives();
        this.collectedGizmos.addTemporaryGizmos(this.minecraft.getPerTickGizmos());
        IntegratedServer server = this.minecraft.getSingleplayerServer();
        if (server != null) {
            this.collectedGizmos.addTemporaryGizmos(server.getPerTickGizmos());
        }

        long currentMillis = Util.getMillis();

        for (SimpleGizmoCollector.GizmoInstance instance : this.collectedGizmos.drainGizmos()) {
            instance.gizmo().emit(instance.isAlwaysOnTop() ? alwaysOnTopPrimitives : standardPrimitives, instance.getAlphaMultiplier(currentMillis));
        }

        this.finalizedGizmos = new LevelRenderer.FinalizedGizmos(standardPrimitives, alwaysOnTopPrimitives);
    }

    @FunctionalInterface
    @OnlyIn(Dist.CLIENT)
    public interface BrightnessGetter {
        LevelRenderer.BrightnessGetter DEFAULT = (level, pos) -> {
            int sky = level.getBrightness(LightLayer.SKY, pos);
            int block = level.getBrightness(LightLayer.BLOCK, pos);
            return LightCoordsUtil.pack(block, sky);
        };

        int packedBrightness(BlockAndLightGetter level, BlockPos pos);
    }

    @OnlyIn(Dist.CLIENT)
    private record FinalizedGizmos(DrawableGizmoPrimitives standardPrimitives, DrawableGizmoPrimitives alwaysOnTopPrimitives) {
    }
}

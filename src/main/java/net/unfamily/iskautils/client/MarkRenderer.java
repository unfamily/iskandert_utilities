package net.unfamily.iskautils.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Renderer for visible blocks through walls
 */
public class MarkRenderer {
    private static final MarkRenderer INSTANCE = new MarkRenderer();
    private final Map<BlockPos, MarkBlockData> highlightedBlocks = new HashMap<>();
    private final Map<BlockPos, MarkBlockData> billboardMarkers = new HashMap<>();
    
    private static final ResourceLocation MARKER_TEXTURE = ResourceLocation.fromNamespaceAndPath("iska_utils", "textures/gui/marker_other.png");
    
    private MarkRenderer() {}
    
    public static MarkRenderer getInstance() {
        return INSTANCE;
    }
    
    /**
     * Add a block to highlight
     * @param pos Block position
     * @param color Color in ARGB format (0xAARRGGBB)
     * @param durationTicks Duration in tick (20 tick = 1 second)
     */
    public void addHighlightedBlock(BlockPos pos, int color, int durationTicks) {
        // Add the block to the map
        highlightedBlocks.put(pos, new MarkBlockData(color, Minecraft.getInstance().level.getGameTime() + durationTicks));
    }
    
    /**
     * Add a billboard marker at the specified position
     * @param pos Block position
     * @param color Color tint in ARGB format (0xAARRGGBB)
     * @param durationTicks Duration in tick (20 tick = 1 second)
     */
    public void addBillboardMarker(BlockPos pos, int color, int durationTicks) {
        // Add the marker to the map
        billboardMarkers.put(pos, new MarkBlockData(color, Minecraft.getInstance().level.getGameTime() + durationTicks));
    }
    
    /**
     * Remove a highlighted block
     */
    public void removeHighlightedBlock(BlockPos pos) {
        highlightedBlocks.remove(pos);
    }
    
    /**
     * Remove a billboard marker
     */
    public void removeBillboardMarker(BlockPos pos) {
        billboardMarkers.remove(pos);
    }
    
    /**
     * Remove all highlighted blocks and markers
     */
    public void clearHighlightedBlocks() {
        highlightedBlocks.clear();
        billboardMarkers.clear();
    }
    
    /**
     * Render all highlighted blocks and markers
     */
    public void render(PoseStack poseStack, float partialTick) {
        if (highlightedBlocks.isEmpty() && billboardMarkers.isEmpty()) {
            return;
        }
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        
        long currentTime = mc.level.getGameTime();
        
        // Remove expired blocks
        Iterator<Map.Entry<BlockPos, MarkBlockData>> iterator = highlightedBlocks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, MarkBlockData> entry = iterator.next();
            if (entry.getValue().expirationTime <= currentTime) {
                iterator.remove();
            }
        }
        
        // Remove expired billboard markers
        Iterator<Map.Entry<BlockPos, MarkBlockData>> markerIterator = billboardMarkers.entrySet().iterator();
        while (markerIterator.hasNext()) {
            Map.Entry<BlockPos, MarkBlockData> entry = markerIterator.next();
            if (entry.getValue().expirationTime <= currentTime) {
                markerIterator.remove();
            }
        }
        
        if (highlightedBlocks.isEmpty() && billboardMarkers.isEmpty()) {
            return;
        }
        
        // Get the camera position
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();
        
        // Render cube highlights
        if (!highlightedBlocks.isEmpty()) {
            renderCubeHighlights(poseStack, mc, cameraPos, currentTime);
        }
        
        // Render billboard markers
        if (!billboardMarkers.isEmpty()) {
            renderBillboardMarkers(poseStack, mc, cameraPos, currentTime);
        }
    }
    
    /**
     * Render cube highlights
     */
    private void renderCubeHighlights(PoseStack poseStack, Minecraft mc, Vec3 cameraPos, long currentTime) {
        // Check if there are valid blocks to render
        boolean hasValidBlocks = false;
        for (Map.Entry<BlockPos, MarkBlockData> entry : highlightedBlocks.entrySet()) {
            if (!mc.level.getBlockState(entry.getKey()).isAir()) {
                hasValidBlocks = true;
                break;
            }
        }
        
        // If there are no valid blocks, exit without rendering
        if (!hasValidBlocks) {
            return;
        }
        
        // Prepare the rendering
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        
        // Start rendering
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = poseStack.last().pose();
        
        // Render each block
        for (Map.Entry<BlockPos, MarkBlockData> entry : highlightedBlocks.entrySet()) {
            BlockPos pos = entry.getKey();
            int color = entry.getValue().color;
            
            // Check if the block is still valid
            if (mc.level.getBlockState(pos).isAir()) {
                continue;
            }
            
            // Draw the cube
            drawCube(bufferBuilder, matrix, pos, cameraPos, color);
        }
        
        // Complete the rendering
        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
        
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }
    
    /**
     * Render billboard markers
     */
    private void renderBillboardMarkers(PoseStack poseStack, Minecraft mc, Vec3 cameraPos, long currentTime) {
        // Prepare the rendering for billboards
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, MARKER_TEXTURE);
        
        // Start rendering
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        
        // Render each billboard marker
        for (Map.Entry<BlockPos, MarkBlockData> entry : billboardMarkers.entrySet()) {
            BlockPos pos = entry.getKey();
            int color = entry.getValue().color;
            
            // Draw the billboard
            drawBillboard(poseStack, bufferBuilder, pos, cameraPos, color, 1.0f);
        }
        
        // Complete the rendering
        com.mojang.blaze3d.vertex.MeshData meshData = bufferBuilder.build();
        if (meshData != null) {
            BufferUploader.drawWithShader(meshData);
        }
        
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }
    
    /**
     * Draw a billboard marker that always faces the camera
     */
    private void drawBillboard(PoseStack poseStack, BufferBuilder bufferBuilder, BlockPos pos, Vec3 cameraPos, int color, float size) {
        // Save the current transformation matrix
        poseStack.pushPose();
        
        // Translate to the marker position (center of the block)
        float x = pos.getX() + 0.5f - (float)cameraPos.x;
        float y = pos.getY() + 0.5f - (float)cameraPos.y;
        float z = pos.getZ() + 0.5f - (float)cameraPos.z;
        poseStack.translate(x, y, z);
        
        // Make the billboard face the camera
        // Get the camera rotation and apply it to make the billboard face the camera
        Quaternionf rotation = new Quaternionf(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
        // Non coniughiamo il quaternione, ma lo applichiamo direttamente
        poseStack.mulPose(rotation);
        
        // Ruotiamo di 180 gradi sull'asse Y per far sì che il billboard guardi verso la camera
        poseStack.mulPose(new Quaternionf().rotateY((float)Math.PI));
        
        // Scale the billboard
        float halfSize = size / 2.0f;
        
        // Extract color components
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        int alpha = (color >> 24) & 0xFF;
        
        // Get the transformation matrix
        Matrix4f matrix = poseStack.last().pose();
        
        // Applica la trasformazione della matrice alle coordinate dei vertici
        // Vertice 1: in basso a sinistra
        float[] v1 = transformVertex(matrix, -halfSize, -halfSize, 0);
        bufferBuilder.addVertex(v1[0], v1[1], v1[2])
                    .setUv(0, 1)
                    .setColor(red, green, blue, alpha);
        
        // Vertice 2: in basso a destra
        float[] v2 = transformVertex(matrix, halfSize, -halfSize, 0);
        bufferBuilder.addVertex(v2[0], v2[1], v2[2])
                    .setUv(1, 1)
                    .setColor(red, green, blue, alpha);
        
        // Vertice 3: in alto a destra
        float[] v3 = transformVertex(matrix, halfSize, halfSize, 0);
        bufferBuilder.addVertex(v3[0], v3[1], v3[2])
                    .setUv(1, 0)
                    .setColor(red, green, blue, alpha);
        
        // Vertice 4: in alto a sinistra
        float[] v4 = transformVertex(matrix, -halfSize, halfSize, 0);
        bufferBuilder.addVertex(v4[0], v4[1], v4[2])
                    .setUv(0, 0)
                    .setColor(red, green, blue, alpha);
        
        // Restore the transformation matrix
        poseStack.popPose();
    }
    
    /**
     * Applica la trasformazione della matrice a un vertice
     */
    private float[] transformVertex(Matrix4f matrix, float x, float y, float z) {
        float[] result = new float[3];
        float w = 1.0f;
        
        result[0] = matrix.m00() * x + matrix.m10() * y + matrix.m20() * z + matrix.m30() * w;
        result[1] = matrix.m01() * x + matrix.m11() * y + matrix.m21() * z + matrix.m31() * w;
        result[2] = matrix.m02() * x + matrix.m12() * y + matrix.m22() * z + matrix.m32() * w;
        
        return result;
    }
    
    /**
     * Draw a cube at the specified position
     */
    private void drawCube(BufferBuilder bufferBuilder, Matrix4f matrix, BlockPos pos, Vec3 cameraPos, int color) {
        float x = pos.getX() - (float)cameraPos.x;
        float y = pos.getY() - (float)cameraPos.y;
        float z = pos.getZ() - (float)cameraPos.z;
        
        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        float alpha = ((color >> 24) & 0xFF) / 255.0F;
        
        // Bottom face
        bufferBuilder.addVertex(x, y, z).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(x + 1, y, z).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(x + 1, y, z + 1).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(x, y, z + 1).setColor(red, green, blue, alpha);
        
        // Top face
        bufferBuilder.addVertex(x, y + 1, z).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(x, y + 1, z + 1).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(x + 1, y + 1, z + 1).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(x + 1, y + 1, z).setColor(red, green, blue, alpha);
        
        // North face
        bufferBuilder.addVertex(x, y, z).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(x, y + 1, z).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(x + 1, y + 1, z).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(x + 1, y, z).setColor(red, green, blue, alpha);
        
        // South face
        bufferBuilder.addVertex(x, y, z + 1).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(x + 1, y, z + 1).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(x + 1, y + 1, z + 1).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(x, y + 1, z + 1).setColor(red, green, blue, alpha);
        
        // West face
        bufferBuilder.addVertex(x, y, z).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(x, y, z + 1).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(x, y + 1, z + 1).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(x, y + 1, z).setColor(red, green, blue, alpha);
        
        // East face
        bufferBuilder.addVertex(x + 1, y, z).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(x + 1, y + 1, z).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(x + 1, y + 1, z + 1).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(x + 1, y, z + 1).setColor(red, green, blue, alpha);
    }
    
    /**
     * Class to store the data of a highlighted block
     */
    private static class MarkBlockData {
        final int color;
        final long expirationTime;
        
        MarkBlockData(int color, long expirationTime) {
            this.color = color;
            this.expirationTime = expirationTime;
        }
    }
} 
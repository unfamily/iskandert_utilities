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
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Renderer for visible blocks through walls
 */
public class MarkRenderer {
    private static final MarkRenderer INSTANCE = new MarkRenderer();
    private final Map<BlockPos, MarkBlockData> highlightedBlocks = new HashMap<>();
    
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
     * Remove a highlighted block
     */
    public void removeHighlightedBlock(BlockPos pos) {
        highlightedBlocks.remove(pos);
    }
    
    /**
     * Remove all highlighted blocks
     */
    public void clearHighlightedBlocks() {
        highlightedBlocks.clear();
    }
    
    /**
     * Render all highlighted blocks
     */
    public void render(PoseStack poseStack, float partialTick) {
        if (highlightedBlocks.isEmpty()) {
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
        
        if (highlightedBlocks.isEmpty()) {
            return;
        }
        
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
        
        // Get the camera position
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();
        
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
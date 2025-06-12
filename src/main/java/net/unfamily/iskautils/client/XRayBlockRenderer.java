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
 * Renderer per blocchi visibili attraverso i muri
 */
public class XRayBlockRenderer {
    private static final XRayBlockRenderer INSTANCE = new XRayBlockRenderer();
    private final Map<BlockPos, XRayBlockData> highlightedBlocks = new HashMap<>();
    
    // Colori predefiniti per i diversi livelli di difficoltà
    private static final int COLOR_EASY = 0x3300FF00; // Verde semi-trasparente
    private static final int COLOR_MEDIUM = 0x33FFFF00; // Giallo semi-trasparente
    private static final int COLOR_HARD = 0x33FF0000; // Rosso semi-trasparente
    
    private XRayBlockRenderer() {}
    
    public static XRayBlockRenderer getInstance() {
        return INSTANCE;
    }
    
    /**
     * Aggiunge un blocco da evidenziare
     * @param pos Posizione del blocco
     * @param difficulty Difficoltà da 1 a 10
     * @param durationTicks Durata in tick (20 tick = 1 secondo)
     */
    public void addHighlightedBlock(BlockPos pos, int difficulty, int durationTicks) {
        // Limita la difficoltà tra 1 e 10
        difficulty = Math.max(1, Math.min(10, difficulty));
        
        // Calcola il colore in base alla difficoltà
        int color;
        if (difficulty <= 3) {
            color = COLOR_EASY;
        } else if (difficulty <= 7) {
            color = COLOR_MEDIUM;
        } else {
            color = COLOR_HARD;
        }
        
        // Aggiungi il blocco alla mappa
        highlightedBlocks.put(pos, new XRayBlockData(color, Minecraft.getInstance().level.getGameTime() + durationTicks));
    }
    
    /**
     * Rimuove un blocco evidenziato
     */
    public void removeHighlightedBlock(BlockPos pos) {
        highlightedBlocks.remove(pos);
    }
    
    /**
     * Rimuove tutti i blocchi evidenziati
     */
    public void clearHighlightedBlocks() {
        highlightedBlocks.clear();
    }
    
    /**
     * Renderizza tutti i blocchi evidenziati
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
        
        // Rimuovi blocchi scaduti
        Iterator<Map.Entry<BlockPos, XRayBlockData>> iterator = highlightedBlocks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, XRayBlockData> entry = iterator.next();
            if (entry.getValue().expirationTime <= currentTime) {
                iterator.remove();
            }
        }
        
        if (highlightedBlocks.isEmpty()) {
            return;
        }
        
        // Prepara il rendering
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        
        // Ottieni la posizione della camera
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();
        
        // Inizia a disegnare
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = poseStack.last().pose();
        
        // Renderizza ogni blocco
        for (Map.Entry<BlockPos, XRayBlockData> entry : highlightedBlocks.entrySet()) {
            BlockPos pos = entry.getKey();
            int color = entry.getValue().color;
            
            // Verifica se il blocco è ancora valido
            if (mc.level.getBlockState(pos).isAir()) {
                continue;
            }
            
            // Disegna il cubo
            drawCube(bufferBuilder, matrix, pos, cameraPos, color);
        }
        
        // Completa il rendering
        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
        
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }
    
    /**
     * Disegna un cubo alla posizione specificata
     */
    private void drawCube(BufferBuilder bufferBuilder, Matrix4f matrix, BlockPos pos, Vec3 cameraPos, int color) {
        float x = pos.getX() - (float)cameraPos.x;
        float y = pos.getY() - (float)cameraPos.y;
        float z = pos.getZ() - (float)cameraPos.z;
        
        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        float alpha = ((color >> 24) & 0xFF) / 255.0F;
        
        // Faccia inferiore
        bufferBuilder.addVertex(x, y, z).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(x + 1, y, z).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(x + 1, y, z + 1).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(x, y, z + 1).setColor(red, green, blue, alpha);
        
        // Faccia superiore
        bufferBuilder.addVertex(x, y + 1, z).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(x, y + 1, z + 1).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(x + 1, y + 1, z + 1).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(x + 1, y + 1, z).setColor(red, green, blue, alpha);
        
        // Faccia nord
        bufferBuilder.addVertex(x, y, z).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(x, y + 1, z).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(x + 1, y + 1, z).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(x + 1, y, z).setColor(red, green, blue, alpha);
        
        // Faccia sud
        bufferBuilder.addVertex(x, y, z + 1).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(x + 1, y, z + 1).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(x + 1, y + 1, z + 1).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(x, y + 1, z + 1).setColor(red, green, blue, alpha);
        
        // Faccia ovest
        bufferBuilder.addVertex(x, y, z).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(x, y, z + 1).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(x, y + 1, z + 1).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(x, y + 1, z).setColor(red, green, blue, alpha);
        
        // Faccia est
        bufferBuilder.addVertex(x + 1, y, z).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(x + 1, y + 1, z).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(x + 1, y + 1, z + 1).setColor(red, green, blue, alpha);
        bufferBuilder.addVertex(x + 1, y, z + 1).setColor(red, green, blue, alpha);
    }
    
    /**
     * Classe per memorizzare i dati di un blocco evidenziato
     */
    private static class XRayBlockData {
        final int color;
        final long expirationTime;
        
        XRayBlockData(int color, long expirationTime) {
            this.color = color;
            this.expirationTime = expirationTime;
        }
    }
} 
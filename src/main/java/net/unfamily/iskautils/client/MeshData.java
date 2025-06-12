package net.unfamily.iskautils.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.nio.ByteBuffer;

/**
 * Implementazione della classe MeshData necessaria per il rendering XRay
 */
public class MeshData {
    private final ByteBufferBuilder.Result bufferResult;
    private final DrawState drawState;
    
    public MeshData(ByteBufferBuilder.Result bufferResult, DrawState drawState) {
        this.bufferResult = bufferResult;
        this.drawState = drawState;
    }
    
    public ByteBuffer getBuffer() {
        return this.bufferResult.getBuffer();
    }
    
    public DrawState getDrawState() {
        return this.drawState;
    }
    
    public void upload() {
        // Non possiamo usare BufferUploader.drawWithShader direttamente perché accetta solo com.mojang.blaze3d.vertex.MeshData
        // Useremo un approccio alternativo nel renderer
    }
    
    public static class DrawState {
        private final VertexFormat format;
        private final int vertexCount;
        private final int indexCount;
        private final VertexFormat.Mode mode;
        private final VertexFormat.IndexType indexType;
        
        public DrawState(VertexFormat format, int vertexCount, int indexCount, VertexFormat.Mode mode, VertexFormat.IndexType indexType) {
            this.format = format;
            this.vertexCount = vertexCount;
            this.indexCount = indexCount;
            this.mode = mode;
            this.indexType = indexType;
        }
        
        public VertexFormat getFormat() {
            return this.format;
        }
        
        public int getVertexCount() {
            return this.vertexCount;
        }
        
        public int getIndexCount() {
            return this.indexCount;
        }
        
        public VertexFormat.Mode getMode() {
            return this.mode;
        }
        
        public VertexFormat.IndexType getIndexType() {
            return this.indexType;
        }
    }
} 
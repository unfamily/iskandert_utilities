package net.unfamily.iskautils.client;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.lwjgl.system.MemoryUtil;

/**
 * Implementazione della classe ByteBufferBuilder necessaria per il rendering XRay
 */
public class ByteBufferBuilder {
    private ByteBuffer buffer;
    private final int capacity;
    private int position = 0;
    
    public ByteBufferBuilder(int capacity) {
        this.capacity = capacity;
        this.buffer = ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder());
    }
    
    public long reserve(int size) {
        ensureCapacity(size);
        long address = MemoryUtil.memAddress(buffer) + position;
        position += size;
        return address;
    }
    
    private void ensureCapacity(int size) {
        if (position + size > buffer.capacity()) {
            int newCapacity = Math.max(buffer.capacity() * 2, position + size);
            ByteBuffer newBuffer = ByteBuffer.allocateDirect(newCapacity).order(ByteOrder.nativeOrder());
            buffer.position(0);
            newBuffer.put(buffer);
            buffer = newBuffer;
        }
    }
    
    public void clear() {
        buffer.clear();
        position = 0;
    }
    
    @Nullable
    public Result build() {
        if (position == 0) {
            return null;
        }
        
        ByteBuffer resultBuffer = ByteBuffer.allocateDirect(position).order(ByteOrder.nativeOrder());
        buffer.position(0);
        buffer.limit(position);
        resultBuffer.put(buffer);
        buffer.limit(buffer.capacity());
        resultBuffer.position(0);
        
        return new Result(resultBuffer, position);
    }
    
    public static class Result {
        private final ByteBuffer buffer;
        private final int size;
        
        public Result(ByteBuffer buffer, int size) {
            this.buffer = buffer;
            this.size = size;
        }
        
        public ByteBuffer getBuffer() {
            return buffer;
        }
        
        public int getSize() {
            return size;
        }
    }
} 
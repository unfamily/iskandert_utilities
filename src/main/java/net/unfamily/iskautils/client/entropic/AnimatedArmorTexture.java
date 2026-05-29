package net.unfamily.iskautils.client.entropic;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.Tickable;
import net.minecraft.client.resources.metadata.animation.AnimationFrame;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class AnimatedArmorTexture extends AbstractTexture implements Tickable {
    private final ResourceLocation location;
    private NativeImage sourceImage;
    private AnimationMetadataSection animation;
    private List<AnimationFrame> frames = List.of();
    private int frameWidth;
    private int frameHeight;
    private int frameIndex;
    private int subFrame;
    private int currentFrameTime = 1;
    private boolean interpolateFrames;
    private boolean blur;
    private boolean clamp;

    public AnimatedArmorTexture(ResourceLocation location) {
        this.location = location;
    }

    @Override
    public void load(ResourceManager resourceManager) throws IOException {
        closeSourceImage();

        Resource resource = resourceManager.getResourceOrThrow(this.location);
        try (InputStream inputStream = resource.open()) {
            this.sourceImage = NativeImage.read(inputStream);
        }

        this.animation = resource.metadata().getSection(AnimationMetadataSection.SERIALIZER).orElse(null);
        TextureMetadataSection textureMetadata = resource.metadata().getSection(TextureMetadataSection.SERIALIZER).orElse(null);
        this.blur = textureMetadata != null && textureMetadata.isBlur();
        this.clamp = textureMetadata != null && textureMetadata.isClamp();
        setupAnimation(this.sourceImage);
        uploadCurrentFrame();
    }

    @Override
    public void tick() {
        if (this.sourceImage == null || this.frames.size() <= 1) {
            return;
        }

        this.subFrame++;
        if (this.subFrame >= this.currentFrameTime) {
            this.frameIndex = (this.frameIndex + 1) % this.frames.size();
            this.subFrame = 0;
            this.currentFrameTime = this.frames.get(this.frameIndex).getTime(defaultFrameTime());
        }

        uploadCurrentFrame();
    }

    @Override
    public void close() {
        closeSourceImage();
        super.close();
        this.releaseId();
    }

    private void setupAnimation(NativeImage image) {
        this.frameIndex = 0;
        this.subFrame = 0;
        this.interpolateFrames = false;

        if (this.animation == null) {
            this.frameWidth = image.getWidth();
            this.frameHeight = image.getHeight();
            this.frames = List.of(new AnimationFrame(0));
            this.currentFrameTime = 1;
            return;
        }

        this.interpolateFrames = this.animation.isInterpolatedFrames();
        FrameSize frameSize = this.animation.calculateFrameSize(image.getWidth(), image.getHeight());
        this.frameWidth = frameSize.width();
        this.frameHeight = frameSize.height();

        List<AnimationFrame> parsedFrames = new ArrayList<>();
        this.animation.forEachFrame((index, time) -> parsedFrames.add(new AnimationFrame(index, time)));
        if (parsedFrames.isEmpty()) {
            int frameCount = (image.getWidth() / this.frameWidth) * (image.getHeight() / this.frameHeight);
            for (int index = 0; index < frameCount; index++) {
                parsedFrames.add(new AnimationFrame(index));
            }
        }
        this.frames = parsedFrames;
        this.currentFrameTime = this.frames.isEmpty() ? defaultFrameTime() : this.frames.get(0).getTime(defaultFrameTime());
    }

    private int defaultFrameTime() {
        return this.animation == null ? 1 : this.animation.getDefaultFrameTime();
    }

    private int spriteIndexForFrame(int listIndex) {
        return this.frames.get(listIndex).getIndex();
    }

    private float frameProgress() {
        return this.currentFrameTime <= 0 ? 0.0F : (float) this.subFrame / this.currentFrameTime;
    }

    private void uploadCurrentFrame() {
        if (this.sourceImage == null) {
            return;
        }

        if (!RenderSystem.isOnRenderThreadOrInit()) {
            RenderSystem.recordRenderCall(this::uploadCurrentFrameOnRenderThread);
        } else {
            uploadCurrentFrameOnRenderThread();
        }
    }

    private void uploadCurrentFrameOnRenderThread() {
        try (NativeImage frame = buildFrameImage(frameProgress())) {
            TextureUtil.prepareImage(this.getId(), 0, this.frameWidth, this.frameHeight);
            frame.upload(0, 0, 0, 0, 0, this.frameWidth, this.frameHeight, this.blur, this.clamp, false, true);
        }
    }

    private NativeImage buildFrameImage(float progress) {
        int currentIndex = spriteIndexForFrame(this.frameIndex);
        if (!this.interpolateFrames || this.frames.size() <= 1) {
            return extractFrame(currentIndex);
        }

        int nextIndex = spriteIndexForFrame((this.frameIndex + 1) % this.frames.size());
        if (currentIndex == nextIndex) {
            return extractFrame(currentIndex);
        }

        return blendFrames(currentIndex, nextIndex, progress);
    }

    private NativeImage blendFrames(int fromIndex, int toIndex, float progress) {
        float inverse = 1.0F - progress;
        NativeImage from = extractFrame(fromIndex);
        NativeImage to = extractFrame(toIndex);
        NativeImage blended = new NativeImage(this.frameWidth, this.frameHeight, false);
        for (int y = 0; y < this.frameHeight; y++) {
            for (int x = 0; x < this.frameWidth; x++) {
                int p0 = from.getPixelRGBA(x, y);
                int p1 = to.getPixelRGBA(x, y);
                int alpha = mixChannel(inverse, progress, p0 >>> 24, p1 >>> 24);
                int red = mixChannel(inverse, progress, p0 & 0xFF, p1 & 0xFF);
                int green = mixChannel(inverse, progress, (p0 >> 8) & 0xFF, (p1 >> 8) & 0xFF);
                int blue = mixChannel(inverse, progress, (p0 >> 16) & 0xFF, (p1 >> 16) & 0xFF);
                blended.setPixelRGBA(x, y, alpha << 24 | blue << 16 | green << 8 | red);
            }
        }
        from.close();
        to.close();
        return blended;
    }

    private static int mixChannel(float inverse, float progress, int from, int to) {
        return (int) (inverse * from + progress * to);
    }

    private NativeImage extractFrame(int spriteIndex) {
        int columns = this.sourceImage.getWidth() / this.frameWidth;
        int column = spriteIndex % columns;
        int row = spriteIndex / columns;
        int sourceX = column * this.frameWidth;
        int sourceY = row * this.frameHeight;

        NativeImage frame = new NativeImage(this.frameWidth, this.frameHeight, false);
        this.sourceImage.copyRect(frame, sourceX, sourceY, 0, 0, this.frameWidth, this.frameHeight, false, false);
        return frame;
    }

    private void closeSourceImage() {
        if (this.sourceImage != null) {
            this.sourceImage.close();
            this.sourceImage = null;
        }
    }
}

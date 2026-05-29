package net.unfamily.iskautils.client.entropic;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.renderer.texture.ReloadableTexture;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.client.renderer.texture.TickableTexture;
import net.minecraft.client.resources.metadata.animation.AnimationFrame;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ARGB;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class AnimatedArmorTexture extends ReloadableTexture implements TickableTexture {
    private NativeImage sourceImage;
    private AnimationMetadataSection animation;
    private List<AnimationFrame> frames = List.of();
    private int frameWidth;
    private int frameHeight;
    private int frameIndex;
    private int subFrame;
    private int currentFrameTime = 1;
    private boolean interpolateFrames;

    public AnimatedArmorTexture(Identifier location) {
        super(location);
    }

    @Override
    public TextureContents loadContents(ResourceManager resourceManager) throws IOException {
        closeSourceImage();

        Resource resource = resourceManager.getResourceOrThrow(this.resourceId());
        NativeImage image;
        try (InputStream inputStream = resource.open()) {
            image = NativeImage.read(inputStream);
        }

        this.sourceImage = image;
        this.animation = resource.metadata().getSection(AnimationMetadataSection.TYPE).orElse(null);
        setupAnimation(image);

        TextureMetadataSection textureMetadata = resource.metadata().getSection(TextureMetadataSection.TYPE).orElse(null);
        return new TextureContents(buildFrameImage(0.0F), textureMetadata);
    }

    @Override
    protected void doLoad(NativeImage image) {
        if (this.texture != null) {
            this.texture.close();
            this.texture = null;
        }
        if (this.textureView != null) {
            this.textureView.close();
            this.textureView = null;
        }

        GpuDevice device = RenderSystem.getDevice();
        this.texture = device.createTexture(this.resourceId()::toString, 5, TextureFormat.RGBA8, image.getWidth(), image.getHeight(), 1, 1);
        this.textureView = device.createTextureView(this.texture);
        device.createCommandEncoder().writeToTexture(this.texture, image);
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
            this.currentFrameTime = this.frames.get(this.frameIndex).timeOr(defaultFrameTime());
        }

        uploadCurrentFrame();
    }

    @Override
    public void close() {
        closeSourceImage();
        super.close();
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

        this.interpolateFrames = this.animation.interpolatedFrames();
        FrameSize frameSize = this.animation.calculateFrameSize(image.getWidth(), image.getHeight());
        this.frameWidth = frameSize.width();
        this.frameHeight = frameSize.height();

        int frameCount = (image.getWidth() / this.frameWidth) * (image.getHeight() / this.frameHeight);
        if (this.animation.frames().isPresent()) {
            this.frames = this.animation.frames().get();
        } else {
            List<AnimationFrame> defaultFrames = new ArrayList<>(frameCount);
            for (int index = 0; index < frameCount; index++) {
                defaultFrames.add(new AnimationFrame(index));
            }
            this.frames = defaultFrames;
        }

        this.currentFrameTime = this.frames.isEmpty() ? defaultFrameTime() : this.frames.get(0).timeOr(defaultFrameTime());
    }

    private int defaultFrameTime() {
        return this.animation == null ? 1 : this.animation.defaultFrameTime();
    }

    private int spriteIndexForFrame(int listIndex) {
        return this.frames.get(listIndex).index();
    }

    private float frameProgress() {
        return this.currentFrameTime <= 0 ? 0.0F : (float) this.subFrame / this.currentFrameTime;
    }

    private void uploadCurrentFrame() {
        if (this.sourceImage == null || this.texture == null) {
            return;
        }

        try (NativeImage frame = buildFrameImage(frameProgress())) {
            RenderSystem.getDevice().createCommandEncoder().writeToTexture(this.texture, frame);
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
        NativeImage from = extractFrame(fromIndex);
        NativeImage to = extractFrame(toIndex);
        NativeImage blended = new NativeImage(this.frameWidth, this.frameHeight, false);
        for (int y = 0; y < this.frameHeight; y++) {
            for (int x = 0; x < this.frameWidth; x++) {
                blended.setPixel(x, y, ARGB.linearLerp(progress, from.getPixel(x, y), to.getPixel(x, y)));
            }
        }
        from.close();
        to.close();
        return blended;
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

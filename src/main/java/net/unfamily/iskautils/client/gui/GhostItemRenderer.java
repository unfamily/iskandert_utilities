package net.unfamily.iskautils.client.gui;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Renders GUI item previews with true ARGB alpha (ported from Custom Machinery FakeItemRenderer).
 */
public final class GhostItemRenderer {

    private GhostItemRenderer() {}

    public static void render(GuiGraphics graphics, ItemStack stack, int x, int y, int argbColor) {
        if (stack.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        BakedModel model = mc.getItemRenderer().getModel(stack, mc.level, mc.player, 42);
        graphics.pose().pushPose();
        graphics.pose().translate(x + 8, y + 8, 150);

        try {
            graphics.pose().scale(16.0F, -16.0F, 16.0F);
            boolean flatItem = !model.usesBlockLight();
            if (flatItem) {
                Lighting.setupForFlatItems();
            }

            WrappedBufferSource buffer = new WrappedBufferSource(mc.renderBuffers().bufferSource(), argbColor);
            mc.getItemRenderer().render(
                    stack,
                    ItemDisplayContext.GUI,
                    false,
                    graphics.pose(),
                    buffer,
                    LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY,
                    model);
            buffer.end();

            if (flatItem) {
                Lighting.setupFor3DItems();
            }
        } catch (Throwable throwable) {
            CrashReport crashReport = CrashReport.forThrowable(throwable, "Rendering ghost item");
            CrashReportCategory category = crashReport.addCategory("Item being rendered");
            category.setDetail("Item Type", () -> String.valueOf(stack.getItem()));
            category.setDetail("Item Count", () -> String.valueOf(stack.getCount()));
            category.setDetail("Item Foil", () -> String.valueOf(stack.hasFoil()));
            throw new ReportedException(crashReport);
        }

        graphics.pose().popPose();
    }

    private record WrappedBufferSource(MultiBufferSource.BufferSource wrapped, int color) implements MultiBufferSource {
        @Override
        public VertexConsumer getBuffer(RenderType type) {
            return new WrappedVertexConsumer(
                    wrapped.getBuffer(RenderType.entityTranslucentCull(InventoryMenu.BLOCK_ATLAS)),
                    color);
        }

        void end() {
            wrapped.endBatch();
        }
    }

    private record WrappedVertexConsumer(VertexConsumer wrapped, int color) implements VertexConsumer {
        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            wrapped.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            wrapped.setColor(
                    red * argbRed(color) / 255,
                    green * argbGreen(color) / 255,
                    blue * argbBlue(color) / 255,
                    alpha * argbAlpha(color) / 255);
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            wrapped.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            wrapped.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            wrapped.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            wrapped.setNormal(x, y, z);
            return this;
        }

        private static int argbAlpha(int color) {
            return (color >> 24) & 0xFF;
        }

        private static int argbRed(int color) {
            return (color >> 16) & 0xFF;
        }

        private static int argbGreen(int color) {
            return (color >> 8) & 0xFF;
        }

        private static int argbBlue(int color) {
            return color & 0xFF;
        }
    }
}

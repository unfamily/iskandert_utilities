package net.unfamily.iskautils.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.TntMinecartRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.unfamily.iskautils.entity.PrimedEntropyTnt;

@OnlyIn(Dist.CLIENT)
public class PrimedEntropyTntRenderer extends EntityRenderer<PrimedEntropyTnt> {
    private final BlockRenderDispatcher blockRenderer;

    public PrimedEntropyTntRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5F;
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(PrimedEntropyTnt entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.5F, 0.0F);
        int fuse = entity.getFuse();
        if ((float) fuse - partialTick + 1.0F < 10.0F) {
            float scaleFactor = 1.0F - ((float) fuse - partialTick + 1.0F) / 10.0F;
            scaleFactor = Mth.clamp(scaleFactor, 0.0F, 1.0F);
            scaleFactor *= scaleFactor;
            scaleFactor *= scaleFactor;
            float scale = 1.0F + scaleFactor * 0.3F;
            poseStack.scale(scale, scale, scale);
        }

        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        poseStack.translate(-0.5F, -0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        TntMinecartRenderer.renderWhiteSolidBlock(
                this.blockRenderer, entity.getBlockState(), poseStack, buffer, packedLight, fuse / 5 % 2 == 0);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(PrimedEntropyTnt entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}

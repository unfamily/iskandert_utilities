package net.unfamily.iskautils.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.TntMinecartRenderer;
import net.minecraft.client.renderer.entity.TntRenderer;
import net.minecraft.client.renderer.entity.state.TntRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.unfamily.iskautils.entity.PrimedEntropyTnt;

public class PrimedEntropyTntRenderer extends EntityRenderer<PrimedEntropyTnt, TntRenderState> {
    private final BlockModelResolver blockModelResolver;

    public PrimedEntropyTntRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5F;
        this.blockModelResolver = context.getBlockModelResolver();
    }

    @Override
    public void submit(TntRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.5F, 0.0F);
        float fuse = state.fuseRemainingInTicks;
        if (state.fuseRemainingInTicks < 10.0F) {
            float g = 1.0F - state.fuseRemainingInTicks / 10.0F;
            g = Mth.clamp(g, 0.0F, 1.0F);
            g *= g;
            g *= g;
            float s = 1.0F + g * 0.3F;
            poseStack.scale(s, s, s);
        }

        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        poseStack.translate(-0.5F, -0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        if (!state.blockState.isEmpty()) {
            TntMinecartRenderer.submitWhiteSolidBlock(
                    state.blockState, poseStack, submitNodeCollector, state.lightCoords, (int) fuse / 5 % 2 == 0, state.outlineColor);
        }

        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    @Override
    public TntRenderState createRenderState() {
        return new TntRenderState();
    }

    @Override
    public void extractRenderState(PrimedEntropyTnt entity, TntRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.fuseRemainingInTicks = entity.getFuse() - partialTicks + 1.0F;
        this.blockModelResolver.update(state.blockState, entity.getBlockState(), TntRenderer.BLOCK_DISPLAY_CONTEXT);
    }
}

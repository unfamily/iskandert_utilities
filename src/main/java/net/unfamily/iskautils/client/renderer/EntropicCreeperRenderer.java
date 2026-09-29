package net.unfamily.iskautils.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.creeper.CreeperModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EnergySwirlLayer;
import net.minecraft.client.renderer.entity.state.CreeperRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.entity.EntropicCreeper;

public class EntropicCreeperRenderer extends MobRenderer<EntropicCreeper, CreeperRenderState, CreeperModel> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/entity/entropic_creeper.png");
    private static final Identifier POWER_TEXTURE =
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/entity/entropic_creeper_armor.png");

    public EntropicCreeperRenderer(EntityRendererProvider.Context context) {
        super(context, new CreeperModel(context.bakeLayer(ModelLayers.CREEPER)), 0.5F);
        this.addLayer(new PowerLayer(this, context.getModelSet()));
    }

    @Override
    protected void scale(CreeperRenderState state, PoseStack poseStack) {
        float g = state.swelling;
        float wobble = 1.0F + Mth.sin(g * 100.0F) * g * 0.01F;
        g = Mth.clamp(g, 0.0F, 1.0F);
        g *= g;
        g *= g;
        float s = (1.0F + g * 0.4F) * wobble;
        float hs = (1.0F + g * 0.1F) / wobble;
        poseStack.scale(s, hs, s);
    }

    @Override
    protected float getWhiteOverlayProgress(CreeperRenderState state) {
        float step = state.swelling;
        return (int) (step * 10.0F) % 2 == 0 ? 0.0F : Mth.clamp(step, 0.5F, 1.0F);
    }

    @Override
    public Identifier getTextureLocation(CreeperRenderState state) {
        return TEXTURE;
    }

    @Override
    public CreeperRenderState createRenderState() {
        return new CreeperRenderState();
    }

    @Override
    public void extractRenderState(EntropicCreeper entity, CreeperRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.swelling = entity.getSwelling(partialTicks);
        state.isPowered = entity.isPowered();
    }

    private static final class PowerLayer extends EnergySwirlLayer<CreeperRenderState, CreeperModel> {
        private final CreeperModel model;

        PowerLayer(RenderLayerParent<CreeperRenderState, CreeperModel> parent, EntityModelSet modelSet) {
            super(parent);
            this.model = new CreeperModel(modelSet.bakeLayer(ModelLayers.CREEPER_ARMOR));
        }

        @Override
        protected boolean isPowered(CreeperRenderState state) {
            return state.isPowered;
        }

        @Override
        protected float xOffset(float t) {
            return t * 0.01F;
        }

        @Override
        protected Identifier getTextureLocation() {
            return POWER_TEXTURE;
        }

        @Override
        protected CreeperModel model() {
            return this.model;
        }
    }
}

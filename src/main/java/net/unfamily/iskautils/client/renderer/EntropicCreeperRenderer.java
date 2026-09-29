package net.unfamily.iskautils.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.CreeperModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EnergySwirlLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.entity.EntropicCreeper;

@OnlyIn(Dist.CLIENT)
public class EntropicCreeperRenderer extends MobRenderer<EntropicCreeper, CreeperModel<EntropicCreeper>> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/entity/entropic_creeper.png");
    private static final ResourceLocation POWER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/entity/entropic_creeper_armor.png");

    public EntropicCreeperRenderer(EntityRendererProvider.Context context) {
        super(context, new CreeperModel<>(context.bakeLayer(ModelLayers.CREEPER)), 0.5F);
        this.addLayer(new PowerLayer(this, context.getModelSet()));
    }

    @Override
    protected void scale(EntropicCreeper entity, PoseStack poseStack, float partialTick) {
        float swell = entity.getSwelling(partialTick);
        float pulse = 1.0F + Mth.sin(swell * 100.0F) * swell * 0.01F;
        swell = Mth.clamp(swell, 0.0F, 1.0F);
        swell *= swell;
        swell *= swell;
        float xz = (1.0F + swell * 0.4F) * pulse;
        float y = (1.0F + swell * 0.1F) / pulse;
        poseStack.scale(xz, y, xz);
    }

    @Override
    protected float getWhiteOverlayProgress(EntropicCreeper entity, float partialTick) {
        float swell = entity.getSwelling(partialTick);
        return (int) (swell * 10.0F) % 2 == 0 ? 0.0F : Mth.clamp(swell, 0.5F, 1.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(EntropicCreeper entity) {
        return TEXTURE;
    }

    @OnlyIn(Dist.CLIENT)
    private static final class PowerLayer extends EnergySwirlLayer<EntropicCreeper, CreeperModel<EntropicCreeper>> {
        private final CreeperModel<EntropicCreeper> model;

        PowerLayer(EntropicCreeperRenderer parent, EntityModelSet modelSet) {
            super(parent);
            this.model = new CreeperModel<>(modelSet.bakeLayer(ModelLayers.CREEPER_ARMOR));
        }

        @Override
        protected float xOffset(float tick) {
            return tick * 0.01F;
        }

        @Override
        protected ResourceLocation getTextureLocation() {
            return POWER_TEXTURE;
        }

        @Override
        protected EntityModel<EntropicCreeper> model() {
            return this.model;
        }
    }
}

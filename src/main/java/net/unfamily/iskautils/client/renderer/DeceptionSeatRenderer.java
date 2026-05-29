package net.unfamily.iskautils.client.renderer;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.unfamily.iskautils.entity.DeceptionSeatEntity;

/** Invisible seat entity — no model or texture. */
public class DeceptionSeatRenderer extends EntityRenderer<DeceptionSeatEntity> {
    public DeceptionSeatRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(DeceptionSeatEntity entity) {
        return null;
    }
}

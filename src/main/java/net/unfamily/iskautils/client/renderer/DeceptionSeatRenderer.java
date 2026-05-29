package net.unfamily.iskautils.client.renderer;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.unfamily.iskautils.entity.DeceptionSeatEntity;

/** Invisible seat entity — no model, shadow, or name tag. */
public class DeceptionSeatRenderer extends EntityRenderer<DeceptionSeatEntity, EntityRenderState> {
    public DeceptionSeatRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }

    @Override
    protected boolean shouldShowName(DeceptionSeatEntity entity, double distanceToCameraSq) {
        return false;
    }
}

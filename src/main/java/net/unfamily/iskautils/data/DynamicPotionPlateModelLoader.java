package net.unfamily.iskautils.data;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
import org.slf4j.Logger;

import java.util.function.Function;

/**
 * Custom model loader for dynamic potion plate models
 */
public class DynamicPotionPlateModelLoader implements IGeometryLoader<DynamicPotionPlateModelLoader.Geometry> {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public static final DynamicPotionPlateModelLoader INSTANCE = new DynamicPotionPlateModelLoader();
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("iska_utils", "dynamic_potion_plate");
    
    private DynamicPotionPlateModelLoader() {}
    
    @Override
    public Geometry read(JsonObject jsonObject, JsonDeserializationContext context) throws JsonParseException {
        // The texture will be determined by the model file name
        LOGGER.debug("Loading dynamic potion plate model");
        return new Geometry();
    }
    
    public static class Geometry implements IUnbakedGeometry<Geometry> {
        
        public Geometry() {
        }
        
        @Override
        public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ItemOverrides overrides) {
            // Use a default texture - the actual texture will be handled by the specific model files
            TextureAtlasSprite defaultTexture = spriteGetter.apply(new Material(
                net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS,
                ResourceLocation.fromNamespaceAndPath("iska_utils", "block/plate_base")
            ));
            
            TextureAtlasSprite particleTexture = defaultTexture;
            
            return new DynamicPotionPlateBakedModel(
                context.useAmbientOcclusion(),
                context.isGui3d(),
                context.useBlockLight(),
                particleTexture,
                overrides,
                defaultTexture,
                defaultTexture
            );
        }
        
        @Override
        public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter, IGeometryBakingContext context) {
            // No parents to resolve for our simple model
        }
    }
} 
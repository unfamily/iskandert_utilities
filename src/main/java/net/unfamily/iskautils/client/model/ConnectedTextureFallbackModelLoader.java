package net.unfamily.iskautils.client.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
import net.unfamily.iskautils.IskaUtils;

import java.util.function.Function;

/**
 * Geometry loader that uses a Fusion connected model when Fusion is present,
 * otherwise falls back to a plain model (cube or glass pane template).
 */
public final class ConnectedTextureFallbackModelLoader implements IGeometryLoader<ConnectedTextureFallbackModelLoader.Geometry> {

    public static final ConnectedTextureFallbackModelLoader INSTANCE = new ConnectedTextureFallbackModelLoader();
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "connected_texture_fallback");

    private ConnectedTextureFallbackModelLoader() {}

    @Override
    public Geometry read(JsonObject jsonObject, JsonDeserializationContext deserializationContext) throws JsonParseException {
        JsonElement connectedModelEl = jsonObject.get("connected_model");
        if (!(connectedModelEl instanceof JsonObject connectedModelObj)) {
            throw new JsonParseException("ConnectedTextureFallbackModelLoader requires JSON object \"connected_model\".");
        }

        UnbakedModel delegate;
        if (ModList.get().isLoaded("fusion")) {
            delegate = deserializationContext.deserialize(connectedModelObj, UnbakedModel.class);
        } else {
            JsonElement fallbackModelEl = jsonObject.get("fallback_model");
            if (fallbackModelEl instanceof JsonObject fallbackModelObj) {
                delegate = deserializationContext.deserialize(fallbackModelObj, UnbakedModel.class);
            } else {
                JsonElement singleTextureEl = jsonObject.get("single_texture");
                if (!(singleTextureEl instanceof JsonPrimitive prim) || !prim.isString()) {
                    throw new JsonParseException(
                            "ConnectedTextureFallbackModelLoader requires \"fallback_model\" or \"single_texture\" when Fusion is absent.");
                }
                JsonObject singleModelJson = new JsonObject();
                singleModelJson.addProperty("parent", "minecraft:block/cube_all");
                JsonObject textures = new JsonObject();
                textures.addProperty("all", prim.getAsString());
                singleModelJson.add("textures", textures);
                delegate = deserializationContext.deserialize(singleModelJson, UnbakedModel.class);
            }
        }

        return new Geometry(delegate);
    }

    public static final class Geometry implements IUnbakedGeometry<Geometry> {
        private final UnbakedModel delegate;

        private Geometry(UnbakedModel delegate) {
            this.delegate = delegate;
        }

        @Override
        public BakedModel bake(
                IGeometryBakingContext context,
                ModelBaker baker,
                Function<Material, TextureAtlasSprite> spriteGetter,
                ModelState modelState,
                ItemOverrides overrides) {
            return delegate.bake(baker, spriteGetter, modelState);
        }

        @Override
        public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter, IGeometryBakingContext context) {
            delegate.resolveParents(modelGetter);
        }
    }
}

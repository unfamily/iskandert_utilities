package net.unfamily.iskautils.client.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.resources.model.UnbakedModel;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.model.UnbakedModelLoader;

/**
 * Model loader that uses a Fusion connected model when Fusion is present,
 * otherwise falls back to a plain model (cube or glass pane template).
 */
public final class ConnectedTextureFallbackModelLoader implements UnbakedModelLoader<UnbakedModel> {

    @Override
    public UnbakedModel read(JsonObject jsonObject, JsonDeserializationContext deserializationContext) throws JsonParseException {
        JsonElement connectedModelEl = jsonObject.get("connected_model");
        if (!(connectedModelEl instanceof JsonObject connectedModelObj)) {
            throw new JsonParseException("ConnectedTextureFallbackModelLoader requires JSON object \"connected_model\".");
        }

        if (ModList.get().isLoaded("fusion")) {
            return deserializationContext.deserialize(connectedModelObj, UnbakedModel.class);
        }

        JsonElement fallbackModelEl = jsonObject.get("fallback_model");
        if (fallbackModelEl instanceof JsonObject fallbackModelObj) {
            return deserializationContext.deserialize(fallbackModelObj, UnbakedModel.class);
        }

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

        return deserializationContext.deserialize(singleModelJson, UnbakedModel.class);
    }
}

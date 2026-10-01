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
 * Model loader (NeoForge 26+ UnbakedModelLoader) that uses Fusion connected textures
 * when the Fusion mod is present, otherwise falls back to a plain model.
 *
 * <p>JSON fields:
 * <ul>
 *   <li>{@code connected_model} (required): Full Fusion model JSON (loader: fusion:model, …)</li>
 *   <li>{@code single_texture} (optional): Resource location for a cube_all fallback texture.
 *       Used when Fusion is absent and no {@code fallback_model} is provided.</li>
 *   <li>{@code fallback_model} (optional): Inline model JSON used when Fusion is absent.
 *       Takes priority over {@code single_texture}. Typically used for pane templates.</li>
 *   <li>{@code render_type} (optional): Render type forwarded to the {@code single_texture}
 *       fallback (e.g. {@code "minecraft:translucent"}). In NeoForge 26+ render_type in the
 *       outer JSON is not applied automatically — the loader must propagate it.
 *       Ignored for the Fusion path (use *.png.mcmeta instead).</li>
 * </ul>
 */
public final class ConnectedTextureFallbackModelLoader implements UnbakedModelLoader<UnbakedModel> {

    @Override
    public UnbakedModel read(JsonObject jsonObject, JsonDeserializationContext deserializationContext) throws JsonParseException {
        // connected_model is always required
        JsonElement connectedModelEl = jsonObject.get("connected_model");
        if (!(connectedModelEl instanceof JsonObject connectedModelObj)) {
            throw new JsonParseException("ConnectedTextureFallbackModelLoader requires JSON object \"connected_model\".");
        }

        if (ModList.get().isLoaded("fusion")) {
            // Fusion path: let Fusion's loader parse the connected model JSON directly.
            return deserializationContext.deserialize(connectedModelObj, UnbakedModel.class);
        }

        // No-Fusion path: prefer an explicit fallback_model (used for pane templates, etc.)
        JsonElement fallbackModelEl = jsonObject.get("fallback_model");
        if (fallbackModelEl instanceof JsonObject fallbackModelObj) {
            return deserializationContext.deserialize(fallbackModelObj, UnbakedModel.class);
        }

        // Otherwise build a cube_all from single_texture
        JsonElement singleTextureEl = jsonObject.get("single_texture");
        if (!(singleTextureEl instanceof JsonPrimitive prim) || !prim.isString()) {
            throw new JsonParseException(
                    "ConnectedTextureFallbackModelLoader requires \"fallback_model\" or \"single_texture\" when Fusion is absent.");
        }

        JsonObject singleModelJson = new JsonObject();
        singleModelJson.addProperty("parent", "minecraft:block/cube_all");

        // In NeoForge 26+ the UnbakedModelLoader fully owns the model JSON; render_type at
        // the outer level is NOT applied automatically, so we propagate it explicitly.
        JsonElement renderTypeEl = jsonObject.get("render_type");
        if (renderTypeEl instanceof JsonPrimitive rtPrim && rtPrim.isString()) {
            singleModelJson.addProperty("render_type", rtPrim.getAsString());
        }

        JsonObject textures = new JsonObject();
        textures.addProperty("all", prim.getAsString());
        singleModelJson.add("textures", textures);

        return deserializationContext.deserialize(singleModelJson, UnbakedModel.class);
    }
}

package net.unfamily.iskautils.client.model;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.SmartTimerBlock;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * BakedModel wrapper per Smart Timer che modifica le texture delle facce
 * in base alla configurazione I/O del BlockEntity
 */
public class SmartTimerBakedModel extends BakedModelWrapper<BakedModel> {
    
    // Texture resources (path senza .png, come nei modelli JSON)
    private static final ResourceLocation BLANK_TEXTURE = ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "block/redstone_machines/general/blank");
    private static final ResourceLocation INPUT_OFF_TEXTURE = ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "block/redstone_machines/general/input_off");
    private static final ResourceLocation INPUT_ON_TEXTURE = ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "block/redstone_machines/general/input_on");
    
    private final Function<ResourceLocation, TextureAtlasSprite> textureGetter;
    
    public SmartTimerBakedModel(BakedModel originalModel, Function<ResourceLocation, TextureAtlasSprite> textureGetter) {
        super(originalModel);
        this.textureGetter = textureGetter;
    }
    
    /**
     * Ottiene una TextureAtlasSprite dal ResourceLocation usando il textureGetter
     */
    private TextureAtlasSprite getSprite(ResourceLocation location) {
        return textureGetter.apply(location);
    }
    
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData extraData, @Nullable RenderType renderType) {
        List<BakedQuad> quads = new ArrayList<>(originalModel.getQuads(state, side, rand, extraData, renderType));
        
        // Se non abbiamo dati o lo stato non è valido, ritorna i quads originali
        if (state == null || !(state.getBlock() instanceof SmartTimerBlock)) {
            return quads;
        }
        
        // Ottieni i dati I/O dal ModelData
        byte[] ioConfig = extraData.get(SmartTimerModelData.IO_CONFIG_PROPERTY);
        boolean[] inputStates = extraData.get(SmartTimerModelData.INPUT_STATES_PROPERTY);
        
        if (ioConfig == null) {
            return quads;
        }
        
        Direction facing = state.getValue(SmartTimerBlock.FACING);
        
        // Se side è null, iteriamo su tutte le facce (non dovrebbe succedere per blocchi standard)
        // Se side è specifico, processiamo solo quella faccia
        if (side == null) {
            // Per side null, non modifichiamo nulla (tutti i quads vengono ritornati così come sono)
            return quads;
        }
        
        // Se questa faccia è il front, non modificarla
        if (side == facing) {
            return quads;
        }
        
        byte faceIoConfig = ioConfig[side.ordinal()];
        
        // Determina quale texture usare per questa faccia
        TextureAtlasSprite newSprite = null;
        if (faceIoConfig == 0) { // BLANK
            newSprite = getSprite(BLANK_TEXTURE);
        } else if (faceIoConfig == 1) { // INPUT
            boolean hasSignal = inputStates != null && inputStates[side.ordinal()];
            ResourceLocation textureLoc = hasSignal ? INPUT_ON_TEXTURE : INPUT_OFF_TEXTURE;
            newSprite = getSprite(textureLoc);
        }
        // OUTPUT (faceIoConfig == 2) non ha texture dedicata, usa il comportamento default (quads originali)
        
        if (newSprite == null) {
            return quads;
        }
        
        // Sostituisci i quads di questa faccia con nuovi quads usando la nuova texture
        // Copiamo i vertici originali ma cambiamo solo la sprite
        // Le coordinate UV normalizzate (0-1) funzioneranno per qualsiasi sprite 16x16
        List<BakedQuad> newQuads = new ArrayList<>();
        for (BakedQuad quad : quads) {
            if (quad.getDirection() == side) {
                // Crea un nuovo quad con la nuova sprite ma gli stessi vertici
                // Le coordinate UV nei vertici sono normalizzate (0-1), quindi funzioneranno per qualsiasi sprite 16x16
                int[] vertices = quad.getVertices();
                int[] newVertices = new int[vertices.length];
                System.arraycopy(vertices, 0, newVertices, 0, vertices.length);
                
                // Crea il nuovo BakedQuad con la nuova sprite
                newQuads.add(new BakedQuad(newVertices, quad.getTintIndex(), side, newSprite, quad.isShade()));
            } else {
                // Mantieni i quads delle altre facce
                newQuads.add(quad);
            }
        }
        
        return newQuads;
    }
    
}

package net.unfamily.iskautils.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.iskautils.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

/**
 * Gestisce il salvataggio delle strutture lato client
 */
public class ClientStructureSaver {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientStructureSaver.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    
    /**
     * Salva una struttura sul client
     */
    public static void saveStructure(String structureName, String structureId, 
                                   BlockPos vertex1, BlockPos vertex2, BlockPos center, 
                                   ClientLevel level) throws IOException {
        
        LOGGER.info("=== SAVING STRUCTURE CLIENT-SIDE ===");
        LOGGER.info("Structure name: '{}'", structureName);
        LOGGER.info("Structure ID: '{}'", structureId);
        
        // Logica di salvataggio identica a quella del server ma lato client
        saveToPlayerStructuresFile(createStructureJson(structureName, structureId, vertex1, vertex2, center, level));
        
        // Ricarica le strutture client
        net.unfamily.iskautils.structure.StructureLoader.reloadAllDefinitions(true);
    }
    
    private static JsonObject createStructureJson(String structureName, String structureId, 
                                                BlockPos vertex1, BlockPos vertex2, BlockPos center, 
                                                ClientLevel level) {
        // TODO: Implementazione completa identica al server
        // Per ora struttura minima corretta
        JsonObject root = new JsonObject();
        root.addProperty("type", "iska_utils:structure");
        root.addProperty("overwritable", true);
        
        JsonArray structureArray = new JsonArray();
        JsonObject structureObj = new JsonObject();
        
        structureObj.addProperty("id", structureId);
        structureObj.addProperty("name", structureName);
        
        // Campi opzionali vuoti
        structureObj.add("can_replace", new JsonArray());
        
        // Icona corretta
        JsonObject icon = new JsonObject();
        icon.addProperty("type", "minecraft:item");
        icon.addProperty("item", "iska_utils:blueprint");
        structureObj.add("icon", icon);
        
        // Descrizione vuota
        structureObj.add("description", new JsonArray());
        
        // Pattern vuoto per ora
        structureObj.add("pattern", new JsonArray());
        
        // Key vuota per ora
        structureObj.add("key", new JsonObject());
        
        structureArray.add(structureObj);
        root.add("structure", structureArray);
        return root;
    }
    
    private static void saveToPlayerStructuresFile(JsonObject newStructure) throws IOException {
        String configPath = Config.clientStructurePath;
        if (configPath == null || configPath.trim().isEmpty()) {
            configPath = "iska_utils_client/structures";
        }
        
        Path structuresDir = Paths.get(configPath);
        Path playerStructuresFile = structuresDir.resolve("player_structures.json");
        
        if (!Files.exists(structuresDir)) {
            Files.createDirectories(structuresDir);
        }
        
        // Salvataggio temporaneo semplificato
        Files.writeString(playerStructuresFile, GSON.toJson(newStructure), 
                         StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
} 
package net.unfamily.iskautils.structure;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;

/**
 * Classe responsabile del piazzamento fisico delle strutture nel mondo
 */
public class StructurePlacer {
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Piazza una struttura nel mondo
     * 
     * @param level Il livello del server dove piazzare la struttura
     * @param centerPos La posizione dove piazzare il centro della struttura
     * @param structure La definizione della struttura da piazzare
     * @param player Il giocatore che ha richiesto il piazzamento (può essere null)
     * @return true se il piazzamento è riuscito, false altrimenti
     */
    public static boolean placeStructure(ServerLevel level, BlockPos centerPos, StructureDefinition structure, ServerPlayer player) {
        try {
            // Verifica i prerequisiti
            if (!canPlaceStructure(level, centerPos, structure, player)) {
                LOGGER.debug("Impossibile piazzare la struttura {} - prerequisiti non soddisfatti", structure.getId());
                return false;
            }

            // Ottieni il pattern della struttura
            String[][][][] pattern = structure.getPattern();
            if (pattern == null || pattern.length == 0) {
                LOGGER.error("Struttura {} ha un pattern vuoto o nullo", structure.getId());
                return false;
            }

            // Trova la posizione relativa del centro
            BlockPos relativeCenter = structure.findCenter();
            if (relativeCenter == null) {
                relativeCenter = new BlockPos(0, 0, 0);
                LOGGER.warn("Centro non trovato per la struttura {}, uso (0,0,0)", structure.getId());
            }

            // Calcola l'offset per posizionare correttamente la struttura
            // Sottrae la posizione relativa del centro e alza di un blocco
            BlockPos offsetPos = centerPos.subtract(relativeCenter).above();

            // Piazza ogni blocco del pattern
            // Pattern convertito dal loader: [Y][X][Z][caratteri] 
            // dove Y=altezza, X=est-ovest, Z=nord-sud, caratteri=multiple posizioni Z dalla stringa
            Map<String, List<StructureDefinition.BlockDefinition>> key = structure.getKey();
            
            for (int y = 0; y < pattern.length; y++) {                    // Y (altezza/layer)
                for (int x = 0; x < pattern[y].length; x++) {             // X (est-ovest) 
                    for (int z = 0; z < pattern[y][x].length; z++) {      // Z (nord-sud)
                        String[] cellChars = pattern[y][x][z];
                        
                        if (cellChars != null) {
                            for (int charIndex = 0; charIndex < cellChars.length; charIndex++) {
                                String character = cellChars[charIndex];
                                
                                if (character == null || character.equals(" ")) {
                                    continue; // Salta gli spazi vuoti
                                }
                                
                                // Calcola la posizione assoluta di questo blocco
                                // Ogni carattere della stringa Z rappresenta una posizione Z aggiuntiva
                                int worldX = x;
                                int worldY = y;
                                int worldZ = z * cellChars.length + charIndex;
                                
                                BlockPos blockPos = offsetPos.offset(worldX, worldY, worldZ);
                                
                                // Piazza il blocco basato sulla chiave
                                if (!placeBlockFromKey(level, blockPos, character, key, structure)) {
                                    LOGGER.warn("Impossibile piazzare il blocco '{}' alla posizione {} per la struttura {}", 
                                        character, blockPos, structure.getId());
                                    // Continua comunque con gli altri blocchi
                                }
                            }
                        }
                    }
                }
            }

            LOGGER.info("Struttura {} piazzata con successo al centro {}", structure.getId(), centerPos);
            return true;

        } catch (Exception e) {
            LOGGER.error("Errore durante il piazzamento della struttura {}: {}", structure.getId(), e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            }
            return false;
        }
    }

    /**
     * Verifica se una struttura può essere piazzata
     */
    private static boolean canPlaceStructure(ServerLevel level, BlockPos centerPos, StructureDefinition structure, ServerPlayer player) {
        // Verifica gli stage se specificati
        if (structure.getStages() != null && !structure.getStages().isEmpty()) {
            if (!structure.canPlace(player)) {
                LOGGER.debug("Stage non soddisfatti per la struttura {}", structure.getId());
                return false;
            }
        }

        // Per ora permettiamo sempre il piazzamento
        // Più avanti si possono aggiungere controlli per:
        // - Verifica dello spazio disponibile
        // - Controllo dei blocchi rimpiazzabili
        // - Verifica dell'inventario del giocatore per i materiali necessari
        
        return true;
    }

    /**
     * Piazza un singolo blocco basato sulla definizione nella chiave
     */
    private static boolean placeBlockFromKey(ServerLevel level, BlockPos pos, String keyCharacter, 
                                           Map<String, List<StructureDefinition.BlockDefinition>> key, 
                                           StructureDefinition structure) {
        
        // Ottieni le definizioni per questo carattere
        List<StructureDefinition.BlockDefinition> blockDefs = key.get(keyCharacter);
        if (blockDefs == null || blockDefs.isEmpty()) {
            LOGGER.warn("Nessuna definizione trovata per il carattere '{}' nella struttura {}", keyCharacter, structure.getId());
            return false;
        }

        // Usa la prima definizione disponibile (in futuro si potranno implementare alternative)
        StructureDefinition.BlockDefinition blockDef = blockDefs.get(0);

        try {
            BlockState blockState = getBlockStateFromDefinition(blockDef);
            if (blockState != null) {
                // Verifica se possiamo rimpiazzare il blocco esistente
                if (canReplaceBlock(level, pos, structure)) {
                    level.setBlock(pos, blockState, 3); // Flag 3 = aggiorna + notifica clienti
                    return true;
                } else {
                    LOGGER.debug("Impossibile rimpiazzare il blocco alla posizione {} per la struttura {}", pos, structure.getId());
                    return false;
                }
            } else {
                LOGGER.warn("Impossibile creare BlockState dalla definizione per '{}' nella struttura {}", keyCharacter, structure.getId());
                return false;
            }
        } catch (Exception e) {
            LOGGER.error("Errore nel piazzamento del blocco '{}' alla posizione {} per la struttura {}: {}", 
                keyCharacter, pos, structure.getId(), e.getMessage());
            return false;
        }
    }

    /**
     * Crea un BlockState dalla definizione del blocco
     */
    private static BlockState getBlockStateFromDefinition(StructureDefinition.BlockDefinition blockDef) {
        try {
            // Gestisci i blocchi definiti per nome
            if (blockDef.getBlock() != null && !blockDef.getBlock().isEmpty()) {
                ResourceLocation blockId = ResourceLocation.parse(blockDef.getBlock());
                Block block = BuiltInRegistries.BLOCK.get(blockId);
                
                if (block == Blocks.AIR && !blockDef.getBlock().equals("minecraft:air")) {
                    LOGGER.warn("Blocco non trovato: {}", blockDef.getBlock());
                    return null;
                }
                
                BlockState blockState = block.defaultBlockState();
                
                // Applica le proprietà se specificate
                if (blockDef.getProperties() != null && !blockDef.getProperties().isEmpty()) {
                    blockState = applyProperties(blockState, blockDef.getProperties());
                }
                
                return blockState;
            }
            
            // TODO: Gestisci i tag (blocchi definiti tramite tag)
            if (blockDef.getTag() != null && !blockDef.getTag().isEmpty()) {
                LOGGER.warn("Gestione dei tag non ancora implementata: {}", blockDef.getTag());
                return Blocks.STONE.defaultBlockState(); // Fallback temporaneo
            }
            
            LOGGER.warn("Definizione di blocco vuota o non valida");
            return null;
            
        } catch (Exception e) {
            LOGGER.error("Errore nella creazione del BlockState: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Applica le proprietà ad un BlockState
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState applyProperties(BlockState blockState, Map<String, String> properties) {
        try {
            BlockState result = blockState;
            
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                String propertyName = entry.getKey();
                String propertyValue = entry.getValue();
                
                // Trova la proprietà nel BlockState
                Property<?> property = null;
                for (Property<?> prop : blockState.getProperties()) {
                    if (prop.getName().equals(propertyName)) {
                        property = prop;
                        break;
                    }
                }
                
                if (property == null) {
                    LOGGER.warn("Proprietà '{}' non trovata per il blocco {}", propertyName, blockState.getBlock());
                    continue;
                }
                
                // Prova ad applicare il valore
                try {
                    Comparable value = property.getValue(propertyValue).orElse(null);
                    if (value != null) {
                        result = result.setValue((Property) property, value);
                    } else {
                        LOGGER.warn("Valore '{}' non valido per la proprietà '{}' del blocco {}", 
                            propertyValue, propertyName, blockState.getBlock());
                    }
                } catch (Exception e) {
                    LOGGER.warn("Errore nell'applicazione della proprietà '{}={}' al blocco {}: {}", 
                        propertyName, propertyValue, blockState.getBlock(), e.getMessage());
                }
            }
            
            return result;
        } catch (Exception e) {
            LOGGER.error("Errore nell'applicazione delle proprietà: {}", e.getMessage());
            return blockState; // Ritorna lo stato originale in caso di errore
        }
    }

    /**
     * Verifica se un blocco può essere rimpiazzato
     */
    private static boolean canReplaceBlock(ServerLevel level, BlockPos pos, StructureDefinition structure) {
        BlockState existingState = level.getBlockState(pos);
        
        // L'aria può sempre essere rimpiazzata
        if (existingState.isAir()) {
            return true;
        }
        
        // Controlla la lista can_replace della struttura
        List<String> canReplace = structure.getCanReplace();
        if (canReplace != null && !canReplace.isEmpty()) {
            String blockId = BuiltInRegistries.BLOCK.getKey(existingState.getBlock()).toString();
            
            for (String replaceableId : canReplace) {
                if (replaceableId.equals(blockId)) {
                    return true;
                }
                
                // TODO: Gestisci i casi speciali come $replaceable, $fluids, ecc.
                // TODO: Gestisci i tag con prefisso #
            }
            
            return false; // Se c'è una lista can_replace e il blocco non è in lista, non può essere rimpiazzato
        }
        
        // Se non c'è una lista can_replace, per sicurezza rimpiazziamo solo l'aria
        // In futuro si può cambiare questo comportamento
        return existingState.isAir();
    }
} 
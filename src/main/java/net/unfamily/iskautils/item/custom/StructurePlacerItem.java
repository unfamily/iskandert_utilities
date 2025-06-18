package net.unfamily.iskautils.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.BlockItem;
import net.unfamily.iskautils.client.MarkRenderer;
import net.unfamily.iskautils.network.packet.StructurePlacerGuiOpenC2SPacket;
import net.unfamily.iskautils.structure.StructureDefinition;
import net.unfamily.iskautils.structure.StructureLoader;
import net.unfamily.iskautils.structure.StructurePlacer;
import net.unfamily.iskautils.util.ModUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Item per piazzare strutture nel mondo con GUI di selezione
 */
public class StructurePlacerItem extends Item {
    
    public static final String SELECTED_STRUCTURE_KEY = "selected_structure";
    public static final String PREVIEW_MODE_KEY = "preview_mode";
    public static final String LAST_PREVIEW_POS_KEY = "last_preview_pos";
    public static final String LAST_CLICK_TIME_KEY = "last_click_time";
    public static final String LAST_CLICK_POS_KEY = "last_click_pos";
    
    // Colori per i marker
    public static final int PREVIEW_COLOR = 0x804444FF; // Azzurro semi-trasparente
    public static final int CONFLICT_COLOR = 0x80FF4444; // Rosso semi-trasparente
    public static final int SUCCESS_COLOR = 0x8044FF44; // Verde semi-trasparente
    
    public StructurePlacerItem(Properties properties) {
        super(properties);
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            // Usa il sistema testuale per la selezione struttura
            openStructureSelectionGui(serverPlayer, stack);
        }
        
        return InteractionResultHolder.success(stack);
    }
    
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockPos pos = context.getClickedPos(); // Piazza alla stessa altezza del blocco cliccato
        
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        
        String structureId = getSelectedStructure(stack);
        if (structureId == null || structureId.isEmpty()) {
            player.displayClientMessage(Component.literal("§cNo structure selected! Right-click to select."), true);
            return InteractionResult.FAIL;
        }
        
        StructureDefinition structure = StructureLoader.getStructure(structureId);
        if (structure == null) {
            player.displayClientMessage(Component.literal("§cStructure not found: " + structureId), true);
            return InteractionResult.FAIL;
        }
        
        // Nuovo sistema: controlla se è il secondo click entro 5 secondi nella stessa posizione
        long currentTime = level.getGameTime();
        long lastClickTime = getLastClickTime(stack);
        BlockPos lastClickPos = getLastClickPos(stack);
        
        boolean isSecondClick = (currentTime - lastClickTime <= 100) && // 5 secondi = 100 tick
                               pos.equals(lastClickPos);
        
        if (isSecondClick) {
            // Secondo click: tenta di piazzare la struttura
            return attemptStructurePlacement(serverPlayer, pos, structure, stack);
        } else {
            // Primo click: mostra preview con marker specifici
            showDetailedPreview(serverPlayer, pos, structure, stack);
            
            // Salva il tempo e la posizione del click
            saveLastClick(stack, currentTime, pos);
            
            return InteractionResult.SUCCESS;
        }
    }
    
    /**
     * Apre la GUI di selezione della struttura
     */
    private void openStructureSelectionGui(ServerPlayer player, ItemStack stack) {
        // Apre la GUI del Structure Placer
        player.openMenu(new net.minecraft.world.MenuProvider() {
            @Override
            public net.minecraft.network.chat.Component getDisplayName() {
                return Component.translatable("gui.iska_utils.structure_placer.title");
            }
            
            @Override
            public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int containerId, 
                    net.minecraft.world.entity.player.Inventory playerInventory, 
                    net.minecraft.world.entity.player.Player player) {
                return new net.unfamily.iskautils.client.gui.StructurePlacerMenu(containerId, playerInventory);
            }
        });
    }
    
    /**
     * Mostra l'anteprima dettagliata della struttura con marker azzurri per spazi vuoti e rossi per spazi occupati
     */
    private void showDetailedPreview(ServerPlayer player, BlockPos centerPos, StructureDefinition structure, ItemStack stack) {
        // Rimuovi i marker precedenti se esistono
        clearPreviousMarkers(player, stack);
        
        // Calcola le posizioni dei blocchi della struttura
        Map<BlockPos, String> blockPositions = calculateStructurePositions(centerPos, structure);
        
        if (blockPositions.isEmpty()) {
            player.displayClientMessage(Component.literal("§cError: No blocks to place in structure!"), true);
            return;
        }
        
        // Mostra marker specifici per ogni posizione
        int blueMarkers = 0;
        int redMarkers = 0;
        
        for (Map.Entry<BlockPos, String> entry : blockPositions.entrySet()) {
            BlockPos blockPos = entry.getKey();
            BlockState currentState = player.level().getBlockState(blockPos);
            
            if (canReplaceBlock(currentState, structure)) {
                // Spazio vuoto/sostituibile: marker azzurro
                MarkRenderer.getInstance().addBillboardMarker(blockPos, PREVIEW_COLOR, 100, // 5 secondi
                    "Empty space");
                blueMarkers++;
            } else {
                // Spazio occupato: marker rosso
                MarkRenderer.getInstance().addBillboardMarker(blockPos, CONFLICT_COLOR, 100, // 5 secondi
                    "Occupied: " + currentState.getBlock().getName().getString());
                redMarkers++;
            }
        }
        
        // Salva la posizione per riferimenti futuri
        saveLastPreviewPos(stack, centerPos);
        
        // Informa il giocatore
        String structureName = structure.getName() != null ? structure.getName() : structure.getId();
        player.displayClientMessage(Component.literal("§bPreview: §f" + structureName), true);
        player.displayClientMessage(Component.literal("§a" + blueMarkers + " §7empty spaces, §c" + redMarkers + " §7occupied spaces"), true);
        player.displayClientMessage(Component.literal("§7Click again within 5 seconds to place"), true);
        
        if (redMarkers > 0 && structure.isCanForce()) {
            player.displayClientMessage(Component.literal("§7Hold shift to force placement over occupied spaces"), true);
        }
    }
    
    /**
     * Tenta di piazzare la struttura controllando i materiali
     */
    private InteractionResult attemptStructurePlacement(ServerPlayer player, BlockPos centerPos, StructureDefinition structure, ItemStack stack) {
        // Controlla se il giocatore ha i materiali necessari
        Map<String, MaterialRequirement> requiredMaterials = calculateRequiredMaterials(centerPos, structure);
        Map<String, Integer> availableMaterials = scanPlayerInventory(player, requiredMaterials);
        
        // Verifica se ha tutti i materiali
        boolean hasAllMaterials = true;
        Map<String, Integer> missingMaterials = new HashMap<>();
        
        for (Map.Entry<String, MaterialRequirement> entry : requiredMaterials.entrySet()) {
            String displayName = entry.getKey();
            MaterialRequirement requirement = entry.getValue();
            int needed = requirement.getCount();
            int available = availableMaterials.getOrDefault(displayName, 0);
            
            if (available < needed) {
                hasAllMaterials = false;
                missingMaterials.put(displayName, needed - available);
            }
        }
        
        if (!hasAllMaterials) {
            int totalBlocks = requiredMaterials.values().stream().mapToInt(MaterialRequirement::getCount).sum();
            showMissingMaterialsMessage(player, missingMaterials, totalBlocks, requiredMaterials);
            return InteractionResult.FAIL;
        }
        
        // Controlla lo spazio
        Map<BlockPos, String> blockPositions = calculateStructurePositions(centerPos, structure);
        boolean hasConflicts = false;
        
        for (Map.Entry<BlockPos, String> entry : blockPositions.entrySet()) {
            BlockPos blockPos = entry.getKey();
            BlockState currentState = player.level().getBlockState(blockPos);
            
            if (!canReplaceBlock(currentState, structure)) {
                hasConflicts = true;
                break;
            }
        }
        
        // Se ci sono conflitti, mostra marker rossi
        if (hasConflicts) {
            if (!structure.isCanForce()) {
                showConflictMarkers(player, blockPositions, structure);
                player.displayClientMessage(Component.literal("§cSpace is occupied! Structure cannot be placed."), true);
                return InteractionResult.FAIL;
            } else {
                // Se può forzare, chiedi conferma
                if (!player.isShiftKeyDown()) {
                    showConflictMarkers(player, blockPositions, structure);
                    player.displayClientMessage(Component.literal("§cSpace is occupied! Use shift+right-click to force placement."), true);
                    return InteractionResult.FAIL;
                }
            }
        }
        
        // Rimuovi i materiali dall'inventario
        consumeMaterialsFromRequirements(player, requiredMaterials, availableMaterials);
        
        // Piazza la struttura
        boolean success = StructurePlacer.placeStructure((ServerLevel) player.level(), centerPos, structure, player);
        
        if (success) {
            // Mostra marker verdi di successo
            for (BlockPos blockPos : blockPositions.keySet()) {
                MarkRenderer.getInstance().addBillboardMarker(blockPos, SUCCESS_COLOR, 40, // 2 secondi
                    "Placed: " + structure.getId());
            }
            
            player.displayClientMessage(Component.literal("§aStructure §f" + structure.getId() + " §aplaced successfully!"), true);
            return InteractionResult.SUCCESS;
        } else {
            player.displayClientMessage(Component.literal("§cFailed to place structure!"), true);
            return InteractionResult.FAIL;
        }
    }
    
    /**
     * Calcola le posizioni di tutti i blocchi della struttura
     */
    private Map<BlockPos, String> calculateStructurePositions(BlockPos centerPos, StructureDefinition structure) {
        Map<BlockPos, String> positions = new HashMap<>();
        
        String[][][][] pattern = structure.getPattern();
        if (pattern == null) return positions;
        
        BlockPos relativeCenter = structure.findCenter();
        if (relativeCenter == null) relativeCenter = BlockPos.ZERO;
        
        BlockPos offsetPos = centerPos.subtract(relativeCenter);
        
        for (int y = 0; y < pattern.length; y++) {
            for (int x = 0; x < pattern[y].length; x++) {
                for (int z = 0; z < pattern[y][x].length; z++) {
                    String[] cellChars = pattern[y][x][z];
                    
                    if (cellChars != null) {
                        for (int charIndex = 0; charIndex < cellChars.length; charIndex++) {
                            String character = cellChars[charIndex];
                            
                            if (character != null && !character.equals(" ")) {
                                int worldX = x;
                                int worldY = y;
                                int worldZ = z * cellChars.length + charIndex;
                                
                                BlockPos blockPos = offsetPos.offset(worldX, worldY, worldZ);
                                positions.put(blockPos, character);
                            }
                        }
                    }
                }
            }
        }
        
        return positions;
    }
    
    /**
     * Calcola i materiali necessari per la struttura (raggruppati per display name)
     */
    private Map<String, MaterialRequirement> calculateRequiredMaterials(BlockPos centerPos, StructureDefinition structure) {
        Map<String, MaterialRequirement> materials = new HashMap<>();
        
        Map<BlockPos, String> positions = calculateStructurePositions(centerPos, structure);
        Map<String, List<StructureDefinition.BlockDefinition>> key = structure.getKey();
        
        for (String character : positions.values()) {
            List<StructureDefinition.BlockDefinition> blockDefs = key.get(character);
            if (blockDefs != null && !blockDefs.isEmpty()) {
                // Usa il display name o il primo block ID come chiave
                final String displayName = blockDefs.get(0).getDisplay() != null && !blockDefs.get(0).getDisplay().isEmpty() 
                    ? blockDefs.get(0).getDisplay() 
                    : blockDefs.get(0).getBlock();
                
                if (displayName != null) {
                    MaterialRequirement req = materials.computeIfAbsent(displayName, k -> new MaterialRequirement(displayName, blockDefs));
                    req.incrementCount();
                }
            }
        }
        
        return materials;
    }
    
    /**
     * Classe helper per raggruppare i requisiti di materiali con alternative
     */
    private static class MaterialRequirement {
        private final String displayName;
        private final List<StructureDefinition.BlockDefinition> alternatives;
        private int count = 0;
        
        public MaterialRequirement(String displayName, List<StructureDefinition.BlockDefinition> alternatives) {
            this.displayName = displayName;
            this.alternatives = alternatives;
        }
        
        public void incrementCount() {
            this.count++;
        }
        
        public String getDisplayName() { return displayName; }
        public List<StructureDefinition.BlockDefinition> getAlternatives() { return alternatives; }
        public int getCount() { return count; }
    }
    
    /**
     * Scansiona l'inventario del giocatore per i materiali disponibili
     */
    private Map<String, Integer> scanPlayerInventory(ServerPlayer player, Map<String, MaterialRequirement> requiredMaterials) {
        Map<String, Integer> available = new HashMap<>();
        
        // Scansiona inventario principale + hotbar
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem blockItem) {
                Block block = blockItem.getBlock();
                String blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).toString();
                
                // Controlla se questo blocco è una delle alternative richieste
                for (MaterialRequirement requirement : requiredMaterials.values()) {
                    for (StructureDefinition.BlockDefinition blockDef : requirement.getAlternatives()) {
                        if (blockDef.getBlock() != null && blockDef.getBlock().equals(blockId)) {
                            available.merge(requirement.getDisplayName(), stack.getCount(), Integer::sum);
                            break; // Esci dal loop interno una volta trovato
                        }
                    }
                }
            }
        }
        
        return available;
    }
    
    /**
     * Consuma i materiali dall'inventario del giocatore basandosi sui requisiti
     */
    private void consumeMaterialsFromRequirements(ServerPlayer player, Map<String, MaterialRequirement> requirements, Map<String, Integer> availableMaterials) {
        for (Map.Entry<String, MaterialRequirement> entry : requirements.entrySet()) {
            String displayName = entry.getKey();
            MaterialRequirement requirement = entry.getValue();
            int needed = requirement.getCount();
            int remaining = needed;
            
            // Cerca negli slot dell'inventario
            for (int i = 0; i < player.getInventory().items.size() && remaining > 0; i++) {
                ItemStack stack = player.getInventory().items.get(i);
                
                if (!stack.isEmpty() && stack.getItem() instanceof BlockItem blockItem) {
                    Block block = blockItem.getBlock();
                    String stackBlockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).toString();
                    
                    // Controlla se questo blocco è una delle alternative richieste
                    for (StructureDefinition.BlockDefinition blockDef : requirement.getAlternatives()) {
                        if (blockDef.getBlock() != null && blockDef.getBlock().equals(stackBlockId)) {
                            int toConsume = Math.min(remaining, stack.getCount());
                            stack.shrink(toConsume);
                            remaining -= toConsume;
                            
                            if (stack.isEmpty()) {
                                player.getInventory().items.set(i, ItemStack.EMPTY);
                            }
                            break; // Esci dal loop delle alternative
                        }
                    }
                }
            }
        }
        
        // Aggiorna l'inventario del giocatore
        player.getInventory().setChanged();
    }
    
    /**
     * Verifica se un blocco può essere rimpiazzato
     */
    private boolean canReplaceBlock(BlockState state, StructureDefinition structure) {
        // L'aria può sempre essere rimpiazzata
        if (state.isAir()) return true;
        
        // Controlla la lista can_replace della struttura
        List<String> canReplace = structure.getCanReplace();
        if (canReplace != null) {
            String blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
            return canReplace.contains(blockId);
        }
        
        return false;
    }
    
    /**
     * Mostra marker rossi per i conflitti
     */
    private void showConflictMarkers(ServerPlayer player, Map<BlockPos, String> positions, StructureDefinition structure) {
        for (Map.Entry<BlockPos, String> entry : positions.entrySet()) {
            BlockPos blockPos = entry.getKey();
            BlockState currentState = player.level().getBlockState(blockPos);
            
            if (!canReplaceBlock(currentState, structure)) {
                MarkRenderer.getInstance().addBillboardMarker(blockPos, CONFLICT_COLOR, 60, // 3 secondi
                    "Conflict: " + currentState.getBlock().getName().getString());
            }
        }
    }
    
    /**
     * Rimuove i marker precedenti
     */
    private void clearPreviousMarkers(ServerPlayer player, ItemStack stack) {
        // I marker scadono automaticamente, ma potremmo implementare una rimozione esplicita
        // se necessario in futuro
    }
    
    /**
     * Mostra un messaggio formattato dei materiali mancanti
     */
    private void showMissingMaterialsMessage(ServerPlayer player, Map<String, Integer> missingMaterials, int totalBlocks, Map<String, MaterialRequirement> requirements) {
        // Non mostra più "required_blocks" generico, ma i display name specifici
        
        for (Map.Entry<String, Integer> entry : missingMaterials.entrySet()) {
            String displayName = entry.getKey();
            int missing = entry.getValue();
            
            // Usa il display name tradotto se disponibile, altrimenti usa il display name grezzo
            String translatedDisplayName = Component.translatable(displayName).getString();
            if (translatedDisplayName.equals(displayName)) {
                // Se non c'è traduzione, usa il nome formattato
                translatedDisplayName = getTranslatedBlockName(displayName);
            }
            
            Component message = Component.literal("§c" + missing + " §f")
                .append(Component.literal(translatedDisplayName));
            
            player.displayClientMessage(message, false);
        }
        
        // Rimuove "total blocks needed" - ora ogni gruppo ha il suo nome specifico
        
        // Mostra le alternative disponibili
        for (Map.Entry<String, MaterialRequirement> reqEntry : requirements.entrySet()) {
            MaterialRequirement requirement = reqEntry.getValue();
            if (requirement.getAlternatives().size() > 1) {
                String translatedDisplayName = Component.translatable(requirement.getDisplayName()).getString();
                if (translatedDisplayName.equals(requirement.getDisplayName())) {
                    translatedDisplayName = getTranslatedBlockName(requirement.getDisplayName());
                }
                
                player.displayClientMessage(Component.literal("  §7- alternatives for §f" + translatedDisplayName + "§7:"), false);
                for (StructureDefinition.BlockDefinition blockDef : requirement.getAlternatives()) {
                    if (blockDef.getBlock() != null) {
                        String translatedName = getTranslatedBlockName(blockDef.getBlock());
                        // Controlla se il blocco esiste nel gioco
                        if (blockExists(blockDef.getBlock())) {
                            player.displayClientMessage(Component.literal("    §a- " + translatedName), false);
                        } else {
                            player.displayClientMessage(Component.literal("    §8- " + translatedName + " §7(not available)"), false);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Controlla se un blocco esiste nel registro
     */
    private boolean blockExists(String blockId) {
        try {
            net.minecraft.resources.ResourceLocation resourceLocation = net.minecraft.resources.ResourceLocation.parse(blockId);
            return net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(resourceLocation);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Ottiene il nome tradotto di un blocco dal suo ID
     */
    private String getTranslatedBlockName(String blockId) {
        try {
            // Converti l'ID del blocco (namespace:name) nel formato del modname
            String[] parts = blockId.split(":");
            if (parts.length == 2) {
                String namespace = parts[0];
                String blockName = parts[1];
                
                // Mappa alcuni namespace comuni ai nomi dei mod
                String modName = switch (namespace) {
                    case "minecraft" -> "Minecraft";
                    case "iska_utils" -> "Iskandert's Utilities";
                    case "industrialforegoing" -> "Industrial Foregoing";
                    default -> {
                        // Capitalizza il namespace come fallback
                        yield namespace.substring(0, 1).toUpperCase() + namespace.substring(1);
                    }
                };
                
                // Converte il nome del blocco in un formato leggibile
                String readableName = blockName.replace("_", " ");
                String[] words = readableName.split(" ");
                StringBuilder result = new StringBuilder();
                
                for (String word : words) {
                    if (result.length() > 0) result.append(" ");
                    result.append(word.substring(0, 1).toUpperCase()).append(word.substring(1));
                }
                
                return modName + ": " + result.toString();
            }
        } catch (Exception e) {
            // Se fallisce, usa l'ID originale
        }
        
        return blockId;
    }
    
    // ===== NBT Helper Methods =====
    
    public static String getSelectedStructure(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        return tag.getString(SELECTED_STRUCTURE_KEY);
    }
    
    public static void setSelectedStructure(ItemStack stack, String structureId) {
        CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        tag.putString(SELECTED_STRUCTURE_KEY, structureId);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
    }
    
    public static boolean isPreviewMode(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        return tag.getBoolean(PREVIEW_MODE_KEY);
    }
    
    public static void setPreviewMode(ItemStack stack, boolean previewMode) {
        CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        tag.putBoolean(PREVIEW_MODE_KEY, previewMode);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
    }
    
    private void saveLastPreviewPos(ItemStack stack, BlockPos pos) {
        CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        tag.putLong(LAST_PREVIEW_POS_KEY, pos.asLong());
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
    }
    
    private long getLastClickTime(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        return tag.getLong(LAST_CLICK_TIME_KEY);
    }
    
    private BlockPos getLastClickPos(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        long posLong = tag.getLong(LAST_CLICK_POS_KEY);
        if (posLong == 0) return null;
        return BlockPos.of(posLong);
    }
    
    private void saveLastClick(ItemStack stack, long time, BlockPos pos) {
        CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        tag.putLong(LAST_CLICK_TIME_KEY, time);
        tag.putLong(LAST_CLICK_POS_KEY, pos.asLong());
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String structureId = getSelectedStructure(stack);
        if (structureId != null && !structureId.isEmpty()) {
            StructureDefinition structure = StructureLoader.getStructure(structureId);
            if (structure != null) {
                tooltip.add(Component.literal("§bSelected: §f" + structure.getName()));
                tooltip.add(Component.literal("§7Right-click to select different structure"));
                tooltip.add(Component.literal("§7Left-click on block to place"));
                
                if (structure.isCanForce()) {
                    tooltip.add(Component.literal("§7Shift+click to force placement"));
                }
            } else {
                tooltip.add(Component.literal("§cInvalid structure: " + structureId));
            }
        } else {
            tooltip.add(Component.literal("§7Right-click to select structure"));
        }
        
        if (isPreviewMode(stack)) {
            tooltip.add(Component.literal("§aPreview mode enabled"));
        }
    }
} 
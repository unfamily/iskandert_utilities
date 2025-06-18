package net.unfamily.iskautils.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
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
    public static final String ROTATION_KEY = "rotation";
    
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
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        // Impedisce il danno alle entità quando si usa per la rotazione
        return true;
    }
    
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockPos pos = context.getClickedPos().above(); // Piazza un blocco sopra al blocco cliccato
        
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        
        String structureId = getSelectedStructure(stack);
        if (structureId == null || structureId.isEmpty()) {
            player.displayClientMessage(Component.translatable("item.iska_utils.structure_placer.no_structure_selected"), true);
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
        Map<BlockPos, String> blockPositions = calculateStructurePositions(centerPos, structure, stack);
        
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
                // Spazio vuoto/sostituibile: marker azzurro alla posizione del blocco (15 secondi, senza testo)
                MarkRenderer.getInstance().addBillboardMarker(blockPos, PREVIEW_COLOR, 300); // 15 secondi
                blueMarkers++;
            } else {
                // Spazio occupato: marker rosso alla posizione del blocco (15 secondi, senza testo)
                MarkRenderer.getInstance().addBillboardMarker(blockPos, CONFLICT_COLOR, 300); // 15 secondi
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
        // Calcola le posizioni e crea una mappa di allocazione specifica dei blocchi
        Map<BlockPos, String> blockPositions = calculateStructurePositions(centerPos, structure, stack);
        Map<String, List<StructureDefinition.BlockDefinition>> key = structure.getKey();
        
        // Crea una mappa di allocazione che traccia esattamente quale blocco usare per ogni posizione
        Map<BlockPos, AllocatedBlock> blockAllocation = new HashMap<>();
        Map<String, Integer> missingMaterials = new HashMap<>();
        
        // Traccia le alternative per ogni tipo di materiale
        Map<String, List<StructureDefinition.BlockDefinition>> alternativesMap = new HashMap<>();
        
        // Alloca i blocchi specifici dall'inventario
        for (Map.Entry<BlockPos, String> entry : blockPositions.entrySet()) {
            BlockPos blockPos = entry.getKey();
            String character = entry.getValue();
            List<StructureDefinition.BlockDefinition> blockDefs = key.get(character);
            
            if (blockDefs != null && !blockDefs.isEmpty()) {
                AllocatedBlock allocated = allocateBlockFromInventory(player, blockDefs);
                if (allocated != null) {
                    blockAllocation.put(blockPos, allocated);
                } else {
                    // Non trovato, aggiungi ai materiali mancanti
                    String displayName = blockDefs.get(0).getDisplay() != null && !blockDefs.get(0).getDisplay().isEmpty() 
                        ? blockDefs.get(0).getDisplay() 
                        : blockDefs.get(0).getBlock();
                    missingMaterials.merge(displayName, 1, Integer::sum);
                    
                    // Traccia le alternative disponibili per questo materiale
                    alternativesMap.put(displayName, blockDefs);
                }
            }
        }
        
        if (!missingMaterials.isEmpty()) {
            // Ripristina gli item allocati che non sono stati utilizzati
            restoreAllocatedBlocks(player, blockAllocation.values());
            showMissingMaterialsWithAlternatives(player, missingMaterials, alternativesMap);
            return InteractionResult.FAIL;
        }
        
        // Controlla lo spazio - controlla alla posizione effettiva, non un blocco sopra
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
                restoreAllocatedBlocks(player, blockAllocation.values());
                player.displayClientMessage(Component.literal("§cSpace is occupied! Structure cannot be placed."), true);
                return InteractionResult.FAIL;
            } else {
                // Se può forzare, chiedi conferma
                if (!player.isShiftKeyDown()) {
                    showConflictMarkers(player, blockPositions, structure);
                    restoreAllocatedBlocks(player, blockAllocation.values());
                    player.displayClientMessage(Component.literal("§cSpace is occupied! Use shift+right-click to force placement."), true);
                    return InteractionResult.FAIL;
                }
            }
        }
        
        // I materiali sono già stati consumati durante l'allocazione, ora piazza la struttura
        boolean isForced = hasConflicts && structure.isCanForce() && player.isShiftKeyDown();
        boolean success = placeStructureWithAllocatedBlocks((ServerLevel) player.level(), blockAllocation, structure, player, isForced);
        
        if (success) {
            // Mostra marker verdi di successo UN BLOCCO PIÙ IN ALTO solo per i blocchi effettivamente piazzati
            showSuccessMarkers((ServerLevel) player.level(), blockAllocation, structure, isForced);
            
            String structureName = structure.getName() != null ? structure.getName() : structure.getId();
            if (isForced) {
                player.displayClientMessage(Component.literal("§aStructure §f" + structureName + " §apartially placed! (Some blocks skipped)"), true);
            } else {
                player.displayClientMessage(Component.literal("§aStructure §f" + structureName + " §aplaced successfully!"), true);
            }
            return InteractionResult.SUCCESS;
        } else {
            // Se il piazzamento fallisce, ripristina i blocchi
            restoreAllocatedBlocks(player, blockAllocation.values());
            player.displayClientMessage(Component.literal("§cFailed to place structure!"), true);
            return InteractionResult.FAIL;
        }
    }
    
    /**
     * Calcola le posizioni di tutti i blocchi della struttura con rotazione attorno al centro @
     */
    private Map<BlockPos, String> calculateStructurePositions(BlockPos centerPos, StructureDefinition structure, ItemStack stack) {
        Map<BlockPos, String> positions = new HashMap<>();
        
        String[][][][] pattern = structure.getPattern();
        if (pattern == null) return positions;
        
        // Trova il centro della struttura (simbolo @)
        BlockPos relativeCenter = structure.findCenter();
        if (relativeCenter == null) relativeCenter = BlockPos.ZERO;
        
        // Ottieni la rotazione dall'ItemStack
        int rotation = getRotation(stack);
        
        for (int y = 0; y < pattern.length; y++) {
            for (int x = 0; x < pattern[y].length; x++) {
                for (int z = 0; z < pattern[y][x].length; z++) {
                    String[] cellChars = pattern[y][x][z];
                    
                    if (cellChars != null) {
                        for (int charIndex = 0; charIndex < cellChars.length; charIndex++) {
                            String character = cellChars[charIndex];
                            
                            // Salta spazi vuoti
                            if (character == null || character.equals(" ")) continue;
                            
                            // Se è @, controllare se è definito nella key
                            if (character.equals("@")) {
                                Map<String, List<StructureDefinition.BlockDefinition>> key = structure.getKey();
                                if (key == null || !key.containsKey("@")) {
                                    // @ non è definito nella key, trattalo come spazio vuoto
                                    continue;
                                }
                                // Se arriviamo qui, @ è definito nella key, quindi processalo come un blocco normale
                            }
                                int originalX = x;
                                int originalY = y;
                                int originalZ = z * cellChars.length + charIndex;
                                
                                // Calcola posizione relativa rispetto al centro @
                                int relX = originalX - relativeCenter.getX();
                                int relY = originalY - relativeCenter.getY();
                                int relZ = originalZ - relativeCenter.getZ();
                                
                                // Applica la rotazione alle coordinate relative
                                BlockPos rotatedRelativePos = applyRotation(relX, relY, relZ, rotation);
                                
                                // Calcola la posizione finale nel mondo
                                BlockPos blockPos = centerPos.offset(
                                    rotatedRelativePos.getX(), 
                                    rotatedRelativePos.getY(), 
                                    rotatedRelativePos.getZ()
                                );
                                
                                positions.put(blockPos, character);
                        }
                    }
                }
            }
        }
        
        return positions;
    }
    
    /**
     * Applica la rotazione alle coordinate relative
     */
    private BlockPos applyRotation(int x, int y, int z, int rotation) {
        // Rotazione solo orizzontale (X e Z), Y rimane uguale
        int newX = x;
        int newZ = z;
        
        switch (rotation) {
            case 0:   // Nord (nessuna rotazione)
                newX = x;
                newZ = z;
                break;
            case 90:  // Est (90° in senso orario)
                newX = -z;
                newZ = x;
                break;
            case 180: // Sud (180°)
                newX = -x;
                newZ = -z;
                break;
            case 270: // Ovest (270° in senso orario = 90° antiorario)
                newX = z;
                newZ = -x;
                break;
        }
        
        return new BlockPos(newX, y, newZ);
    }
    
    /**
     * Calcola i materiali necessari per la struttura (raggruppati per display name)
     */
    private Map<String, MaterialRequirement> calculateRequiredMaterials(BlockPos centerPos, StructureDefinition structure) {
        Map<String, MaterialRequirement> materials = new HashMap<>();
        
        Map<BlockPos, String> positions = calculateStructurePositions(centerPos, structure, ItemStack.EMPTY);
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
     * Classe per tracciare un blocco allocato dall'inventario
     */
    private static class AllocatedBlock {
        private final StructureDefinition.BlockDefinition blockDefinition;
        private final int inventorySlot;
        private final ItemStack originalStack;
        private final ItemStack consumedStack;
        
        public AllocatedBlock(StructureDefinition.BlockDefinition blockDefinition, int inventorySlot, 
                            ItemStack originalStack, ItemStack consumedStack) {
            this.blockDefinition = blockDefinition;
            this.inventorySlot = inventorySlot;
            this.originalStack = originalStack;
            this.consumedStack = consumedStack;
        }
        
        public StructureDefinition.BlockDefinition getBlockDefinition() { return blockDefinition; }
        public int getInventorySlot() { return inventorySlot; }
        public ItemStack getOriginalStack() { return originalStack; }
        public ItemStack getConsumedStack() { return consumedStack; }
    }
    
    /**
     * Alloca un singolo blocco dall'inventario del giocatore
     */
    private AllocatedBlock allocateBlockFromInventory(ServerPlayer player, List<StructureDefinition.BlockDefinition> blockDefinitions) {
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            ItemStack stack = player.getInventory().items.get(i);
            
            if (!stack.isEmpty()) {
                // Ottieni il blocco dall'item se possibile
                Block block = null;
                String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                
                if (stack.getItem() instanceof BlockItem blockItem) {
                    block = blockItem.getBlock();
                } else {
                    // Prova a trovare un blocco corrispondente per item che non sono BlockItem
                    try {
                        net.minecraft.resources.ResourceLocation itemLocation = net.minecraft.resources.ResourceLocation.parse(itemId);
                        net.minecraft.resources.ResourceLocation blockLocation = 
                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(itemLocation.getNamespace(), itemLocation.getPath());
                        
                        if (net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(blockLocation)) {
                            block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(blockLocation);
                        }
                    } catch (Exception e) {
                        // Ignora se non riesce a trovare il blocco corrispondente
                    }
                }
                
                if (block != null) {
                    String blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).toString();
                    
                    // Controlla se questo blocco corrisponde a una delle definizioni richieste
                    for (StructureDefinition.BlockDefinition blockDef : blockDefinitions) {
                        if (blockDef.getBlock() != null && blockDef.getBlock().equals(blockId)) {
                            // Trovato! Consuma un item e crea l'allocazione
                            ItemStack consumedStack = stack.copy();
                            consumedStack.setCount(1);
                            
                            stack.shrink(1);
                            if (stack.isEmpty()) {
                                player.getInventory().items.set(i, ItemStack.EMPTY);
                            }
                            
                            player.getInventory().setChanged();
                            return new AllocatedBlock(blockDef, i, stack.copy(), consumedStack);
                        }
                    }
                }
            }
        }
        
        return null; // Non trovato
    }
    
    /**
     * Ripristina i blocchi allocati nell'inventario del giocatore
     */
    private void restoreAllocatedBlocks(ServerPlayer player, java.util.Collection<AllocatedBlock> allocatedBlocks) {
        for (AllocatedBlock allocated : allocatedBlocks) {
            ItemStack consumedStack = allocated.getConsumedStack();
            
            // Prova a rimettere l'item nello slot originale
            ItemStack currentStack = player.getInventory().items.get(allocated.getInventorySlot());
            if (currentStack.isEmpty()) {
                player.getInventory().items.set(allocated.getInventorySlot(), consumedStack.copy());
            } else if (ItemStack.isSameItemSameComponents(currentStack, consumedStack)) {
                currentStack.grow(consumedStack.getCount());
            } else {
                // Se lo slot originale è occupato, trova un altro slot
                if (!player.getInventory().add(consumedStack.copy())) {
                    // Se l'inventario è pieno, droppa l'item
                    player.drop(consumedStack.copy(), false);
                }
            }
        }
        
        player.getInventory().setChanged();
    }
    
    /**
     * Piazza la struttura usando i blocchi allocati specificamente con delay tra i layer
     */
    private boolean placeStructureWithAllocatedBlocks(ServerLevel level, Map<BlockPos, AllocatedBlock> blockAllocation, 
                                                    StructureDefinition structure, ServerPlayer player, boolean isForced) {
        try {
            // Raggruppa i blocchi per layer (Y coordinate)
            Map<Integer, Map<BlockPos, AllocatedBlock>> blocksByLayer = new HashMap<>();
            
            for (Map.Entry<BlockPos, AllocatedBlock> entry : blockAllocation.entrySet()) {
                BlockPos pos = entry.getKey();
                int layer = pos.getY();
                blocksByLayer.computeIfAbsent(layer, k -> new HashMap<>()).put(pos, entry.getValue());
            }
            
            // Piazza il primo layer immediatamente
            Map<Integer, Boolean> layerResults = new HashMap<>();
            List<Integer> sortedLayers = blocksByLayer.keySet().stream().sorted().toList();
            
            if (!sortedLayers.isEmpty()) {
                int firstLayer = sortedLayers.get(0);
                boolean firstLayerSuccess = placeLayer(level, blocksByLayer.get(firstLayer), structure, player, isForced);
                layerResults.put(firstLayer, firstLayerSuccess);
                
                // Schedula i layer rimanenti con delay di 20 tick tra ognuno
                for (int i = 1; i < sortedLayers.size(); i++) {
                    final int layerY = sortedLayers.get(i);
                    final int delayTicks = i * 20; // 20 tick per ogni layer dopo il primo
                    final Map<BlockPos, AllocatedBlock> layerBlocks = blocksByLayer.get(layerY);
                    
                    // Schedula il piazzamento del layer dopo il delay
                    new Thread(() -> {
                        try {
                            Thread.sleep(delayTicks * 50); // Converti tick in millisecondi
                            level.getServer().execute(() -> {
                                boolean layerSuccess = placeLayer(level, layerBlocks, structure, player, isForced);
                                layerResults.put(layerY, layerSuccess);
                            });
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }).start();
                }
            }
            
            // Ritorna true se almeno il primo layer è stato piazzato con successo
            return !layerResults.isEmpty() && layerResults.values().stream().anyMatch(success -> success);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Piazza un singolo layer di blocchi
     */
    private boolean placeLayer(ServerLevel level, Map<BlockPos, AllocatedBlock> layerBlocks, 
                             StructureDefinition structure, ServerPlayer player, boolean isForced) {
        try {
            int placedBlocks = 0;
            Map<BlockPos, AllocatedBlock> skippedBlocks = new HashMap<>();
            
            for (Map.Entry<BlockPos, AllocatedBlock> entry : layerBlocks.entrySet()) {
                BlockPos pos = entry.getKey();
                AllocatedBlock allocated = entry.getValue();
                StructureDefinition.BlockDefinition blockDef = allocated.getBlockDefinition();
                
                // Se è in modalità forzata, controlla se la posizione può essere sostituita
                if (isForced) {
                    BlockState currentState = level.getBlockState(pos);
                    if (!canReplaceBlock(currentState, structure)) {
                        // Salta questa posizione e tieni da parte il blocco per ripristinarlo
                        skippedBlocks.put(pos, allocated);
                        continue;
                    }
                }
                
                // Ottieni il blocco dal registro
                net.minecraft.resources.ResourceLocation blockLocation = 
                    net.minecraft.resources.ResourceLocation.parse(blockDef.getBlock());
                Block block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(blockLocation);
                
                if (block != null && block != Blocks.AIR) {
                    BlockState blockState = block.defaultBlockState();
                    
                    // Applica le proprietà se specificate
                    if (blockDef.getProperties() != null) {
                        for (Map.Entry<String, String> propEntry : blockDef.getProperties().entrySet()) {
                            try {
                                blockState = applyBlockProperty(blockState, propEntry.getKey(), propEntry.getValue());
                            } catch (Exception e) {
                                // Ignora proprietà non valide
                            }
                        }
                    }
                    
                    // Piazza il blocco
                    level.setBlock(pos, blockState, 3);
                    placedBlocks++;
                } else {
                    // Blocco non valido - tieni da parte per ripristino
                    skippedBlocks.put(pos, allocated);
                }
            }
            
            // Se in modalità forzata e ci sono blocchi saltati, ripristinali nell'inventario
            if (isForced && !skippedBlocks.isEmpty()) {
                restoreAllocatedBlocks(player, skippedBlocks.values());
            }
            
            // Successo se almeno un blocco è stato piazzato
            return placedBlocks > 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Applica una proprietà a un BlockState
     */
    private BlockState applyBlockProperty(BlockState state, String propertyName, String value) {
        for (net.minecraft.world.level.block.state.properties.Property<?> property : state.getProperties()) {
            if (property.getName().equals(propertyName)) {
                return applyPropertyValue(state, property, value);
            }
        }
        return state; // Proprietà non trovata, ritorna lo stato originale
    }
    
    /**
     * Applica il valore di una proprietà specifica
     */
    @SuppressWarnings("unchecked")
    private <T extends Comparable<T>> BlockState applyPropertyValue(BlockState state, 
                                                                   net.minecraft.world.level.block.state.properties.Property<T> property, 
                                                                   String value) {
        try {
            java.util.Optional<T> optionalValue = property.getValue(value);
            if (optionalValue.isPresent()) {
                return state.setValue(property, optionalValue.get());
            }
        } catch (Exception e) {
            // Ignora valori non validi
        }
        return state;
    }

    /**
     * Scansiona l'inventario del giocatore per i materiali disponibili
     */
    private Map<String, Integer> scanPlayerInventory(ServerPlayer player, Map<String, MaterialRequirement> requiredMaterials) {
        Map<String, Integer> available = new HashMap<>();
        
        // Scansiona inventario principale + hotbar
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty()) {
                // Ottieni il blocco dall'item se possibile
                Block block = null;
                String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                
                if (stack.getItem() instanceof BlockItem blockItem) {
                    block = blockItem.getBlock();
                } else {
                    // Prova a trovare un blocco corrispondente per item che non sono BlockItem
                    // ma che corrispondono comunque a un blocco (come lever, button, etc.)
                    try {
                        net.minecraft.resources.ResourceLocation itemLocation = net.minecraft.resources.ResourceLocation.parse(itemId);
                        net.minecraft.resources.ResourceLocation blockLocation = 
                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(itemLocation.getNamespace(), itemLocation.getPath());
                        
                        if (net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(blockLocation)) {
                            block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(blockLocation);
                        }
                    } catch (Exception e) {
                        // Ignora se non riesce a trovare il blocco corrispondente
                    }
                }
                
                if (block != null) {
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
                
                if (!stack.isEmpty()) {
                    // Ottieni il blocco dall'item se possibile
                    Block block = null;
                    String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                    
                    if (stack.getItem() instanceof BlockItem blockItem) {
                        block = blockItem.getBlock();
                    } else {
                        // Prova a trovare un blocco corrispondente per item che non sono BlockItem
                        try {
                            net.minecraft.resources.ResourceLocation itemLocation = net.minecraft.resources.ResourceLocation.parse(itemId);
                            net.minecraft.resources.ResourceLocation blockLocation = 
                                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(itemLocation.getNamespace(), itemLocation.getPath());
                            
                            if (net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(blockLocation)) {
                                block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(blockLocation);
                            }
                        } catch (Exception e) {
                            // Ignora se non riesce a trovare il blocco corrispondente
                        }
                    }
                    
                    if (block != null) {
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
                // Marker alla posizione del blocco (15 secondi, senza testo)
                MarkRenderer.getInstance().addBillboardMarker(blockPos, CONFLICT_COLOR, 300); // 15 secondi
            }
        }
    }
    
    /**
     * Mostra marker verdi di successo solo per i blocchi effettivamente piazzati
     */
    private void showSuccessMarkers(ServerLevel level, Map<BlockPos, AllocatedBlock> blockAllocation, 
                                  StructureDefinition structure, boolean isForced) {
        for (Map.Entry<BlockPos, AllocatedBlock> entry : blockAllocation.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState currentState = level.getBlockState(pos);
            
            // Se è in modalità forzata, mostra marker solo per blocchi che sono stati effettivamente piazzati
            boolean wasPlaced = true;
            if (isForced) {
                // Controlla se alla posizione c'è ancora il blocco originale (non piazzato)
                // oppure se c'è il blocco che dovevamo piazzare (piazzato)
                AllocatedBlock allocated = entry.getValue();
                try {
                    net.minecraft.resources.ResourceLocation blockLocation = 
                        net.minecraft.resources.ResourceLocation.parse(allocated.getBlockDefinition().getBlock());
                    Block expectedBlock = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(blockLocation);
                    
                    // Se il blocco corrente non è quello che dovevamo piazzare, significa che è stato saltato
                    wasPlaced = currentState.getBlock() == expectedBlock;
                } catch (Exception e) {
                    wasPlaced = false;
                }
            }
            
            if (wasPlaced) {
                // Marker di successo (15 secondi, senza testo)
                MarkRenderer.getInstance().addBillboardMarker(pos, SUCCESS_COLOR, 300); // 15 secondi
            }
        }
    }
    
    /**
     * Mostra un messaggio semplificato dei materiali mancanti con alternative specifiche
     */
    private void showMissingMaterialsMessageSimple(ServerPlayer player, Map<String, Integer> missingMaterials, int totalBlocks) {
        player.displayClientMessage(Component.literal("§cMissing materials:"), false);
        
        // Per ogni materiale mancante, trova le alternative dal sistema di allocazione
        // Non usiamo più calculateStructurePositions qui poiché serve solo per i display names
        
        for (Map.Entry<String, Integer> entry : missingMaterials.entrySet()) {
            String displayName = entry.getKey();
            int missing = entry.getValue();
            
            // Usa il display name tradotto se disponibile, altrimenti usa il nome formattato
            String translatedDisplayName = Component.translatable(displayName).getString();
            if (translatedDisplayName.equals(displayName)) {
                // Se non c'è traduzione, usa il nome formattato
                translatedDisplayName = getFormattedDisplayName(displayName);
            }
            
            Component message = Component.literal("§c- " + missing + " §f")
                .append(Component.literal(translatedDisplayName));
            
            player.displayClientMessage(message, false);
        }
    }
    
    /**
     * Mostra i materiali mancanti con tutte le alternative disponibili
     */
    private void showMissingMaterialsWithAlternatives(ServerPlayer player, Map<String, Integer> missingMaterials, 
                                                     Map<String, List<StructureDefinition.BlockDefinition>> alternativesMap) {
        player.displayClientMessage(Component.literal("§cMissing materials:"), false);
        
        for (Map.Entry<String, Integer> entry : missingMaterials.entrySet()) {
            String displayName = entry.getKey();
            int missing = entry.getValue();
            
            // Mostra il nome principale del gruppo
            String translatedDisplayName = getFormattedDisplayName(displayName);
            Component message = Component.literal("§c- " + missing + " §f" + translatedDisplayName + ":");
            player.displayClientMessage(message, false);
            
            // Mostra tutte le alternative disponibili
            List<StructureDefinition.BlockDefinition> alternatives = alternativesMap.get(displayName);
            if (alternatives != null) {
                for (StructureDefinition.BlockDefinition blockDef : alternatives) {
                    if (blockDef.getBlock() != null && blockExists(blockDef.getBlock())) {
                        String formattedBlockName = getFormattedBlockName(blockDef.getBlock());
                        player.displayClientMessage(Component.literal("  §a- " + formattedBlockName), false);
                    }
                }
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
        return getFormattedBlockName(blockId);
    }
    
    /**
     * Formatta il nome di un blocco nel formato "Mod Name: Block Name"
     */
    private String getFormattedBlockName(String blockId) {
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
                    case "mob_grinding_utils" -> "Mob Grinding Utils";
                    case "thermal" -> "Thermal Series";
                    case "mekanism" -> "Mekanism";
                    case "create" -> "Create";
                    case "ae2" -> "Applied Energistics 2";
                    case "botania" -> "Botania";
                    case "tconstruct" -> "Tinkers' Construct";
                    default -> {
                        // Capitalizza il namespace come fallback
                        yield formatModName(namespace);
                    }
                };
                
                // Converte il nome del blocco in un formato leggibile
                String readableName = formatBlockName(blockName);
                
                return modName + ": " + readableName;
            }
        } catch (Exception e) {
            // Se fallisce, usa l'ID originale
        }
        
        return blockId;
    }
    
    /**
     * Formatta il nome di un display name nel formato corretto
     */
    private String getFormattedDisplayName(String displayName) {
        // Se è già un translation key, prova a tradurlo
        if (displayName.contains(".")) {
            String translated = Component.translatable(displayName).getString();
            if (!translated.equals(displayName)) {
                return translated;
            }
        }
        
        // Altrimenti formatta come un block ID
        return getFormattedBlockName(displayName);
    }
    
    /**
     * Formatta il nome di una mod dal namespace
     */
    private String formatModName(String namespace) {
        if (namespace.length() <= 1) return namespace.toUpperCase();
        
        // Dividi per underscore e capitalizza ogni parola
        String[] parts = namespace.split("_");
        StringBuilder result = new StringBuilder();
        
        for (String part : parts) {
            if (result.length() > 0) result.append(" ");
            if (part.length() > 0) {
                result.append(part.substring(0, 1).toUpperCase()).append(part.substring(1));
            }
        }
        
        return result.toString();
    }
    
    /**
     * Formatta il nome di un blocco dal suo ID interno
     */
    private String formatBlockName(String blockName) {
        String readableName = blockName.replace("_", " ");
        String[] words = readableName.split(" ");
        StringBuilder result = new StringBuilder();
        
        for (String word : words) {
            if (result.length() > 0) result.append(" ");
            if (word.length() > 0) {
                result.append(word.substring(0, 1).toUpperCase()).append(word.substring(1));
            }
        }
        
        return result.toString();
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
    
    public static int getRotation(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        return tag.getInt(ROTATION_KEY);
    }
    
    public static void setRotation(ItemStack stack, int rotation) {
        CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        tag.putInt(ROTATION_KEY, rotation);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String structureId = getSelectedStructure(stack);
        if (structureId != null && !structureId.isEmpty()) {
            StructureDefinition structure = StructureLoader.getStructure(structureId);
            if (structure != null) {
                tooltip.add(Component.literal("§bSelected: §f" + structure.getName()));
                
                // Mostra rotazione corrente
                int rotation = getRotation(stack);
                String rotationText = switch (rotation) {
                    case 0 -> Component.translatable("direction.iska_utils.north").getString();
                    case 90 -> Component.translatable("direction.iska_utils.east").getString(); 
                    case 180 -> Component.translatable("direction.iska_utils.south").getString();
                    case 270 -> Component.translatable("direction.iska_utils.west").getString();
                    default -> String.valueOf(rotation) + "°";
                };
                tooltip.add(Component.translatable("item.iska_utils.structure_placer.tooltip.rotation", rotationText));
                
                tooltip.add(Component.literal(""));
                tooltip.add(Component.translatable("item.iska_utils.structure_placer.tooltip.right_click_block"));
                tooltip.add(Component.translatable("item.iska_utils.structure_placer.tooltip.right_click_air"));
                tooltip.add(Component.translatable("item.iska_utils.structure_placer.tooltip.left_click_rotate"));
                
                if (structure.isCanForce()) {
                    tooltip.add(Component.translatable("item.iska_utils.structure_placer.tooltip.shift_force"));
                }
            } else {
                tooltip.add(Component.literal("§cInvalid structure: " + structureId));
            }
        } else {
            tooltip.add(Component.translatable("item.iska_utils.structure_placer.tooltip.no_selection"));
        }
        
        if (isPreviewMode(stack)) {
            tooltip.add(Component.translatable("item.iska_utils.structure_placer.tooltip.preview_enabled"));
        }
    }
} 
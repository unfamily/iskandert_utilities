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
        BlockPos pos = context.getClickedPos().above(); // Piazza sopra il blocco cliccato
        
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
        
        // Controlla se è in modalità preview
        if (isPreviewMode(stack)) {
            // Mostra preview della struttura
            showStructurePreview(serverPlayer, pos, structure, stack);
            return InteractionResult.SUCCESS;
        } else {
            // Prova a piazzare la struttura
            return attemptStructurePlacement(serverPlayer, pos, structure, stack);
        }
    }
    
    /**
     * Apre la GUI di selezione della struttura
     */
    private void openStructureSelectionGui(ServerPlayer player, ItemStack stack) {
        // Usa il sistema testuale per la selezione struttura
        StructurePlacerGuiOpenC2SPacket packet = new StructurePlacerGuiOpenC2SPacket();
        packet.handle(player);
    }
    
    /**
     * Mostra l'anteprima della struttura con marker azzurri
     */
    private void showStructurePreview(ServerPlayer player, BlockPos centerPos, StructureDefinition structure, ItemStack stack) {
        // Rimuovi i marker precedenti se esistono
        clearPreviousMarkers(player, stack);
        
        // Calcola le posizioni dei blocchi della struttura
        Map<BlockPos, String> blockPositions = calculateStructurePositions(centerPos, structure);
        
        // Controlla i conflitti
        boolean hasConflicts = false;
        for (Map.Entry<BlockPos, String> entry : blockPositions.entrySet()) {
            BlockPos blockPos = entry.getKey();
            BlockState currentState = player.level().getBlockState(blockPos);
            
            if (!canReplaceBlock(currentState, structure)) {
                hasConflicts = true;
                break;
            }
        }
        
        // Scegli il colore in base ai conflitti
        int markerColor = hasConflicts ? CONFLICT_COLOR : PREVIEW_COLOR;
        
        // Mostra i marker per ogni posizione
        for (BlockPos blockPos : blockPositions.keySet()) {
            // Usa billboard marker per 3 secondi
            MarkRenderer.getInstance().addBillboardMarker(blockPos, markerColor, 60, // 3 secondi
                "Structure: " + structure.getId());
        }
        
        // Salva la posizione per riferimenti futuri
        saveLastPreviewPos(stack, centerPos);
        
        // Informa il giocatore
        String conflictMsg = hasConflicts ? " §c(Conflicts detected!)" : " §a(Ready to place)";
        player.displayClientMessage(Component.literal("§bPreview: §f" + structure.getId() + conflictMsg), true);
        
        if (hasConflicts && structure.isCanForce()) {
            player.displayClientMessage(Component.literal("§7Use shift+right-click to force placement"), true);
        }
    }
    
    /**
     * Tenta di piazzare la struttura controllando i materiali
     */
    private InteractionResult attemptStructurePlacement(ServerPlayer player, BlockPos centerPos, StructureDefinition structure, ItemStack stack) {
        // Controlla se il giocatore ha i materiali necessari
        Map<String, Integer> requiredMaterials = calculateRequiredMaterials(centerPos, structure);
        Map<String, Integer> availableMaterials = scanPlayerInventory(player, requiredMaterials);
        
        // Verifica se ha tutti i materiali
        boolean hasAllMaterials = true;
        StringBuilder missingMsg = new StringBuilder("§cMissing materials: ");
        for (Map.Entry<String, Integer> entry : requiredMaterials.entrySet()) {
            String blockId = entry.getKey();
            int needed = entry.getValue();
            int available = availableMaterials.getOrDefault(blockId, 0);
            
            if (available < needed) {
                hasAllMaterials = false;
                missingMsg.append(blockId).append(" (").append(needed - available).append("), ");
            }
        }
        
        if (!hasAllMaterials) {
            player.displayClientMessage(Component.literal(missingMsg.toString()), false);
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
        consumeMaterials(player, requiredMaterials);
        
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
        
        BlockPos offsetPos = centerPos.subtract(relativeCenter).above();
        
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
     * Calcola i materiali necessari per la struttura
     */
    private Map<String, Integer> calculateRequiredMaterials(BlockPos centerPos, StructureDefinition structure) {
        Map<String, Integer> materials = new HashMap<>();
        
        Map<BlockPos, String> positions = calculateStructurePositions(centerPos, structure);
        Map<String, List<StructureDefinition.BlockDefinition>> key = structure.getKey();
        
        for (String character : positions.values()) {
            List<StructureDefinition.BlockDefinition> blockDefs = key.get(character);
            if (blockDefs != null && !blockDefs.isEmpty()) {
                StructureDefinition.BlockDefinition blockDef = blockDefs.get(0);
                if (blockDef.getBlock() != null) {
                    materials.merge(blockDef.getBlock(), 1, Integer::sum);
                }
            }
        }
        
        return materials;
    }
    
    /**
     * Scansiona l'inventario del giocatore per i materiali disponibili
     */
    private Map<String, Integer> scanPlayerInventory(ServerPlayer player, Map<String, Integer> requiredMaterials) {
        Map<String, Integer> available = new HashMap<>();
        
        // Scansiona inventario principale + hotbar + armor
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem) {
                // TODO: Implementare la conversione da ItemStack a Block ID
                // Per ora placeholder
            }
        }
        
        return available;
    }
    
    /**
     * Consuma i materiali dall'inventario del giocatore
     */
    private void consumeMaterials(ServerPlayer player, Map<String, Integer> materials) {
        // TODO: Implementare il consumo dei materiali
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
            String blockId = state.getBlock().toString(); // TODO: Convertire correttamente
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
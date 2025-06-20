package net.unfamily.iskautils.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.unfamily.iskautils.client.MarkRenderer;

import java.util.List;

/**
 * Blueprint item per salvare due coordinate che rappresentano i vertici di un parallelepipedo
 */
public class BlueprintItem extends Item {
    
    // NBT Keys per le coordinate
    private static final String VERTEX1_X = "Vertex1X";
    private static final String VERTEX1_Y = "Vertex1Y";
    private static final String VERTEX1_Z = "Vertex1Z";
    private static final String VERTEX2_X = "Vertex2X";
    private static final String VERTEX2_Y = "Vertex2Y";
    private static final String VERTEX2_Z = "Vertex2Z";
    private static final String CENTER_X = "CenterX";
    private static final String CENTER_Y = "CenterY";
    private static final String CENTER_Z = "CenterZ";
    
    // Colori per i marker
    private static final int VERTEX1_COLOR = 0x8000FF00; // Verde semi-trasparente
    private static final int VERTEX2_COLOR = 0x80FF0000; // Rosso semi-trasparente
    private static final int CENTER_COLOR = 0x80FFFF00;  // Giallo semi-trasparente per il centro
    private static final int AREA_COLOR = 0x800080FF;    // Blu semi-trasparente
    
    public BlueprintItem(Properties properties) {
        super(properties);
    }
    
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();
        
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        
        // Controlla se shift+destro su Structure Saver Machine con blueprint completa
        System.out.println("DEBUG BLUEPRINT CLICK: isShiftKeyDown = " + player.isShiftKeyDown());
        System.out.println("DEBUG BLUEPRINT CLICK: hasCompleteBlueprint = " + hasCompleteBlueprint(stack));
        System.out.println("DEBUG BLUEPRINT CLICK: blockState = " + level.getBlockState(clickedPos));
        
        if (player.isShiftKeyDown() && hasCompleteBlueprint(stack)) {
            var blockState = level.getBlockState(clickedPos);
            System.out.println("DEBUG BLUEPRINT CLICK: Checking block type: " + blockState.getBlock().getClass().getName());
            
            if (blockState.getBlock() instanceof net.unfamily.iskautils.block.StructureSaverMachineBlock) {
                System.out.println("DEBUG BLUEPRINT CLICK: Detected Structure Saver Machine!");
                // Importa le coordinate nella Structure Saver Machine
                var vertex1 = getVertex1FromStack(stack);
                var vertex2 = getVertex2FromStack(stack);
                var center = getCenterFromStack(stack);
                
                if (vertex1 != null && vertex2 != null && center != null) {
                    // Calcola le dimensioni e verifica validità
                    int[] dimensions = calculateDimensions(vertex1, vertex2);
                    
                    // Verifica che nessuna dimensione superi 64 blocchi
                    if (dimensions[0] > 64 || dimensions[1] > 64 || dimensions[2] > 64) {
                        serverPlayer.displayClientMessage(
                            Component.translatable("gui.iska_utils.blueprint_invalid_size", 
                                dimensions[0], dimensions[1], dimensions[2]), 
                            true
                        );
                        return InteractionResult.SUCCESS;
                    }
                    
                    // Salva le coordinate nel BlockEntity
                    var entity = level.getBlockEntity(clickedPos);
                    if (entity instanceof net.unfamily.iskautils.block.entity.StructureSaverMachineBlockEntity structureSaverEntity) {
                        System.out.println("DEBUG BLUEPRINT: Transferring data to machine");
                        System.out.println("DEBUG BLUEPRINT: vertex1 = " + vertex1);
                        System.out.println("DEBUG BLUEPRINT: vertex2 = " + vertex2);
                        System.out.println("DEBUG BLUEPRINT: center = " + center);
                        
                        structureSaverEntity.setBlueprintData(vertex1, vertex2, center);
                        
                        // Verifica che i dati siano stati salvati
                        System.out.println("DEBUG BLUEPRINT: After save:");
                        System.out.println("DEBUG BLUEPRINT: hasValidArea = " + structureSaverEntity.hasValidArea());
                        System.out.println("DEBUG BLUEPRINT: stored vertex1 = " + structureSaverEntity.getBlueprintVertex1());
                        System.out.println("DEBUG BLUEPRINT: stored vertex2 = " + structureSaverEntity.getBlueprintVertex2());
                        
                        // Mostra messaggio di successo
                        serverPlayer.displayClientMessage(
                            Component.translatable("gui.iska_utils.blueprint_imported_to_machine", 
                                formatPosition(vertex1), formatPosition(vertex2), formatPosition(center)), 
                            true
                        );
                        
                        return InteractionResult.SUCCESS;
                    }
                }
            }
        }
        
        // Controlla se il primo vertice è già salvato
        if (!hasVertex1(tag)) {
            // Salva il primo vertice
            setVertex1(tag, clickedPos);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            
            // Mostra marker verde per il primo vertice
            MarkRenderer.getInstance().addBillboardMarker(clickedPos, VERTEX1_COLOR, 300); // 15 secondi
            
            serverPlayer.displayClientMessage(
                Component.translatable("item.iska_utils.blueprint.vertex1_saved", formatPosition(clickedPos)), 
                true
            );
            
            return InteractionResult.SUCCESS;
        } 
        else if (!hasVertex2(tag)) {
            // Salva il secondo vertice
            setVertex2(tag, clickedPos);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            
            // Mostra marker rosso per il secondo vertice
            MarkRenderer.getInstance().addBillboardMarker(clickedPos, VERTEX2_COLOR, 300); // 15 secondi
            
            // Calcola e mostra l'area
            BlockPos vertex1 = getVertex1(tag);
            showAreaPreview(vertex1, clickedPos);
            
            serverPlayer.displayClientMessage(
                Component.translatable("item.iska_utils.blueprint.vertex2_saved", formatPosition(clickedPos)), 
                true
            );
            
            // Mostra informazioni sull'area
            showAreaInfo(serverPlayer, vertex1, clickedPos);
            
            return InteractionResult.SUCCESS;
        } 
        else {
            // Entrambi i vertici sono già salvati
            BlockPos vertex1 = getVertex1(tag);
            BlockPos vertex2 = getVertex2(tag);
            
            if (!hasCenter(tag)) {
                // Terzo click: salva il centro della struttura
                setCenter(tag, clickedPos);
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                
                // Mostra marker giallo per il centro
                MarkRenderer.getInstance().addBillboardMarker(clickedPos, CENTER_COLOR, 300); // 15 secondi
                
                serverPlayer.displayClientMessage(
                    Component.translatable("item.iska_utils.blueprint.center_saved", formatPosition(clickedPos)), 
                    true
                );
                
                return InteractionResult.SUCCESS;
            } else {
                // Tutti e tre i punti sono salvati, mostra preview completo
                showAreaPreview(vertex1, vertex2);
                showAreaInfo(serverPlayer, vertex1, vertex2);
                
                // Mostra anche il centro
                BlockPos center = getCenter(tag);
                MarkRenderer.getInstance().addBillboardMarker(center, CENTER_COLOR, 300);
                
                serverPlayer.displayClientMessage(
                    Component.translatable("item.iska_utils.blueprint.preview_complete"), 
                    true
                );
                
                return InteractionResult.SUCCESS;
            }
        }
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.success(stack);
        }
        
        // Shift+destro nel vuoto per resettare i vertici
        if (player.isShiftKeyDown()) {
            // Cancella tutti i marker esistenti prima del reset
            MarkRenderer.getInstance().clearHighlightedBlocks();
            
            CompoundTag tag = new CompoundTag();
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            
            serverPlayer.displayClientMessage(
                Component.translatable("item.iska_utils.blueprint.reset"), 
                true
            );
            
            return InteractionResultHolder.success(stack);
        }
        
        return InteractionResultHolder.pass(stack);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        
        if (hasVertex1(tag)) {
            BlockPos vertex1 = getVertex1(tag);
            tooltip.add(Component.translatable("item.iska_utils.blueprint.tooltip.vertex1", formatPosition(vertex1))
                    .withStyle(ChatFormatting.GREEN));
        } else {
            tooltip.add(Component.translatable("item.iska_utils.blueprint.tooltip.click_first")
                    .withStyle(ChatFormatting.GRAY));
        }
        
        if (hasVertex2(tag)) {
            BlockPos vertex2 = getVertex2(tag);
            tooltip.add(Component.translatable("item.iska_utils.blueprint.tooltip.vertex2", formatPosition(vertex2))
                    .withStyle(ChatFormatting.RED));
            
            if (hasVertex1(tag)) {
                BlockPos vertex1 = getVertex1(tag);
                int[] dimensions = calculateDimensions(vertex1, vertex2);
                int volume = dimensions[0] * dimensions[1] * dimensions[2];
                
                tooltip.add(Component.translatable("item.iska_utils.blueprint.tooltip.dimensions", 
                        dimensions[0], dimensions[1], dimensions[2])
                        .withStyle(ChatFormatting.AQUA));
                tooltip.add(Component.translatable("item.iska_utils.blueprint.tooltip.volume", volume)
                        .withStyle(ChatFormatting.AQUA));
            }
        } else if (hasVertex1(tag)) {
            tooltip.add(Component.translatable("item.iska_utils.blueprint.tooltip.click_second")
                    .withStyle(ChatFormatting.GRAY));
        }
        
        if (hasCenter(tag)) {
            BlockPos center = getCenter(tag);
            tooltip.add(Component.translatable("item.iska_utils.blueprint.tooltip.center", formatPosition(center))
                    .withStyle(ChatFormatting.YELLOW));
        } else if (hasVertex1(tag) && hasVertex2(tag)) {
            tooltip.add(Component.translatable("item.iska_utils.blueprint.tooltip.click_center")
                    .withStyle(ChatFormatting.GRAY));
        }
        
        tooltip.add(Component.translatable("item.iska_utils.blueprint.tooltip.reset_hint")
                .withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
    }
    
    // Metodi di utilità per gestire i vertici
    
    private boolean hasVertex1(CompoundTag tag) {
        return tag.contains(VERTEX1_X) && tag.contains(VERTEX1_Y) && tag.contains(VERTEX1_Z);
    }
    
    private boolean hasVertex2(CompoundTag tag) {
        return tag.contains(VERTEX2_X) && tag.contains(VERTEX2_Y) && tag.contains(VERTEX2_Z);
    }
    
    private void setVertex1(CompoundTag tag, BlockPos pos) {
        tag.putInt(VERTEX1_X, pos.getX());
        tag.putInt(VERTEX1_Y, pos.getY());
        tag.putInt(VERTEX1_Z, pos.getZ());
    }
    
    private void setVertex2(CompoundTag tag, BlockPos pos) {
        tag.putInt(VERTEX2_X, pos.getX());
        tag.putInt(VERTEX2_Y, pos.getY());
        tag.putInt(VERTEX2_Z, pos.getZ());
    }
    
    private BlockPos getVertex1(CompoundTag tag) {
        return new BlockPos(
            tag.getInt(VERTEX1_X),
            tag.getInt(VERTEX1_Y),
            tag.getInt(VERTEX1_Z)
        );
    }
    
    private BlockPos getVertex2(CompoundTag tag) {
        return new BlockPos(
            tag.getInt(VERTEX2_X),
            tag.getInt(VERTEX2_Y),
            tag.getInt(VERTEX2_Z)
        );
    }
    
    public static BlockPos getVertex1FromStack(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(VERTEX1_X)) return null;
        
        return new BlockPos(
            tag.getInt(VERTEX1_X),
            tag.getInt(VERTEX1_Y),
            tag.getInt(VERTEX1_Z)
        );
    }
    
    public static BlockPos getVertex2FromStack(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(VERTEX2_X) || !tag.contains(VERTEX2_Y) || !tag.contains(VERTEX2_Z)) {
            return null;
        }
        return new BlockPos(
            tag.getInt(VERTEX2_X),
            tag.getInt(VERTEX2_Y),
            tag.getInt(VERTEX2_Z)
        );
    }
    
    public static BlockPos getCenterFromStack(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(CENTER_X) || !tag.contains(CENTER_Y) || !tag.contains(CENTER_Z)) {
            return null;
        }
        return new BlockPos(
            tag.getInt(CENTER_X),
            tag.getInt(CENTER_Y),
            tag.getInt(CENTER_Z)
        );
    }
    
    public static boolean hasValidArea(ItemStack stack) {
        return getVertex1FromStack(stack) != null && getVertex2FromStack(stack) != null;
    }
    
    public static boolean hasCompleteBlueprint(ItemStack stack) {
        boolean hasArea = hasValidArea(stack);
        boolean hasCenter = getCenterFromStack(stack) != null;
        boolean complete = hasArea && hasCenter;
        
        System.out.println("DEBUG BLUEPRINT: hasValidArea = " + hasArea);
        System.out.println("DEBUG BLUEPRINT: hasCenter = " + hasCenter);
        System.out.println("DEBUG BLUEPRINT: hasCompleteBlueprint = " + complete);
        
        return complete;
    }
    
    private String formatPosition(BlockPos pos) {
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }
    
    private int[] calculateDimensions(BlockPos vertex1, BlockPos vertex2) {
        int width = Math.abs(vertex2.getX() - vertex1.getX()) + 1;
        int height = Math.abs(vertex2.getY() - vertex1.getY()) + 1;
        int depth = Math.abs(vertex2.getZ() - vertex1.getZ()) + 1;
        
        return new int[]{width, height, depth};
    }
    
    private void showAreaPreview(BlockPos vertex1, BlockPos vertex2) {
        // Calcola i confini dell'area
        int minX = Math.min(vertex1.getX(), vertex2.getX());
        int maxX = Math.max(vertex1.getX(), vertex2.getX());
        int minY = Math.min(vertex1.getY(), vertex2.getY());
        int maxY = Math.max(vertex1.getY(), vertex2.getY());
        int minZ = Math.min(vertex1.getZ(), vertex2.getZ());
        int maxZ = Math.max(vertex1.getZ(), vertex2.getZ());
        
        // Mostra marker agli 8 angoli del parallelepipedo
        MarkRenderer.getInstance().addBillboardMarker(new BlockPos(minX, minY, minZ), AREA_COLOR, 300);
        MarkRenderer.getInstance().addBillboardMarker(new BlockPos(maxX, minY, minZ), AREA_COLOR, 300);
        MarkRenderer.getInstance().addBillboardMarker(new BlockPos(minX, maxY, minZ), AREA_COLOR, 300);
        MarkRenderer.getInstance().addBillboardMarker(new BlockPos(maxX, maxY, minZ), AREA_COLOR, 300);
        MarkRenderer.getInstance().addBillboardMarker(new BlockPos(minX, minY, maxZ), AREA_COLOR, 300);
        MarkRenderer.getInstance().addBillboardMarker(new BlockPos(maxX, minY, maxZ), AREA_COLOR, 300);
        MarkRenderer.getInstance().addBillboardMarker(new BlockPos(minX, maxY, maxZ), AREA_COLOR, 300);
        MarkRenderer.getInstance().addBillboardMarker(new BlockPos(maxX, maxY, maxZ), AREA_COLOR, 300);
        
        // Marker verdi e rossi per i vertici originali
        MarkRenderer.getInstance().addBillboardMarker(vertex1, VERTEX1_COLOR, 300);
        MarkRenderer.getInstance().addBillboardMarker(vertex2, VERTEX2_COLOR, 300);
    }
    
    private void showAreaInfo(ServerPlayer player, BlockPos vertex1, BlockPos vertex2) {
        int[] dimensions = calculateDimensions(vertex1, vertex2);
        int volume = dimensions[0] * dimensions[1] * dimensions[2];
        
        player.displayClientMessage(
            Component.translatable("item.iska_utils.blueprint.area_info", 
                    dimensions[0], dimensions[1], dimensions[2], volume), 
            true
        );
    }
    
    private boolean hasCenter(CompoundTag tag) {
        return tag.contains(CENTER_X) && tag.contains(CENTER_Y) && tag.contains(CENTER_Z);
    }
    
    private void setCenter(CompoundTag tag, BlockPos pos) {
        tag.putInt(CENTER_X, pos.getX());
        tag.putInt(CENTER_Y, pos.getY());
        tag.putInt(CENTER_Z, pos.getZ());
    }
    
    private BlockPos getCenter(CompoundTag tag) {
        return new BlockPos(
            tag.getInt(CENTER_X),
            tag.getInt(CENTER_Y),
            tag.getInt(CENTER_Z)
        );
    }
} 
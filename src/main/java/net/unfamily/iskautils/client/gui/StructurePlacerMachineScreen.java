package net.unfamily.iskautils.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.core.BlockPos;
import net.unfamily.iskautils.block.entity.StructurePlacerMachineBlockEntity;
// import net.unfamily.iskautils.client.gui.StructurePlacerScreen;
// import net.neoforged.neoforge.network.PacketDistributor;
// import net.unfamily.iskautils.network.packet.StructurePlacerMachineTogglePreviewC2SPacket;

/**
 * Screen for the Structure Placer Machine GUI
 */
public class StructurePlacerMachineScreen extends AbstractContainerScreen<StructurePlacerMachineMenu> {
    
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath("iska_utils", "textures/gui/backgrounds/block_structure.png");
    private static final ResourceLocation ENERGY_BAR = ResourceLocation.fromNamespaceAndPath("iska_utils", "textures/gui/energy_bar.png");
    
    // GUI dimensions (based on block_structure.png: 176x248 - increased button area height by 48px)
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 248;
    
    // Energy bar dimensions (from energy_bar.png: 16x32, first 8px = charged, next 8px = empty)
    private static final int ENERGY_BAR_WIDTH = 8;
    private static final int ENERGY_BAR_HEIGHT = 32;
    
    // Buttons for the X layout
    private Button structureSelectButton;  // Top left
    private Button showButton;            // Top right (aligned with title end) - renamed from applyButton
    private Button rotateButton;          // Bottom left
    private Button setInventoryButton;    // Bottom right
    
    public StructurePlacerMachineScreen(StructurePlacerMachineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        
        // Set the GUI dimensions
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Center the GUI on screen
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        
        // Initialize buttons in X layout
        initializeButtons();
    }
    
    private void initializeButtons() {
        // Calculate title width for alignment
        Component titleComponent = Component.translatable("block.iska_utils.structure_placer_machine");
        String titleText = titleComponent.getString();
        int titleWidth = this.font.width(titleText);
        int titleStartX = (this.imageWidth - titleWidth) / 2;
        int titleEndX = titleStartX + titleWidth;
        
        // Button dimensions
        int buttonWidth = 50;
        int buttonHeight = 20;
        
        // Top row Y position (under title)
        int topRowY = this.topPos + 25;
        
        // Bottom row Y position
        int bottomRowY = this.topPos + 70;
        
        // Top Left: Select Button (after energy bar)
        int selectButtonX = this.leftPos + 20; // After energy bar + margin
        this.structureSelectButton = Button.builder(
                Component.translatable("gui.iska_utils.structure_placer_machine.select"),
                button -> onStructureSelectPressed()
        ).bounds(selectButtonX, topRowY, buttonWidth, buttonHeight).build();
        this.addRenderableWidget(this.structureSelectButton);
        
        // Top Right: Show Button (aligned with title end)
        int showButtonX = this.leftPos + titleEndX - buttonWidth;
        this.showButton = Button.builder(
                Component.translatable("gui.iska_utils.structure_placer_machine.show"),
                button -> onShowPressed()
        ).bounds(showButtonX, topRowY, buttonWidth, buttonHeight).build();
        this.addRenderableWidget(this.showButton);
        
        // Bottom Left: Rotate Button
        this.rotateButton = Button.builder(
                Component.translatable("gui.iska_utils.structure_placer_machine.rotate"),
                button -> onRotatePressed()
        ).bounds(selectButtonX, bottomRowY, buttonWidth, buttonHeight).build();
        this.addRenderableWidget(this.rotateButton);
        
        // Bottom Right: Set Inventory Button
        this.setInventoryButton = Button.builder(
                Component.translatable("gui.iska_utils.structure_placer_machine.set_inventory"),
                button -> onSetInventoryPressed()
        ).bounds(showButtonX, bottomRowY, buttonWidth, buttonHeight).build();
        this.addRenderableWidget(this.setInventoryButton);
    }
    
    private void onStructureSelectPressed() {
        // Create a new structure selection menu without slots
        if (this.minecraft != null && this.minecraft.player != null) {
            // Get the block entity from the current menu to pass to the selection menu
            StructurePlacerMachineBlockEntity blockEntity = this.menu.getBlockEntityFromLevel(this.minecraft.level);
            
            StructureSelectionMenu selectionMenu;
            if (blockEntity != null) {
                // Pass the block position for client-side lookup
                selectionMenu = new StructureSelectionMenu(0, this.minecraft.player.getInventory(), blockEntity.getBlockPos());
            } else {
                // Fallback: try to get position from menu
                BlockPos machinePos = this.menu.getBlockPos();
                if (!machinePos.equals(BlockPos.ZERO)) {
                    selectionMenu = new StructureSelectionMenu(0, this.minecraft.player.getInventory(), machinePos);
                } else {
                    selectionMenu = new StructureSelectionMenu(0, this.minecraft.player.getInventory());
                }
            }
            
            StructureSelectionScreen selectionScreen = new StructureSelectionScreen(
                selectionMenu, 
                this.minecraft.player.getInventory(), 
                Component.translatable("gui.iska_utils.structure_selection.title")
            );
            this.minecraft.setScreen(selectionScreen);
        }
    }
    
    private void onShowPressed() {
        // Get the machine position from the menu (synced from server)
        BlockPos machinePos = this.menu.getSyncedBlockPos();
        System.out.println("=== DEBUG onShowPressed ===");
        System.out.println("Machine position from menu: " + machinePos);
        System.out.println("Is ZERO? " + machinePos.equals(BlockPos.ZERO));
        
        if (!machinePos.equals(BlockPos.ZERO)) {
            System.out.println("Calling sendStructurePlacerMachineShowPacket...");
            // Send show packet to toggle preview mode
            net.unfamily.iskautils.network.ModMessages.sendStructurePlacerMachineShowPacket(machinePos);
        } else {
            System.out.println("Position is ZERO, not calling packet method");
            
            // Try to get position from block entity as fallback
            if (this.minecraft != null && this.minecraft.level != null) {
                StructurePlacerMachineBlockEntity blockEntity = this.menu.getBlockEntityFromLevel(this.minecraft.level);
                if (blockEntity != null) {
                    BlockPos actualPos = blockEntity.getBlockPos();
                    System.out.println("Found block entity at: " + actualPos);
                    net.unfamily.iskautils.network.ModMessages.sendStructurePlacerMachineShowPacket(actualPos);
                } else {
                    System.out.println("No block entity found nearby");
                }
            }
        }
        System.out.println("=== END DEBUG onShowPressed ===");
    }
    
    private void onRotatePressed() {
        // Get the machine position from the menu (synced from server)
        BlockPos machinePos = this.menu.getSyncedBlockPos();
        if (!machinePos.equals(BlockPos.ZERO)) {
            // Send rotate packet to rotate the structure
            net.unfamily.iskautils.network.ModMessages.sendStructurePlacerMachineRotatePacket(machinePos);
        }
    }
    
    private void onSetInventoryPressed() {
        // Implementation for set inventory button pressed
    }
    
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // Draw the background texture
        guiGraphics.blit(BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, GUI_WIDTH, GUI_HEIGHT);
        
        // Draw the energy bar
        renderEnergyBar(guiGraphics);
    }
    
    private void renderEnergyBar(GuiGraphics guiGraphics) {
        // Position energy bar more internally and centered between left buttons (Select and Rotate)
        // Select button is at X=20, so center the energy bar between it and the left edge
        int energyBarX = this.leftPos + ((20 - ENERGY_BAR_WIDTH) / 2); // Centered between left edge and Select button
        
        // Center vertically between top row (Y=25) and bottom row (Y=70)
        // Top button: Y=25, height=20, so bottom = 45
        // Bottom button: Y=70, so center between 45 and 70 = 57.5, minus half bar height
        int energyBarY = this.topPos + 57 - (ENERGY_BAR_HEIGHT / 2); // Centered between button rows
        
        // Always draw empty energy bar background (right half of texture - pixels 8-15)
        guiGraphics.blit(ENERGY_BAR, energyBarX, energyBarY, 
                       8, 0, // Source: right half starts at x=8 (empty part)
                       ENERGY_BAR_WIDTH, ENERGY_BAR_HEIGHT, 
                       16, 32); // Total texture size: 16x32
        
        // Calculate energy fill percentage and draw filled part using synced data
        int energy = this.menu.getEnergyStored();
        int maxEnergy = this.menu.getMaxEnergyStored();
        
        if (energy > 0 && maxEnergy > 0) {
            int energyHeight = (energy * ENERGY_BAR_HEIGHT) / maxEnergy;
            int energyY = energyBarY + (ENERGY_BAR_HEIGHT - energyHeight);
            
            // Draw filled energy bar (left half of texture - pixels 0-7, from bottom up)
            guiGraphics.blit(ENERGY_BAR, energyBarX, energyY,
                           0, ENERGY_BAR_HEIGHT - energyHeight, // Source: left half (charged part), from bottom
                           ENERGY_BAR_WIDTH, energyHeight,
                           16, 32); // Total texture size: 16x32
        }
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Render the background
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        // Render energy bar tooltip
        renderEnergyTooltip(guiGraphics, mouseX, mouseY);
        
        // Render item tooltips
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
    
    private void renderEnergyTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Use same position calculation as in renderEnergyBar
        int energyBarX = this.leftPos + ((20 - ENERGY_BAR_WIDTH) / 2); // Centered between left edge and Select button
        int energyBarY = this.topPos + 57 - (ENERGY_BAR_HEIGHT / 2); // Centered between button rows
        
        // Check if mouse is over energy bar
        if (mouseX >= energyBarX && mouseX <= energyBarX + ENERGY_BAR_WIDTH &&
            mouseY >= energyBarY && mouseY <= energyBarY + ENERGY_BAR_HEIGHT) {
            
            // Use synced data for tooltip
            int energy = this.menu.getEnergyStored();
            int maxEnergy = this.menu.getMaxEnergyStored();
            
            Component tooltip = Component.literal(String.format("%,d / %,d FE", energy, maxEnergy));
            guiGraphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }
    
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Draw the title (centered)
        Component titleComponent = Component.translatable("block.iska_utils.structure_placer_machine");
        String title = titleComponent.getString();
        int titleX = (this.imageWidth - this.font.width(title)) / 2;
        guiGraphics.drawString(this.font, title, titleX, 6, 0x404040, false);
        
        // Draw selected structure text (centered between button rows)
        renderSelectedStructureText(guiGraphics);
    }
    
    private void renderSelectedStructureText(GuiGraphics guiGraphics) {
        // Get the selected structure from the block entity
        StructurePlacerMachineBlockEntity blockEntity = this.menu.getBlockEntityFromLevel(this.minecraft.level);
        String selectedStructure = "";
        
        if (blockEntity != null && !blockEntity.getSelectedStructure().isEmpty()) {
            // Try to get the structure name, fallback to ID
            var structure = net.unfamily.iskautils.structure.StructureLoader.getStructure(blockEntity.getSelectedStructure());
            if (structure != null) {
                selectedStructure = structure.getName() != null ? structure.getName() : structure.getId();
            } else {
                selectedStructure = blockEntity.getSelectedStructure();
            }
        }
        
        // Create the text components (first line: label, second line: structure name)
        Component labelComponent = Component.translatable("gui.iska_utils.structure_placer_machine.selected_structure");
        String labelText = labelComponent.getString() + ":"; // Remove space after colon since it's on its own line
        
        Component structureComponent = selectedStructure.isEmpty() ? 
            Component.translatable("gui.iska_utils.structure_placer_machine.none_selected") :
            Component.literal(selectedStructure);
        String structureText = structureComponent.getString();
        
        // Use a smaller scale for better fit
        float scale = 0.75f; // 75% of normal size for better fit
        
        // Calculate positions for two-line text (centered exactly between the 4 buttons)
        // Top row: Y=25, height=20, so ends at Y=45
        // Bottom row: Y=70
        // Center between 45 and 70 = 57.5, adjusted for two lines
        int firstLineY = 51;  // First line (label) slightly above center
        int secondLineY = 60; // Second line (structure name) slightly below center
        
        // Save current matrix state
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(scale, scale, 1.0f);
        
        // Calculate scaled positions
        int scaledFirstLineY = Math.round(firstLineY / scale);
        int scaledSecondLineY = Math.round(secondLineY / scale);
        
        // Draw first line: "Selected Structure:" (centered, dark color)
        int scaledLabelWidth = this.font.width(labelText);
        int scaledLabelX = Math.round((this.imageWidth / scale - scaledLabelWidth) / 2);
        guiGraphics.drawString(this.font, labelText, scaledLabelX, scaledFirstLineY, 0x404040, false);
        
        // Draw second line: structure name or "None" (centered, colored)
        int scaledStructureWidth = this.font.width(structureText);
        int scaledStructureX = Math.round((this.imageWidth / scale - scaledStructureWidth) / 2);
        int structureColor = selectedStructure.isEmpty() ? 0xFF4040 : 0x4040FF;
        guiGraphics.drawString(this.font, structureText, scaledStructureX, scaledSecondLineY, structureColor, false);
        
        // Restore matrix state
        guiGraphics.pose().popPose();
    }
} 
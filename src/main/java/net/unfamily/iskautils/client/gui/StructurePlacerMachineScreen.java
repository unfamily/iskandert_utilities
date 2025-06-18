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
    
    // Medium buttons texture (16x32 - normal and highlighted)
    private static final ResourceLocation MEDIUM_BUTTONS = ResourceLocation.fromNamespaceAndPath("iska_utils", "textures/gui/medium_buttons.png");
    // Redstone GUI icon
    private static final ResourceLocation REDSTONE_GUI = ResourceLocation.fromNamespaceAndPath("iska_utils", "textures/gui/redstone_gui.png");
    
    // GUI dimensions (based on block_structure.png: 176x248 - increased button area height by 48px)
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 248;
    
    // Energy bar dimensions (from energy_bar.png: 16x32, first 8px = charged, next 8px = empty)
    private static final int ENERGY_BAR_WIDTH = 8;
    private static final int ENERGY_BAR_HEIGHT = 32;
    
    // Button references
    private Button structureSelectButton;  // Top left
    private Button showButton;            // Top right (aligned with title end) - renamed from applyButton
    private Button rotateButton;          // Bottom left
    private Button setInventoryButton;    // Bottom right
    
    // Custom redstone mode button
    private int redstoneModeButtonX, redstoneModeButtonY;
    private static final int REDSTONE_BUTTON_SIZE = 16;
    
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
    
    @Override
    public void containerTick() {
        super.containerTick();
        
        // Update button states based on current data
        updateButtonStates();
    }
    
    /**
     * Updates button states and text based on current synced data
     */
    private void updateButtonStates() {
        // Aggiorniamo solo il testo del pulsante di rotazione
        // Il pulsante "Show" mantiene il testo fisso
        
        if (this.rotateButton != null) {
            // Update rotate button text to show current rotation
            int rotation = this.menu.getRotation();
            String rotationText = switch (rotation) {
                case 0 -> Component.translatable("direction.iska_utils.north").getString();
                case 90 -> Component.translatable("direction.iska_utils.east").getString(); 
                case 180 -> Component.translatable("direction.iska_utils.south").getString();
                case 270 -> Component.translatable("direction.iska_utils.west").getString();
                default -> rotation + "°";
            };
            Component rotateText = Component.literal("↻ " + rotationText);
            this.rotateButton.setMessage(rotateText);
        }
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
        
        // Top Right: Show Button (aligned with title end) - testo fisso "Show"
        int showButtonX = this.leftPos + titleEndX - buttonWidth;
        this.showButton = Button.builder(
                Component.translatable("gui.iska_utils.structure_placer_machine.show"),
                button -> onShowPressed()
        ).bounds(showButtonX, topRowY, buttonWidth, buttonHeight).build();
        this.addRenderableWidget(this.showButton);
        
        // Bottom Left: Rotate Button - text will be updated by updateButtonStates()
        int rotation = this.menu.getRotation();
        String rotationText = switch (rotation) {
            case 0 -> Component.translatable("direction.iska_utils.north").getString();
            case 90 -> Component.translatable("direction.iska_utils.east").getString(); 
            case 180 -> Component.translatable("direction.iska_utils.south").getString();
            case 270 -> Component.translatable("direction.iska_utils.west").getString();
            default -> rotation + "°";
        };
        Component initialRotateText = Component.literal("↻ " + rotationText);
        this.rotateButton = Button.builder(
                initialRotateText,
                button -> onRotatePressed()
        ).bounds(selectButtonX, bottomRowY, buttonWidth, buttonHeight).build();
        this.addRenderableWidget(this.rotateButton);
        
        // Bottom Right: Set Inventory Button
        this.setInventoryButton = Button.builder(
                Component.translatable("gui.iska_utils.structure_placer_machine.set_inventory"),
                button -> onSetInventoryPressed()
        ).bounds(showButtonX, bottomRowY, buttonWidth, buttonHeight).build();
        this.addRenderableWidget(this.setInventoryButton);
        
        // Right side: Redstone Mode Button (opposite to energy bar, between Show and Set Inventory)
        // Position it at the right edge, similar to energy bar positioning on the left
        int rightMargin = ((20 - REDSTONE_BUTTON_SIZE) / 2); // Same margin logic as energy bar
        this.redstoneModeButtonX = this.leftPos + this.imageWidth - rightMargin - REDSTONE_BUTTON_SIZE;
        // Position Y between Show (topRowY=25) and Set Inventory (bottomRowY=70)
        // Show button bottom: 25+20=45, Set Inventory top: 70
        // Center between them: (45+70)/2 = 57.5, minus half button height: 57.5-8=49.5
        this.redstoneModeButtonY = this.topPos + 49;
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
    
    private void onRedstoneModePressed() {
        // Get the machine position from the menu (synced from server)
        BlockPos machinePos = this.menu.getSyncedBlockPos();
        if (!machinePos.equals(BlockPos.ZERO)) {
            // Send redstone mode packet to cycle the mode
            net.unfamily.iskautils.network.ModMessages.sendStructurePlacerMachineRedstoneModePacket(machinePos);
        }
    }
    
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // Draw the background texture
        guiGraphics.blit(BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, GUI_WIDTH, GUI_HEIGHT);
        
        // Draw the energy bar
        renderEnergyBar(guiGraphics);
        
        // Draw the custom redstone mode button
        renderRedstoneModeButton(guiGraphics, mouseX, mouseY);
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
    
    private void renderRedstoneModeButton(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Check if mouse is over the button
        boolean isHovered = mouseX >= this.redstoneModeButtonX && mouseX <= this.redstoneModeButtonX + REDSTONE_BUTTON_SIZE &&
                           mouseY >= this.redstoneModeButtonY && mouseY <= this.redstoneModeButtonY + REDSTONE_BUTTON_SIZE;
        
        // Draw button background (normal or highlighted)
        int textureY = isHovered ? 16 : 0; // Highlighted version is below the normal one
        guiGraphics.blit(MEDIUM_BUTTONS, this.redstoneModeButtonX, this.redstoneModeButtonY, 
                        0, textureY, REDSTONE_BUTTON_SIZE, REDSTONE_BUTTON_SIZE, 
                        96, 96); // Correct texture size: 96x96
        
        // Get current redstone mode from menu
        int redstoneMode = this.menu.getRedstoneMode();
        
        // Draw the appropriate icon (12x12 pixels, centered in the 16x16 button)
        int iconX = this.redstoneModeButtonX + 2; // Center: (16-12)/2 = 2
        int iconY = this.redstoneModeButtonY + 2; // Center: (16-12)/2 = 2
        int iconSize = 12;
        
        switch (redstoneMode) {
            case 0 -> {
                // NONE mode: Gunpowder icon
                net.minecraft.world.item.ItemStack gunpowder = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GUNPOWDER);
                renderScaledItem(guiGraphics, gunpowder, iconX, iconY, iconSize);
            }
            case 1 -> {
                // LOW mode: Redstone dust icon
                net.minecraft.world.item.ItemStack redstone = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.REDSTONE);
                renderScaledItem(guiGraphics, redstone, iconX, iconY, iconSize);
            }
            case 2 -> {
                // HIGH mode: Redstone GUI texture rendered as item-like (12x12)
                renderScaledTexture(guiGraphics, REDSTONE_GUI, iconX, iconY, iconSize);
            }
            case 3 -> {
                // PULSE mode: Repeater icon
                net.minecraft.world.item.ItemStack repeater = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.REPEATER);
                renderScaledItem(guiGraphics, repeater, iconX, iconY, iconSize);
            }
        }
    }
    
    /**
     * Renders an item scaled to the specified size
     */
    private void renderScaledItem(GuiGraphics guiGraphics, net.minecraft.world.item.ItemStack itemStack, int x, int y, int size) {
        // Save current matrix state
        guiGraphics.pose().pushPose();
        
        // Calculate scale: original item size is 16x16, we want 12x12
        float scale = (float) size / 16.0f;
        
        // Translate to position and apply scale
        guiGraphics.pose().translate(x, y, 0);
        guiGraphics.pose().scale(scale, scale, 1.0f);
        
        // Render the item
        guiGraphics.renderItem(itemStack, 0, 0);
        
        // Restore matrix state
        guiGraphics.pose().popPose();
    }
    
    /**
     * Renders a texture scaled to the specified size (like an item)
     */
    private void renderScaledTexture(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y, int size) {
        // Save current matrix state
        guiGraphics.pose().pushPose();
        
        // Calculate scale: original texture size is 16x16, we want 12x12
        float scale = (float) size / 16.0f;
        
        // Translate to position and apply scale
        guiGraphics.pose().translate(x, y, 0);
        guiGraphics.pose().scale(scale, scale, 1.0f);
        
        // Render the texture (assuming it's 16x16)
        guiGraphics.blit(texture, 0, 0, 0, 0, 16, 16, 16, 16);
        
        // Restore matrix state
        guiGraphics.pose().popPose();
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Render the background
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        // Render energy bar tooltip
        renderEnergyTooltip(guiGraphics, mouseX, mouseY);
        
        // Render redstone mode button tooltip
        renderRedstoneModeTooltip(guiGraphics, mouseX, mouseY);
        
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
    
    private void renderRedstoneModeTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Check if mouse is over the button
        boolean isHovered = mouseX >= this.redstoneModeButtonX && mouseX <= this.redstoneModeButtonX + REDSTONE_BUTTON_SIZE &&
                           mouseY >= this.redstoneModeButtonY && mouseY <= this.redstoneModeButtonY + REDSTONE_BUTTON_SIZE;
        
        if (isHovered) {
            // Get current redstone mode from menu
            int redstoneMode = this.menu.getRedstoneMode();
            
            // Draw the appropriate tooltip
            Component tooltip = switch (redstoneMode) {
                case 0 -> Component.translatable("gui.iska_utils.structure_placer_machine.redstone_mode.none");
                case 1 -> Component.translatable("gui.iska_utils.structure_placer_machine.redstone_mode.low");
                case 2 -> Component.translatable("gui.iska_utils.structure_placer_machine.redstone_mode.high");
                case 3 -> Component.translatable("gui.iska_utils.structure_placer_machine.redstone_mode.pulse");
                default -> Component.literal("Unknown mode");
            };
            
            // Calculate tooltip position
            int tooltipX = this.redstoneModeButtonX - this.font.width(tooltip.getString()) - 10;
            int tooltipY = this.redstoneModeButtonY - 20;
            
            guiGraphics.drawString(this.font, tooltip, tooltipX, tooltipY, 0xFFFFFF, false);
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
        // Use the new cached structure method from the menu instead of directly accessing block entity
        String selectedStructure = this.menu.getCachedSelectedStructure();
        
        // If we have a structure ID, try to get the display name
        String displayName = "";
        if (!selectedStructure.isEmpty()) {
            var structure = net.unfamily.iskautils.structure.StructureLoader.getStructure(selectedStructure);
            if (structure != null) {
                displayName = structure.getName() != null ? structure.getName() : structure.getId();
            } else {
                displayName = selectedStructure; // Fallback to ID if structure not found
            }
        }
        
        // Create the text components (first line: label, second line: structure name)
        Component labelComponent = Component.translatable("gui.iska_utils.structure_placer_machine.selected_structure");
        String labelText = labelComponent.getString() + ":"; // Remove space after colon since it's on its own line
        
        Component structureComponent = displayName.isEmpty() ? 
            Component.translatable("gui.iska_utils.structure_placer_machine.none_selected") :
            Component.literal(displayName);
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
        int structureColor = displayName.isEmpty() ? 0xFF4040 : 0x4040FF;
        guiGraphics.drawString(this.font, structureText, scaledStructureX, scaledSecondLineY, structureColor, false);
        
        // Restore matrix state
        guiGraphics.pose().popPose();
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Check if click is on redstone mode button
        if (button == 0) { // Left click
            if (mouseX >= this.redstoneModeButtonX && mouseX <= this.redstoneModeButtonX + REDSTONE_BUTTON_SIZE &&
                mouseY >= this.redstoneModeButtonY && mouseY <= this.redstoneModeButtonY + REDSTONE_BUTTON_SIZE) {
                
                onRedstoneModePressed();
                return true;
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
} 
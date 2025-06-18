package net.unfamily.iskautils.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
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
    
    // GUI dimensions (based on block_structure.png: 176x200)
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 200;
    
    // Energy bar dimensions (from energy_bar.png: 16x32, first 8px = charged, next 8px = empty)
    private static final int ENERGY_BAR_WIDTH = 8;
    private static final int ENERGY_BAR_HEIGHT = 32;
    
    // Button for structure selection
    private Button structureSelectButton;
    
    // Button for preview toggle
    private Button previewToggleButton;
    
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
        
        // Initialize buttons
        initializeButtons();
    }
    
    private void initializeButtons() {
        // Energy bar will be at leftPos + 5, so buttons start after that
        // Energy bar is 8px wide + some spacing = about 15px total
        
        // Structure Selection Button (moved right to make space for energy bar)
        int selectButtonX = this.leftPos + 25; // 25px from left edge (after energy bar)
        int selectButtonY = this.topPos + 25;  // 25px from top edge of GUI
        int buttonWidth = 40;
        int buttonHeight = 20;
        
        this.structureSelectButton = Button.builder(
                Component.translatable("gui.iska_utils.structure_placer.select"),
                button -> onStructureSelectPressed()
        ).bounds(selectButtonX, selectButtonY, buttonWidth, buttonHeight).build();
        
        this.addRenderableWidget(this.structureSelectButton);
        
        // Preview Toggle Button (right button)
        int previewButtonX = this.leftPos + 70; // 70px from left edge of GUI (next to select button)
        int previewButtonY = this.topPos + 25;  // Same Y as select button
        
        String previewText = getPreviewToggleText();
        this.previewToggleButton = Button.builder(
                Component.literal(previewText),
                button -> onPreviewTogglePressed()
        ).bounds(previewButtonX, previewButtonY, buttonWidth, buttonHeight).build();
        
        this.addRenderableWidget(this.previewToggleButton);
    }
    
    private void onStructureSelectPressed() {
        // Open the structure selection screen with proper parameters
        if (this.minecraft != null && this.minecraft.player != null) {
            StructureSelectionScreen selectionScreen = new StructureSelectionScreen(
                this.menu, 
                this.minecraft.player.getInventory(), 
                Component.translatable("gui.iska_utils.structure_selection.title")
            );
            this.minecraft.setScreen(selectionScreen);
        }
    }
    
    private void onPreviewTogglePressed() {
        // Toggle preview mode using block entity from level
        if (this.minecraft != null && this.minecraft.level != null) {
            StructurePlacerMachineBlockEntity blockEntity = this.menu.getBlockEntityFromLevel(this.minecraft.level);
            if (blockEntity != null) {
                boolean currentPreview = blockEntity.isShowPreview();
                blockEntity.setShowPreview(!currentPreview);
                
                // Update button text
                String newText = getPreviewToggleText();
                this.previewToggleButton.setMessage(Component.literal(newText));
                
                if (this.minecraft.player != null) {
                    String message = currentPreview ? "Preview disabled" : "Preview enabled";
                    this.minecraft.player.sendSystemMessage(Component.literal(message));
                }
            }
        }
    }
    
    private String getPreviewToggleText() {
        // Use synced data for button text
        return this.menu.isShowPreview() ? "Hide" : "Show";
    }
    
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // Draw the background texture
        guiGraphics.blit(BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, GUI_WIDTH, GUI_HEIGHT);
        
        // Draw the energy bar
        renderEnergyBar(guiGraphics);
    }
    
    private void renderEnergyBar(GuiGraphics guiGraphics) {
        // Use synced data instead of blockEntity directly
        int energyBarX = this.leftPos + 5; // Very close to left edge
        int energyBarY = this.topPos + 25 + (20 - ENERGY_BAR_HEIGHT) / 2; // Centered vertically with buttons (button Y=25, height=20)
        
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
        int energyBarX = this.leftPos + 5;
        int energyBarY = this.topPos + 25 + (20 - ENERGY_BAR_HEIGHT) / 2; // Same position as in renderEnergyBar
        
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
        
        // Draw "Inventory" label
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0x404040, false);
    }
} 
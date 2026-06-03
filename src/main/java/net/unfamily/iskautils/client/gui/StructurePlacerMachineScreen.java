package net.unfamily.iskautils.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.core.BlockPos;
import net.unfamily.iskautils.block.entity.StructurePlacerMachineBlockEntity;
import net.unfamily.iskalib.client.marker.MarkRenderer;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.unfamily.iskautils.network.packet.StructurePlacerMachineTogglePreviewC2SPacket;
import net.minecraft.world.item.ItemStack;

/**
 * Screen for the Structure Placer Machine GUI
 */
public class StructurePlacerMachineScreen extends AbstractContainerScreen<StructurePlacerMachineMenu> {
    
    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath("iska_utils", "textures/gui/backgrounds/block_structure.png");
    private static final Identifier ENERGY_BAR = Identifier.fromNamespaceAndPath("iska_utils", "textures/gui/energy_bar.png");
    
    // GUI dimensions (based on block_structure.png: 176x248 - increased button area height by 48px)
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 248;
    
    // Energy bar dimensions (from energy_bar.png: 16x32, first 8px = charged, next 8px = empty)
    private static final int ENERGY_BAR_WIDTH = 8;
    private static final int ENERGY_BAR_HEIGHT = 32;
    
    // Button references
    private Button structureSelectButton;  // Top left
    private Button showButton;            // Top right (aligned with title end) - renamed from applyButton
    private boolean previewButtonShowsHide;
    private Button rotateButton;          // Bottom left
    private Button setInventoryButton;    // Bottom right
    private Button closeButton;           // Close button
    
    private ItemIconButton redstoneModeButton;
    private static final int REDSTONE_BUTTON_SIZE = 16;
    
    // Close button position - top right
    private static final int CLOSE_BUTTON_Y = 5;
    private static final int CLOSE_BUTTON_SIZE = 12;
    private static final int CLOSE_BUTTON_X = GUI_WIDTH - CLOSE_BUTTON_SIZE - 5; // 5px from right edge
    
    public StructurePlacerMachineScreen(StructurePlacerMachineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, GUI_WIDTH, GUI_HEIGHT);
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Ricarica le strutture all'apertura della GUI usando il flag del config
        net.unfamily.iskalib.structure.StructureLoader.reloadAllDefinitions(net.unfamily.iskautils.Config.acceptClientStructure);
        
        // Center the GUI on screen
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        
        // Initialize buttons in X layout
        initializeButtons();
        previewButtonShowsHide = menu.isShowPreview();
        updatePreviewButtonLabel();
        
        // Close button - top left with ✕ symbol
        closeButton = Button.builder(Component.literal("✕"), 
                                    button -> {
                                        playButtonSound();
                                        this.onClose();
                                    })
                           .bounds(this.leftPos + CLOSE_BUTTON_X, this.topPos + CLOSE_BUTTON_Y, 
                                  CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE)
                           .build();
        addRenderableWidget(closeButton);
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
        if (menu.isShowPreview() != previewButtonShowsHide) {
            previewButtonShowsHide = menu.isShowPreview();
            updatePreviewButtonLabel();
        }

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
                Component.translatable("gui.iska_utils.generic.show"),
                button -> togglePreview()
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
        ).bounds(showButtonX, bottomRowY, buttonWidth, buttonHeight)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(
                        Component.translatable("gui.iska_utils.structure_placer_machine.set_inventory.tooltip.line1")
                                .append("\n")
                                .append(Component.translatable("gui.iska_utils.structure_placer_machine.set_inventory.tooltip.line2"))
                                .append("\n")
                                .append(Component.translatable("gui.iska_utils.structure_placer_machine.set_inventory.tooltip.line3"))))
                .build();
        this.addRenderableWidget(this.setInventoryButton);
        
        int rightMargin = ((20 - REDSTONE_BUTTON_SIZE) / 2) + 10;
        int redstoneX = this.leftPos + this.imageWidth - rightMargin - REDSTONE_BUTTON_SIZE;
        int redstoneY = this.topPos + 49;
        redstoneModeButton = addRenderableWidget(MachineGuiButtons.redstoneIconButton(
                redstoneX, redstoneY, b -> onRedstoneModePressed(false), menu::getRedstoneMode, true));
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
    
    private void togglePreview() {
        BlockPos machinePos = resolveMachinePos();
        if (machinePos.equals(BlockPos.ZERO)) {
            return;
        }
        playButtonSound();
        boolean enabling = !menu.isShowPreview();
        MarkRenderer.getInstance().clearBillboardMarkersForOwner(machinePos);
        ClientPacketDistributor.sendToServer(new StructurePlacerMachineTogglePreviewC2SPacket(machinePos, enabling));
        previewButtonShowsHide = enabling;
        updatePreviewButtonLabel();
    }

    private BlockPos resolveMachinePos() {
        BlockPos machinePos = menu.getSyncedBlockPos();
        if (!machinePos.equals(BlockPos.ZERO)) {
            return machinePos;
        }
        if (minecraft != null && minecraft.level != null) {
            StructurePlacerMachineBlockEntity blockEntity = menu.getBlockEntityFromLevel(minecraft.level);
            if (blockEntity != null) {
                return blockEntity.getBlockPos();
            }
        }
        return BlockPos.ZERO;
    }

    private void updatePreviewButtonLabel() {
        if (showButton != null) {
            showButton.setMessage(Component.translatable(
                    previewButtonShowsHide
                            ? "gui.iska_utils.generic.hide"
                            : "gui.iska_utils.generic.show"));
        }
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
        // Get the machine position from the menu (synced from server)
        BlockPos machinePos = this.menu.getSyncedBlockPos();
        
        if (!machinePos.equals(BlockPos.ZERO)) {
            // Determine the mode based on modifier keys
            int mode = net.unfamily.iskautils.network.packet.StructurePlacerMachineSetInventoryC2SPacket.MODE_NORMAL;
            
            if (isShiftDownNow()) {
                mode = net.unfamily.iskautils.network.packet.StructurePlacerMachineSetInventoryC2SPacket.MODE_SHIFT;
            } else if (isCtrlDownNow() || isAltDownNow()) {
                // Support both Ctrl and Alt/AltGr for the same functionality
                mode = net.unfamily.iskautils.network.packet.StructurePlacerMachineSetInventoryC2SPacket.MODE_CTRL;
            }
            
            // Use the same approach as all other buttons
            net.unfamily.iskautils.network.ModMessages.sendStructurePlacerMachineSetInventoryPacket(machinePos, mode);
        } else {
            // Try fallback method
            if (this.minecraft != null && this.minecraft.level != null) {
                StructurePlacerMachineBlockEntity blockEntity = this.menu.getBlockEntityFromLevel(this.minecraft.level);
                if (blockEntity != null) {
                    BlockPos actualPos = blockEntity.getBlockPos();
                    
                    // Determine mode again for fallback
                    int mode = net.unfamily.iskautils.network.packet.StructurePlacerMachineSetInventoryC2SPacket.MODE_NORMAL;
                    if (isShiftDownNow()) {
                        mode = net.unfamily.iskautils.network.packet.StructurePlacerMachineSetInventoryC2SPacket.MODE_SHIFT;
                    } else if (isCtrlDownNow() || isAltDownNow()) {
                        mode = net.unfamily.iskautils.network.packet.StructurePlacerMachineSetInventoryC2SPacket.MODE_CTRL;
                    }
                    
                    net.unfamily.iskautils.network.ModMessages.sendStructurePlacerMachineSetInventoryPacket(actualPos, mode);
                }
            }
        }
    }
    
    private void onRedstoneModePressed(boolean backward) {
        // Try multiple methods to get the machine position
        BlockPos machinePos = this.menu.getSyncedBlockPos();
        
        // If getSyncedBlockPos returns ZERO, try getBlockPos
        if (machinePos.equals(BlockPos.ZERO)) {
            machinePos = this.menu.getBlockPos();
        }
        
        // If still ZERO, try to find the machine by searching nearby
        if (machinePos.equals(BlockPos.ZERO) && this.minecraft != null && this.minecraft.level != null && this.minecraft.player != null) {
            BlockPos playerPos = this.minecraft.player.blockPosition();
            
            // Search in a 16x16x16 area around player for the machine
            for (int x = -8; x <= 8; x++) {
                for (int y = -8; y <= 8; y++) {
                    for (int z = -8; z <= 8; z++) {
                        BlockPos searchPos = playerPos.offset(x, y, z);
                        if (this.minecraft.level.getBlockEntity(searchPos) instanceof net.unfamily.iskautils.block.entity.StructurePlacerMachineBlockEntity) {
                            machinePos = searchPos;
                            break;
                        }
                    }
                    if (!machinePos.equals(BlockPos.ZERO)) break;
                }
                if (!machinePos.equals(BlockPos.ZERO)) break;
            }
        }
        
        if (!machinePos.equals(BlockPos.ZERO)) {
            net.unfamily.iskautils.network.ModMessages.sendStructurePlacerMachineRedstoneModePacket(machinePos, backward);
            playButtonSound();
        }
    }

    private boolean isShiftDownNow() {
        if (this.minecraft == null) return false;
        var window = this.minecraft.getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT) || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    private boolean isCtrlDownNow() {
        if (this.minecraft == null) return false;
        var window = this.minecraft.getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL) || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL);
    }

    private boolean isAltDownNow() {
        if (this.minecraft == null) return false;
        var window = this.minecraft.getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_ALT) || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_ALT);
    }
    
    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, GUI_WIDTH, GUI_HEIGHT);
        renderEnergyBar(guiGraphics);
    }
    
    private void renderEnergyBar(GuiGraphicsExtractor guiGraphics) {
        // Position energy bar more internally and centered between left buttons (Select and Rotate)
        // Select button is at X=20, so center the energy bar between it and the left edge
        int energyBarX = this.leftPos + ((20 - ENERGY_BAR_WIDTH) / 2); // Centered between left edge and Select button
        
        // Center vertically between top row (Y=25) and bottom row (Y=70)
        // Top button: Y=25, height=20, so bottom = 45
        // Bottom button: Y=70, so center between 45 and 70 = 57.5, minus half bar height
        int energyBarY = this.topPos + 57 - (ENERGY_BAR_HEIGHT / 2); // Centered between button rows
        
        // Always draw empty energy bar background (right half of texture - pixels 8-15)
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ENERGY_BAR, energyBarX, energyBarY,
                       8.0F, 0.0F, // Source: right half starts at x=8 (empty part)
                       ENERGY_BAR_WIDTH, ENERGY_BAR_HEIGHT,
                       16, 32); // Total texture size: 16x32
        
        // Calculate energy fill percentage and draw filled part using synced data
        int energy = this.menu.getEnergyStored();
        int maxEnergy = this.menu.getMaxEnergyStored();
        
        if (energy > 0 && maxEnergy > 0) {
            int energyHeight = (energy * ENERGY_BAR_HEIGHT) / maxEnergy;
            int energyY = energyBarY + (ENERGY_BAR_HEIGHT - energyHeight);
            
            // Draw filled energy bar (left half of texture - pixels 0-7, from bottom up)
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ENERGY_BAR, energyBarX, energyY,
                           0.0F, (float)(ENERGY_BAR_HEIGHT - energyHeight), // Source: left half (charged part), from bottom
                           ENERGY_BAR_WIDTH, energyHeight,
                           16, 32); // Total texture size: 16x32
        }
    }
    
    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        renderGhostItems(guiGraphics);
        renderEnergyTooltip(guiGraphics, mouseX, mouseY);
        if (redstoneModeButton != null && redstoneModeButton.isMouseOver(mouseX, mouseY)) {
            MachineGuiButtons.renderTooltipLine(
                    guiGraphics, font, mouseX, mouseY,
                    MachineGuiButtons.redstoneTooltip(menu.getRedstoneMode(), true));
        }
    }
    
    /**
     * Renders ghost items (semi-transparent) in slots that have filters but are empty
     */
    private void renderGhostItems(GuiGraphicsExtractor guiGraphics) {
        for (int slot = 0; slot < 27; slot++) { // Only machine slots (first 27)
            if (this.menu.hasGhostFilter(slot)) {
                // Get the actual slot to check if it's empty
                net.minecraft.world.inventory.Slot guiSlot = this.menu.getSlot(slot);
                if (guiSlot.getItem().isEmpty()) {
                    // Slot is empty but has a ghost filter - render the ghost item
                    ItemStack ghostFilter = this.menu.getGhostFilter(slot);
                    if (!ghostFilter.isEmpty()) {
                        renderGhostItem(guiGraphics, ghostFilter, guiSlot.x, guiSlot.y);
                    }
                }
            }
        }
    }
    
    /**
     * Renders a single ghost item (semi-transparent) at the specified position
     */
    private void renderGhostItem(GuiGraphicsExtractor guiGraphics, ItemStack itemStack, int x, int y) {
        GhostItemRenderer.render(guiGraphics, itemStack, this.leftPos + x, this.topPos + y, GuiGhostItem.DEFAULT_ARGB);
    }

    private void renderEnergyTooltip(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        // Use same position calculation as in renderEnergyBar
        int energyBarX = this.leftPos + ((20 - ENERGY_BAR_WIDTH) / 2); // Centered between left edge and Select button
        int energyBarY = this.topPos + 57 - (ENERGY_BAR_HEIGHT / 2); // Centered between button rows
        
        // Check if mouse is over energy bar
        if (mouseX >= energyBarX && mouseX <= energyBarX + ENERGY_BAR_WIDTH &&
            mouseY >= energyBarY && mouseY <= energyBarY + ENERGY_BAR_HEIGHT) {
            
            // Use synced data for tooltip
            int energy = this.menu.getEnergyStored();
            int maxEnergy = this.menu.getMaxEnergyStored();
            
            Component tooltip = Component.literal(String.format("%,d / %,d RF", energy, maxEnergy));
            guiGraphics.setTooltipForNextFrame(this.font, java.util.List.of(tooltip.getVisualOrderText()), DefaultTooltipPositioner.INSTANCE, mouseX, mouseY, true);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        // Draw the title (centered)
        Component titleComponent = Component.translatable("block.iska_utils.structure_placer_machine");
        String title = titleComponent.getString();
        int titleX = (this.imageWidth - this.font.width(title)) / 2;
        guiGraphics.text(this.font, Component.literal(title), titleX, 6, GuiTextColors.TITLE, false);
        
        // Draw selected structure text (centered between button rows)
        renderSelectedStructureText(guiGraphics);
    }
    
    private void renderSelectedStructureText(GuiGraphicsExtractor guiGraphics) {
        // Use the new cached structure method from the menu instead of directly accessing block entity
        String selectedStructure = this.menu.getCachedSelectedStructure();
        
        // If we have a structure ID, try to get the display name
        String displayName = "";
        if (!selectedStructure.isEmpty()) {
            var structure = net.unfamily.iskalib.structure.StructureLoader.getStructure(selectedStructure);
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
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().scale(scale, scale);
        
        // Calculate scaled positions
        int scaledFirstLineY = Math.round(firstLineY / scale);
        int scaledSecondLineY = Math.round(secondLineY / scale);
        
        // Draw first line: "Selected Structure:" (centered, dark color)
        int scaledLabelWidth = this.font.width(labelText);
        int scaledLabelX = Math.round((this.imageWidth / scale - scaledLabelWidth) / 2);
        guiGraphics.text(this.font, Component.literal(labelText), scaledLabelX, scaledFirstLineY, GuiTextColors.TITLE, false);
        
        // Draw second line: structure name or "None" (centered, colored)
        int scaledStructureWidth = this.font.width(structureText);
        int scaledStructureX = Math.round((this.imageWidth / scale - scaledStructureWidth) / 2);
        int structureColor = displayName.isEmpty() ? GuiTextColors.STRUCTURE_MISSING : GuiTextColors.STRUCTURE_OK;
        guiGraphics.text(this.font, Component.literal(structureText), scaledStructureX, scaledSecondLineY, structureColor, false);
        
        guiGraphics.pose().popMatrix();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 1 && redstoneModeButton != null && redstoneModeButton.isMouseOver(event.x(), event.y())) {
            onRedstoneModePressed(true);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
    
    /**
     * Plays button click sound
     */
    private void playButtonSound() {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(
                net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }
} 
package net.unfamily.iskautils.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.core.BlockPos;

/**
 * Screen per la GUI del Temporal Overclocker
 */
public class TemporalOverclockerScreen extends AbstractContainerScreen<TemporalOverclockerMenu> {
    
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath("iska_utils", "textures/gui/backgrounds/temporal_overclocker.png");
    private static final ResourceLocation ENERGY_BAR = ResourceLocation.fromNamespaceAndPath("iska_utils", "textures/gui/energy_bar.png");
    private static final ResourceLocation ENTRY_TEXTURE = ResourceLocation.fromNamespaceAndPath("iska_utils", "textures/gui/entry_wide.png");
    private static final ResourceLocation SCROLLBAR_TEXTURE = ResourceLocation.fromNamespaceAndPath("iska_utils", "textures/gui/scrollbar.png");
    private static final ResourceLocation MEDIUM_BUTTONS = ResourceLocation.fromNamespaceAndPath("iska_utils", "textures/gui/medium_buttons.png");
    private static final ResourceLocation REDSTONE_GUI = ResourceLocation.fromNamespaceAndPath("iska_utils", "textures/gui/redstone_gui.png");
    private static final ResourceLocation SINGLE_SLOT_TEXTURE = ResourceLocation.fromNamespaceAndPath("iska_utils", "textures/gui/single_slot.png");
    
    // GUI dimensions (based on temporal_overclocker.png: 200x200)
    private static final int GUI_WIDTH = 200;
    private static final int GUI_HEIGHT = 200;
    
    // Energy bar dimensions
    private static final int ENERGY_BAR_WIDTH = 8;
    private static final int ENERGY_BAR_HEIGHT = 32;
    
    // Entry dimensions (entry_wide is 140x24)
    private static final int ENTRY_WIDTH = 140;
    private static final int ENTRY_HEIGHT = 24; // entry_wide is 24px high
    private static final int ENTRIES_START_X = (GUI_WIDTH - ENTRY_WIDTH) / 2; // Centered: (200-140)/2 = 30
    private static final int ENTRIES_START_Y = 30;
    // Calculate max entries: GUI_HEIGHT (200) - ENTRIES_START_Y (30) - button height (20) - spacing (5) - bottom margin (5) = 140px
    // With entry height 24px: 140 / 24 = 5.83, so max 5 entries
    private static final int VISIBLE_ENTRIES = 5;
    
    // Scrollbar constants
    private static final int SCROLLBAR_WIDTH = 8;
    private static final int SCROLLBAR_HEIGHT = 34;
    private static final int HANDLE_SIZE = 8;
    private static final int SCROLLBAR_X = ENTRIES_START_X + ENTRY_WIDTH + 4; // 4 pixel margin
    private static final int BUTTON_UP_Y = ENTRIES_START_Y;
    private static final int SCROLLBAR_Y = ENTRIES_START_Y + HANDLE_SIZE;
    private static final int BUTTON_DOWN_Y = SCROLLBAR_Y + SCROLLBAR_HEIGHT;
    
    // Close button
    private Button closeButton;
    private static final int CLOSE_BUTTON_Y = 5;
    private static final int CLOSE_BUTTON_SIZE = 12;
    private static final int CLOSE_BUTTON_X = GUI_WIDTH - CLOSE_BUTTON_SIZE - 5;
    
    // Custom redstone mode button
    private int redstoneModeButtonX, redstoneModeButtonY;
    private static final int REDSTONE_BUTTON_SIZE = 16;
    
    // Custom persistent mode button
    private int persistentModeButtonX, persistentModeButtonY;
    private static final int PERSISTENT_BUTTON_SIZE = 16;
    
    // Acceleration factor button (vanilla button)
    private Button accelerationButton;
    
    // Entry buttons (vanilla buttons for each visible entry)
    private final java.util.List<Button> highlightButtons = new java.util.ArrayList<>();
    private final java.util.List<Button> removeButtons = new java.util.ArrayList<>();
    
    // Scrolling variables
    private int scrollOffset = 0;
    private java.util.List<BlockPos> linkedBlocks = new java.util.ArrayList<>();
    private boolean isDraggingHandle = false;
    private int dragStartY = 0;
    private int dragStartScrollOffset = 0;
    
    // Cache per evitare il lampeggiamento dei tooltip
    private int lastScrollOffset = -1;
    private int lastLinkedBlocksSize = -1;
    
    public TemporalOverclockerScreen(TemporalOverclockerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Center the GUI on screen
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        
        // Close button - top right
        closeButton = Button.builder(Component.literal("✕"), 
                                    button -> {
                                        playButtonSound();
                                        this.onClose();
                                    })
                           .bounds(this.leftPos + CLOSE_BUTTON_X, this.topPos + CLOSE_BUTTON_Y, 
                                  CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE)
                           .build();
        addRenderableWidget(closeButton);
        
        // Redstone mode button position (right side, centered in the space between entries end and right edge)
        // Entries end at: ENTRIES_START_X + ENTRY_WIDTH = 30 + 140 = 170
        // Available space on right: GUI_WIDTH - 170 = 200 - 170 = 30px
        int entriesEndX = ENTRIES_START_X + ENTRY_WIDTH; // 170px
        int rightSpace = GUI_WIDTH - entriesEndX; // 30px
        
        // Center the two buttons vertically with 2px spacing between them
        int totalButtonsHeight = REDSTONE_BUTTON_SIZE + 2 + PERSISTENT_BUTTON_SIZE; // 16 + 2 + 16 = 34px
        int buttonsStartY = this.topPos + (GUI_HEIGHT - totalButtonsHeight) / 2;
        
        // Redstone mode button (top)
        this.redstoneModeButtonX = this.leftPos + entriesEndX + (rightSpace - REDSTONE_BUTTON_SIZE) / 2;
        this.redstoneModeButtonY = buttonsStartY;
        
        // Persistent mode button (bottom, 2px below redstone)
        this.persistentModeButtonX = this.leftPos + entriesEndX + (rightSpace - PERSISTENT_BUTTON_SIZE) / 2;
        this.persistentModeButtonY = buttonsStartY + REDSTONE_BUTTON_SIZE + 2;
        
        // Acceleration button position (below entries, centered)
        int entriesEndY = ENTRIES_START_Y + (VISIBLE_ENTRIES * ENTRY_HEIGHT);
        int accelerationButtonY = entriesEndY + 5; // 5 pixel spacing below entries
        // Button will end at: accelerationButtonY + 20 = entriesEndY + 25
        // With 5 entries: 30 + (5 * 24) + 25 = 30 + 120 + 25 = 175, leaving 25px margin from bottom (200 - 175)
        int accelerationButtonWidth = 100;
        int accelerationButtonHeight = 20;
        // Center button horizontally with entries
        int accelerationButtonX = ENTRIES_START_X + (ENTRY_WIDTH - accelerationButtonWidth) / 2;
        
        // Create vanilla button with text
        this.accelerationButton = Button.builder(
            Component.literal(""), // Text will be updated in containerTick
            button -> {
                // Left click handler (increase) - handled in mouseClicked
            }
        ).bounds(
            this.leftPos + accelerationButtonX,
            this.topPos + accelerationButtonY,
            accelerationButtonWidth,
            accelerationButtonHeight
        ).build();
        
        this.addRenderableWidget(this.accelerationButton);
    }
    
    @Override
    public void containerTick() {
        super.containerTick();
        
        // Update cached linked blocks from server (like DeepDrawerExtractor does with filters)
        menu.updateCachedLinkedBlocks();
        
        // Get cached data for linked blocks
        java.util.List<BlockPos> cachedLinkedBlocks = menu.getCachedLinkedBlocks();
        
        // Update linked blocks list
        this.linkedBlocks = new java.util.ArrayList<>(cachedLinkedBlocks);
        
        // Ensure scrollOffset is valid
        if (linkedBlocks.size() > VISIBLE_ENTRIES) {
            scrollOffset = Math.max(0, Math.min(scrollOffset, linkedBlocks.size() - VISIBLE_ENTRIES));
        } else {
            scrollOffset = 0;
        }
        
        // Aggiorna il testo del pulsante accelerazione
        if (this.accelerationButton != null) {
            int accelerationFactor = this.menu.getAccelerationFactor();
            int percentage = accelerationFactor * 100; // 20 = 2000%
            String text = String.format("Overclock: %d%%", percentage);
            this.accelerationButton.setMessage(Component.literal(text));
        }
        
        // Update entry buttons SOLO se necessario (evita lampeggiamento tooltip)
        if (scrollOffset != lastScrollOffset || linkedBlocks.size() != lastLinkedBlocksSize) {
            updateEntryButtons();
            lastScrollOffset = scrollOffset;
            lastLinkedBlocksSize = linkedBlocks.size();
        }
    }
    
    /**
     * Updates entry buttons (highlight and remove) for visible entries
     */
    private void updateEntryButtons() {
        // Remove all existing buttons
        highlightButtons.forEach(this::removeWidget);
        removeButtons.forEach(this::removeWidget);
        highlightButtons.clear();
        removeButtons.clear();
        
        // Create buttons for each visible entry
        int buttonSize = 12;
        int buttonSpacing = 2;
        
        for (int i = 0; i < VISIBLE_ENTRIES; i++) {
            int entryIndex = scrollOffset + i;
            if (entryIndex >= linkedBlocks.size()) continue;
            
            int entryX = this.leftPos + ENTRIES_START_X;
            int entryY = this.topPos + ENTRIES_START_Y + i * ENTRY_HEIGHT;
            int buttonY = entryY + (ENTRY_HEIGHT - buttonSize) / 2;
            
            BlockPos linkedPos = linkedBlocks.get(entryIndex);
            
            // Button margin from right edge
            int buttonMargin = 3; // 3 pixels from right edge
            
            // Highlight button (circle with dot)
            int highlightButtonX = entryX + ENTRY_WIDTH - buttonMargin - buttonSize - buttonSize - buttonSpacing;
            Button highlightButton = Button.builder(Component.literal("◎"), 
                button -> {
                    net.unfamily.iskautils.network.ModMessages.sendTemporalOverclockerHighlightBlockPacket(linkedPos);
                    playButtonSound();
                })
                .bounds(highlightButtonX, buttonY, buttonSize, buttonSize)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(
                    Component.translatable("gui.iska_utils.temporal_overclocker.show")))
                .build();
            highlightButtons.add(highlightButton);
            this.addRenderableWidget(highlightButton);
            
            // Remove button (X)
            int removeButtonX = entryX + ENTRY_WIDTH - buttonMargin - buttonSize;
            Button removeButton = Button.builder(Component.literal("✕"), 
                button -> {
                    net.unfamily.iskautils.network.ModMessages.sendTemporalOverclockerRemoveLinkPacket(
                        this.menu.getSyncedBlockPos(), linkedPos);
                    playButtonSound();
                })
                .bounds(removeButtonX, buttonY, buttonSize, buttonSize)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(
                    Component.translatable("gui.iska_utils.temporal_overclocker.disconnect")))
                .build();
            removeButtons.add(removeButton);
            this.addRenderableWidget(removeButton);
        }
    }
    
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // Draw the background texture
        guiGraphics.blit(BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, GUI_WIDTH, GUI_HEIGHT);
        
        // Draw the energy bar
        renderEnergyBar(guiGraphics);
        
        // Draw the custom persistent mode button
        renderPersistentModeButton(guiGraphics, mouseX, mouseY);
        
        // Draw the custom redstone mode button
        renderRedstoneModeButton(guiGraphics, mouseX, mouseY);
        
        // Acceleration button is rendered automatically by vanilla Button
        
        // Draw entries (always draw backgrounds, content only if blocks are linked)
        renderEntries(guiGraphics, mouseX, mouseY);
        
        // Draw scrollbar (only if there are more entries than visible)
        if (linkedBlocks.size() > VISIBLE_ENTRIES) {
            renderScrollbar(guiGraphics, mouseX, mouseY);
        }
    }
    
    private void renderEnergyBar(GuiGraphics guiGraphics) {
        // Position energy bar on the left side, centered in the space between left edge and entries start
        // ENTRIES_START_X is the space available (30px), center the bar in that space
        int energyBarX = this.leftPos + (ENTRIES_START_X - ENERGY_BAR_WIDTH) / 2;
        int energyBarY = this.topPos + (GUI_HEIGHT / 2) - (ENERGY_BAR_HEIGHT / 2);
        
        // Always draw empty energy bar background
        guiGraphics.blit(ENERGY_BAR, energyBarX, energyBarY, 
                       8, 0, // Source: right half starts at x=8 (empty part)
                       ENERGY_BAR_WIDTH, ENERGY_BAR_HEIGHT, 
                       16, 32); // Total texture size: 16x32
        
        // Calculate energy fill percentage
        int energy = this.menu.getEnergyStored();
        int maxEnergy = this.menu.getMaxEnergyStored();
        
        if (energy > 0 && maxEnergy > 0) {
            int energyHeight = (energy * ENERGY_BAR_HEIGHT) / maxEnergy;
            int energyY = energyBarY + (ENERGY_BAR_HEIGHT - energyHeight);
            
            // Draw filled energy bar
            guiGraphics.blit(ENERGY_BAR, energyBarX, energyY,
                           0, ENERGY_BAR_HEIGHT - energyHeight, // Source: left half (charged part), from bottom
                           ENERGY_BAR_WIDTH, energyHeight,
                           16, 32); // Total texture size: 16x32
        }
    }
    
    /**
     * Renders linked block entries
     */
    private void renderEntries(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Always draw entry backgrounds for visible entries
        for (int i = 0; i < VISIBLE_ENTRIES; i++) {
            int entryIndex = scrollOffset + i;
            int entryX = this.leftPos + ENTRIES_START_X;
            int entryY = this.topPos + ENTRIES_START_Y + i * ENTRY_HEIGHT;
            
            // Draw entry background
            guiGraphics.blit(ENTRY_TEXTURE, entryX, entryY, 0, 0, 
                           ENTRY_WIDTH, ENTRY_HEIGHT, ENTRY_WIDTH, ENTRY_HEIGHT);
            
            // If entry has a linked block, show information
            if (entryIndex < linkedBlocks.size()) {
                BlockPos linkedPos = linkedBlocks.get(entryIndex);
                renderLinkedBlockEntry(guiGraphics, entryX, entryY, linkedPos, mouseX, mouseY);
            }
        }
    }
    
    /**
     * Renders a single linked block entry
     */
    private void renderLinkedBlockEntry(GuiGraphics guiGraphics, int entryX, int entryY, BlockPos pos, int mouseX, int mouseY) {
        // Draw single slot for block item
        int slotX = entryX + 3; // 3 pixels from left edge
        int slotY = entryY + 3; // 3 pixels from top edge
        guiGraphics.blit(SINGLE_SLOT_TEXTURE, slotX, slotY, 0, 0, 18, 18, 18, 18);
        
        // Get block state and render as item if possible
        if (this.minecraft != null && this.minecraft.level != null) {
            net.minecraft.world.level.block.state.BlockState blockState = this.minecraft.level.getBlockState(pos);
            if (blockState != null && !blockState.isAir()) {
                net.minecraft.world.item.ItemStack blockStack = new net.minecraft.world.item.ItemStack(blockState.getBlock().asItem());
                if (!blockStack.isEmpty()) {
                    guiGraphics.renderItem(blockStack, slotX + 1, slotY + 1);
                }
                
                // Show block name (truncated if too long)
                int textX = slotX + 18 + 6; // After slot + 6 pixel margin
                int textY = entryY + (ENTRY_HEIGHT - this.font.lineHeight) / 2; // Centered vertically
                
                // Calculate available width: from text start to buttons start
                int buttonSize = 12;
                int buttonSpacing = 2;
                int buttonMargin = 3;
                int buttonsStartX = entryX + ENTRY_WIDTH - buttonMargin - buttonSize - buttonSize - buttonSpacing;
                int availableWidth = buttonsStartX - textX - 4; // 4 pixel margin before buttons
                
                // Get block name
                Component blockName = blockState.getBlock().getName();
                String blockNameString = blockName.getString();
                
                // Truncate if too long
                int textWidth = this.font.width(blockNameString);
                if (textWidth > availableWidth) {
                    // Truncate and add "..."
                    String truncated = this.font.plainSubstrByWidth(blockNameString, availableWidth - this.font.width("..."));
                    blockNameString = truncated + "...";
                }
                
                guiGraphics.drawString(this.font, blockNameString, textX, textY, 0x404040, false);
            }
        }
        
        // Buttons are now vanilla Button widgets, rendered automatically
    }
    
    /**
     * Renderizza il pulsante redstone mode
     */
    private void renderPersistentModeButton(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Check if mouse is over the button
        boolean isHovered = mouseX >= this.persistentModeButtonX && mouseX <= this.persistentModeButtonX + PERSISTENT_BUTTON_SIZE &&
                           mouseY >= this.persistentModeButtonY && mouseY <= this.persistentModeButtonY + PERSISTENT_BUTTON_SIZE;
        
        // Draw button background (normal or highlighted)
        int textureY = isHovered ? 16 : 0; // Highlighted version is below the normal one
        guiGraphics.blit(MEDIUM_BUTTONS, this.persistentModeButtonX, this.persistentModeButtonY, 
                        0, textureY, PERSISTENT_BUTTON_SIZE, PERSISTENT_BUTTON_SIZE, 
                        96, 96); // Correct texture size: 96x96
        
        // Get current persistent mode from menu
        boolean isPersistent = this.menu.isPersistentMode();
        
        // Draw the appropriate icon (12x12 pixels, centered in the 16x16 button)
        int iconX = this.persistentModeButtonX + 2; // Center: (16-12)/2 = 2
        int iconY = this.persistentModeButtonY + 2; // Center: (16-12)/2 = 2
        int iconSize = 12;
        
        if (isPersistent) {
            // ON: Lodestone icon (fixed/permanent point)
            net.minecraft.world.item.ItemStack lodestone = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.LODESTONE);
            renderScaledItem(guiGraphics, lodestone, iconX, iconY, iconSize);
        } else {
            // OFF: Brush icon (auto-remove)
            net.minecraft.world.item.ItemStack brush = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BRUSH);
            renderScaledItem(guiGraphics, brush, iconX, iconY, iconSize);
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
            case 4 -> {
                // DISABLED mode: Barrier icon
                net.minecraft.world.item.ItemStack barrier = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BARRIER);
                renderScaledItem(guiGraphics, barrier, iconX, iconY, iconSize);
            }
        }
    }
    
    /**
     * Renders an item scaled to the specified size
     */
    private void renderScaledItem(GuiGraphics guiGraphics, net.minecraft.world.item.ItemStack itemStack, int x, int y, int size) {
        guiGraphics.pose().pushPose();
        float scale = (float) size / 16.0f;
        guiGraphics.pose().translate(x, y, 0);
        guiGraphics.pose().scale(scale, scale, 1.0f);
        guiGraphics.renderItem(itemStack, 0, 0);
        guiGraphics.pose().popPose();
    }
    
    /**
     * Renders a texture scaled to the specified size (like an item)
     */
    private void renderScaledTexture(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y, int size) {
        guiGraphics.pose().pushPose();
        float scale = (float) size / 16.0f;
        guiGraphics.pose().translate(x, y, 0);
        guiGraphics.pose().scale(scale, scale, 1.0f);
        guiGraphics.blit(texture, 0, 0, 0, 0, 16, 16, 16, 16);
        guiGraphics.pose().popPose();
    }
    
    
    /**
     * Renderizza la scrollbar
     */
    private void renderScrollbar(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Only show scrollbar if there are more entries than visible
        if (linkedBlocks.size() <= VISIBLE_ENTRIES) return;
        
        int scrollbarX = this.leftPos + SCROLLBAR_X;
        int scrollbarY = this.topPos + SCROLLBAR_Y;
        int buttonUpY = this.topPos + BUTTON_UP_Y;
        int buttonDownY = this.topPos + BUTTON_DOWN_Y;
        
        // Disegna la scrollbar completa
        guiGraphics.blit(SCROLLBAR_TEXTURE, scrollbarX, scrollbarY, 0, 0, 
                        SCROLLBAR_WIDTH, SCROLLBAR_HEIGHT, 32, 34);
        
        // Pulsante SU
        boolean upHovered = mouseX >= scrollbarX && mouseX < scrollbarX + HANDLE_SIZE &&
                           mouseY >= buttonUpY && mouseY < buttonUpY + HANDLE_SIZE;
        int upTextureY = upHovered ? HANDLE_SIZE : 0;
        guiGraphics.blit(SCROLLBAR_TEXTURE, scrollbarX, buttonUpY, 
                        SCROLLBAR_WIDTH * 2, upTextureY, HANDLE_SIZE, HANDLE_SIZE, 32, 34);
        
        // DOWN button
        boolean downHovered = mouseX >= scrollbarX && mouseX < scrollbarX + HANDLE_SIZE &&
                             mouseY >= buttonDownY && mouseY < buttonDownY + HANDLE_SIZE;
        int downTextureY = downHovered ? HANDLE_SIZE : 0;
        guiGraphics.blit(SCROLLBAR_TEXTURE, scrollbarX, buttonDownY, 
                        SCROLLBAR_WIDTH * 3, downTextureY, HANDLE_SIZE, HANDLE_SIZE, 32, 34);
        
        // Handle
        float scrollRatio = 0;
        if (linkedBlocks.size() > VISIBLE_ENTRIES) {
            scrollRatio = (float) scrollOffset / (linkedBlocks.size() - VISIBLE_ENTRIES);
        }
        int handleY = scrollbarY + (int)(scrollRatio * (SCROLLBAR_HEIGHT - HANDLE_SIZE));
        
        boolean handleHovered = mouseX >= scrollbarX && mouseX < scrollbarX + HANDLE_SIZE &&
                               mouseY >= handleY && mouseY < handleY + HANDLE_SIZE;
        int handleTextureY = handleHovered ? HANDLE_SIZE : 0;
        guiGraphics.blit(SCROLLBAR_TEXTURE, scrollbarX, handleY, 
                        SCROLLBAR_WIDTH, handleTextureY, HANDLE_SIZE, HANDLE_SIZE, 32, 34);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Handle acceleration button click FIRST (for all button types and modifiers)
        if (this.accelerationButton != null && this.accelerationButton.isMouseOver(mouseX, mouseY)) {
            boolean shift = hasShiftDown();
            boolean ctrl = hasControlDown();
            boolean alt = hasAltDown();
            
            if (button == 2) {
                // Middle click: set to default
                onAccelerationSetDefault();
                return true;
            } else if (button == 0) {
                // Left click
                if (shift) {
                    // Shift + left: set to max
                    onAccelerationSetMax();
                } else if (ctrl || alt) {
                    // Ctrl/Alt + left: increase by 5
                    onAccelerationIncreaseBy5();
                } else {
                    // Normal left: increase by 1
                    onAccelerationIncrease();
                }
                return true;
            } else if (button == 1) {
                // Right click
                if (shift) {
                    // Shift + right: set to min
                    onAccelerationSetMin();
                } else if (ctrl || alt) {
                    // Ctrl/Alt + right: decrease by 5
                    onAccelerationDecreaseBy5();
                } else {
                    // Normal right: decrease by 1
                    onAccelerationDecrease();
                }
                return true;
            }
        }
        
        if (button == 0) { // Left click
            // Handle scrollbar clicks
            if (handleScrollButtonClick(mouseX, mouseY)) {
                return true;
            }
            
            // Handle handle drag start
            if (handleHandleClick(mouseX, mouseY)) {
                return true;
            }
            
            // Handle scrollbar area clicks
            if (handleScrollbarClick(mouseX, mouseY)) {
                return true;
            }
            
            // Entry buttons remove sono gestiti da vanilla Button widgets
            
            // Handle persistent mode button click
            if (mouseX >= this.persistentModeButtonX && mouseX <= this.persistentModeButtonX + PERSISTENT_BUTTON_SIZE &&
                mouseY >= this.persistentModeButtonY && mouseY <= this.persistentModeButtonY + PERSISTENT_BUTTON_SIZE) {
                onPersistentModePressed();
                return true;
            }
            
            // Handle redstone mode button click
            if (mouseX >= this.redstoneModeButtonX && mouseX <= this.redstoneModeButtonX + REDSTONE_BUTTON_SIZE &&
                mouseY >= this.redstoneModeButtonY && mouseY <= this.redstoneModeButtonY + REDSTONE_BUTTON_SIZE) {
                onRedstoneModePressed();
                return true;
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    private void onAccelerationIncrease() {
        BlockPos machinePos = this.menu.getSyncedBlockPos();
        if (!machinePos.equals(BlockPos.ZERO)) {
            net.unfamily.iskautils.network.ModMessages.sendTemporalOverclockerAccelerationChangePacket(machinePos, 0);
        }
    }
    
    private void onAccelerationDecrease() {
        BlockPos machinePos = this.menu.getSyncedBlockPos();
        if (!machinePos.equals(BlockPos.ZERO)) {
            net.unfamily.iskautils.network.ModMessages.sendTemporalOverclockerAccelerationChangePacket(machinePos, 1);
        }
    }
    
    private void onAccelerationIncreaseBy5() {
        BlockPos machinePos = this.menu.getSyncedBlockPos();
        if (!machinePos.equals(BlockPos.ZERO)) {
            net.unfamily.iskautils.network.ModMessages.sendTemporalOverclockerAccelerationChangePacket(machinePos, 2);
        }
    }
    
    private void onAccelerationDecreaseBy5() {
        BlockPos machinePos = this.menu.getSyncedBlockPos();
        if (!machinePos.equals(BlockPos.ZERO)) {
            net.unfamily.iskautils.network.ModMessages.sendTemporalOverclockerAccelerationChangePacket(machinePos, 3);
        }
    }
    
    private void onAccelerationSetMax() {
        BlockPos machinePos = this.menu.getSyncedBlockPos();
        if (!machinePos.equals(BlockPos.ZERO)) {
            net.unfamily.iskautils.network.ModMessages.sendTemporalOverclockerAccelerationChangePacket(machinePos, 4);
        }
    }
    
    private void onAccelerationSetMin() {
        BlockPos machinePos = this.menu.getSyncedBlockPos();
        if (!machinePos.equals(BlockPos.ZERO)) {
            net.unfamily.iskautils.network.ModMessages.sendTemporalOverclockerAccelerationChangePacket(machinePos, 5);
        }
    }
    
    private void onAccelerationSetDefault() {
        BlockPos machinePos = this.menu.getSyncedBlockPos();
        if (!machinePos.equals(BlockPos.ZERO)) {
            net.unfamily.iskautils.network.ModMessages.sendTemporalOverclockerAccelerationChangePacket(machinePos, 6);
        }
    }
    
    private void onPersistentModePressed() {
        BlockPos machinePos = this.menu.getSyncedBlockPos();
        if (!machinePos.equals(BlockPos.ZERO)) {
            net.unfamily.iskautils.network.ModMessages.sendTemporalOverclockerTogglePersistentPacket(machinePos);
            playButtonSound();
        }
    }
    
    private void onRedstoneModePressed() {
        BlockPos machinePos = this.menu.getSyncedBlockPos();
        if (!machinePos.equals(BlockPos.ZERO)) {
            net.unfamily.iskautils.network.ModMessages.sendTemporalOverclockerRedstoneModePacket(machinePos);
        }
    }
    
    private boolean handleScrollButtonClick(double mouseX, double mouseY) {
        if (linkedBlocks.size() <= VISIBLE_ENTRIES) return false;
        
        int guiX = this.leftPos;
        int guiY = this.topPos;
        
        // UP button
        if (mouseX >= guiX + SCROLLBAR_X && mouseX < guiX + SCROLLBAR_X + SCROLLBAR_WIDTH &&
            mouseY >= guiY + BUTTON_UP_Y && mouseY < guiY + BUTTON_UP_Y + HANDLE_SIZE) {
            scrollUp();
            return true;
        }
        
        // DOWN button
        if (mouseX >= guiX + SCROLLBAR_X && mouseX < guiX + SCROLLBAR_X + SCROLLBAR_WIDTH &&
            mouseY >= guiY + BUTTON_DOWN_Y && mouseY < guiY + BUTTON_DOWN_Y + HANDLE_SIZE) {
            scrollDown();
            return true;
        }
        
        return false;
    }
    
    private boolean handleHandleClick(double mouseX, double mouseY) {
        if (linkedBlocks.size() <= VISIBLE_ENTRIES) return false;
        
        int scrollbarX = this.leftPos + SCROLLBAR_X;
        int scrollbarY = this.topPos + SCROLLBAR_Y;
        
        float scrollRatio = (float) scrollOffset / (linkedBlocks.size() - VISIBLE_ENTRIES);
        int handleY = scrollbarY + (int)(scrollRatio * (SCROLLBAR_HEIGHT - HANDLE_SIZE));
        
        if (mouseX >= scrollbarX && mouseX < scrollbarX + HANDLE_SIZE &&
            mouseY >= handleY && mouseY < handleY + HANDLE_SIZE) {
            
            isDraggingHandle = true;
            dragStartY = (int) mouseY;
            dragStartScrollOffset = scrollOffset;
            playButtonSound();
            return true;
        }
        return false;
    }
    
    private boolean handleScrollbarClick(double mouseX, double mouseY) {
        if (linkedBlocks.size() <= VISIBLE_ENTRIES) return false;
        
        int scrollbarX = this.leftPos + SCROLLBAR_X;
        int scrollbarY = this.topPos + SCROLLBAR_Y;
        
        if (mouseX >= scrollbarX && mouseX < scrollbarX + SCROLLBAR_WIDTH &&
            mouseY >= scrollbarY && mouseY < scrollbarY + SCROLLBAR_HEIGHT) {
            
            float clickRatio = (float)(mouseY - scrollbarY) / SCROLLBAR_HEIGHT;
            clickRatio = Math.max(0, Math.min(1, clickRatio));
            
            int newScrollOffset = (int)(clickRatio * (linkedBlocks.size() - VISIBLE_ENTRIES));
            newScrollOffset = Math.max(0, Math.min(linkedBlocks.size() - VISIBLE_ENTRIES, newScrollOffset));
            
            if (newScrollOffset != scrollOffset) {
                scrollOffset = newScrollOffset;
                playButtonSound();
            }
            return true;
        }
        return false;
    }
    
    // Entry click handling is now done by vanilla Button widgets
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && isDraggingHandle) {
            isDraggingHandle = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && isDraggingHandle && linkedBlocks.size() > VISIBLE_ENTRIES) {
            int deltaY = (int) mouseY - dragStartY;
            float scrollRatio = (float) deltaY / (SCROLLBAR_HEIGHT - HANDLE_SIZE);
            
            int newScrollOffset = dragStartScrollOffset + (int)(scrollRatio * (linkedBlocks.size() - VISIBLE_ENTRIES));
            newScrollOffset = Math.max(0, Math.min(linkedBlocks.size() - VISIBLE_ENTRIES, newScrollOffset));
            
            scrollOffset = newScrollOffset;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (deltaY > 0) {
            return scrollUpSilent();
        } else if (deltaY < 0) {
            return scrollDownSilent();
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }
    
    private void scrollUp() {
        if (scrollUpSilent()) {
            playButtonSound();
        }
    }
    
    private void scrollDown() {
        if (scrollDownSilent()) {
            playButtonSound();
        }
    }
    
    private boolean scrollUpSilent() {
        if (linkedBlocks.size() > VISIBLE_ENTRIES && scrollOffset > 0) {
            scrollOffset--;
            return true;
        }
        return false;
    }
    
    private boolean scrollDownSilent() {
        if (linkedBlocks.size() > VISIBLE_ENTRIES && scrollOffset < linkedBlocks.size() - VISIBLE_ENTRIES) {
            scrollOffset++;
            return true;
        }
        return false;
    }
    
    private void playButtonSound() {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(
                net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }
    
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Draw the title (centered)
        Component titleComponent = Component.translatable("block.iska_utils.temporal_overclocker");
        String title = titleComponent.getString();
        int titleX = (this.imageWidth - this.font.width(title)) / 2;
        guiGraphics.drawString(this.font, title, titleX, 6, 0x404040, false);
    }
    
    private void renderEnergyTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int energyBarX = this.leftPos + (ENTRIES_START_X - ENERGY_BAR_WIDTH) / 2;
        int energyBarY = this.topPos + (GUI_HEIGHT / 2) - (ENERGY_BAR_HEIGHT / 2);
        
        if (mouseX >= energyBarX && mouseX <= energyBarX + ENERGY_BAR_WIDTH &&
            mouseY >= energyBarY && mouseY <= energyBarY + ENERGY_BAR_HEIGHT) {
            
            int energy = this.menu.getEnergyStored();
            int maxEnergy = this.menu.getMaxEnergyStored();
            
            Component tooltip = Component.literal(String.format("%,d / %,d RF", energy, maxEnergy));
            guiGraphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }
    
    private void renderPersistentModeTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        boolean isHovered = mouseX >= this.persistentModeButtonX && mouseX <= this.persistentModeButtonX + PERSISTENT_BUTTON_SIZE &&
                           mouseY >= this.persistentModeButtonY && mouseY <= this.persistentModeButtonY + PERSISTENT_BUTTON_SIZE;
        
        if (isHovered) {
            boolean isPersistent = this.menu.isPersistentMode();
            
            java.util.List<Component> tooltipLines = new java.util.ArrayList<>();
            if (isPersistent) {
                tooltipLines.add(Component.translatable("gui.iska_utils.temporal_overclocker.persistent.on"));
            } else {
                tooltipLines.add(Component.translatable("gui.iska_utils.temporal_overclocker.persistent.off"));
            }
            tooltipLines.add(Component.translatable("gui.iska_utils.temporal_overclocker.persistent.description"));
            
            guiGraphics.renderComponentTooltip(this.font, tooltipLines, mouseX, mouseY);
        }
    }
    
    private void renderRedstoneModeTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        boolean isHovered = mouseX >= this.redstoneModeButtonX && mouseX <= this.redstoneModeButtonX + REDSTONE_BUTTON_SIZE &&
                           mouseY >= this.redstoneModeButtonY && mouseY <= this.redstoneModeButtonY + REDSTONE_BUTTON_SIZE;
        
        if (isHovered) {
            int redstoneMode = this.menu.getRedstoneMode();
            
            Component tooltip = switch (redstoneMode) {
                case 0 -> Component.translatable("gui.iska_utils.generic.redstone_mode.none");
                case 1 -> Component.translatable("gui.iska_utils.generic.redstone_mode.low");
                case 2 -> Component.translatable("gui.iska_utils.generic.redstone_mode.high");
                case 3 -> Component.translatable("gui.iska_utils.generic.redstone_mode.pulse");
                case 4 -> Component.translatable("gui.iska_utils.generic.redstone_mode.disabled");
                default -> Component.literal("Unknown mode");
            };
            
            guiGraphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }
    
    private void renderAccelerationTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.accelerationButton != null && this.accelerationButton.isMouseOver(mouseX, mouseY)) {
            java.util.List<Component> tooltipLines = new java.util.ArrayList<>();
            tooltipLines.add(Component.translatable("gui.iska_utils.temporal_overclocker.acceleration_tooltip.line1"));
            tooltipLines.add(Component.translatable("gui.iska_utils.temporal_overclocker.acceleration_tooltip.line2"));
            int defaultValue = net.unfamily.iskautils.Config.temporalOverclockerAccelerationFactor;
            tooltipLines.add(Component.translatable("gui.iska_utils.temporal_overclocker.acceleration_tooltip.line3", defaultValue));
            guiGraphics.renderComponentTooltip(this.font, tooltipLines, mouseX, mouseY);
        }
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        // Render energy bar tooltip
        renderEnergyTooltip(guiGraphics, mouseX, mouseY);
        
        // Render persistent mode button tooltip
        renderPersistentModeTooltip(guiGraphics, mouseX, mouseY);
        
        // Render redstone mode button tooltip
        renderRedstoneModeTooltip(guiGraphics, mouseX, mouseY);
        
        // Render acceleration button tooltip
        renderAccelerationTooltip(guiGraphics, mouseX, mouseY);
        
        // Render tooltips
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}


package net.unfamily.iskautils.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.item.ModItems;
import net.minecraft.core.BlockPos;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

/**
 * Screen per la GUI del Temporal Overclocker
 */
public class TemporalOverclockerScreen extends AbstractContainerScreen<TemporalOverclockerMenu> {

    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath("iska_utils", "textures/gui/backgrounds/temporal_overclocker.png");
    private static final Identifier ENERGY_BAR = Identifier.fromNamespaceAndPath("iska_utils", "textures/gui/energy_bar.png");
    private static final Identifier ENTRY_TEXTURE = Identifier.fromNamespaceAndPath("iska_utils", "textures/gui/entry_wide.png");
    private static final Identifier SCROLLBAR_TEXTURE = Identifier.fromNamespaceAndPath("iska_utils", "textures/gui/scrollbar.png");
    private static final Identifier MEDIUM_BUTTONS = Identifier.fromNamespaceAndPath("iska_utils", "textures/gui/medium_buttons.png");
    private static final Identifier REDSTONE_GUI = Identifier.fromNamespaceAndPath("iska_utils", "textures/gui/redstone_gui.png");
    private static final Identifier SINGLE_SLOT_TEXTURE = Identifier.fromNamespaceAndPath("iska_utils", "textures/gui/single_slot.png");
    private static final Identifier SLOT_HIGHLIGHT_BACK =
            Identifier.withDefaultNamespace("container/slot_highlight_back");
    private static final Identifier SLOT_HIGHLIGHT_FRONT =
            Identifier.withDefaultNamespace("container/slot_highlight_front");
    
    // GUI dimensions (based on temporal_overclocker.png: 200x260)
    private static final int GUI_WIDTH = TemporalOverclockerMenu.GUI_WIDTH;
    private static final int GUI_HEIGHT = TemporalOverclockerMenu.GUI_HEIGHT;
    
    // Energy bar dimensions
    private static final int ENERGY_BAR_WIDTH = 8;
    private static final int ENERGY_BAR_HEIGHT = TemporalOverclockerMenu.ENERGY_BAR_HEIGHT;
    
    // Entry dimensions (entry_wide is 140x24)
    private static final int ENTRY_WIDTH = 140;
    private static final int ENTRY_HEIGHT = 24; // entry_wide is 24px high
    private static final int ENTRIES_START_X = (GUI_WIDTH - ENTRY_WIDTH) / 2; // Centered: (200-140)/2 = 30
    private static final int ENTRIES_START_Y = TemporalOverclockerMenu.LINKED_ENTRIES_START_Y;
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
    
    // Custom redstone / persistent mode buttons (positions derived from imageHeight)
    private static final int REDSTONE_BUTTON_SIZE = TemporalOverclockerMenu.SIDE_BUTTON_SIZE;
    private static final int PERSISTENT_BUTTON_SIZE = TemporalOverclockerMenu.SIDE_BUTTON_SIZE;
    
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
        super(menu, playerInventory, title, GUI_WIDTH, GUI_HEIGHT);
        this.inventoryLabelY = 10000;
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
        
        // Acceleration button position (below entries, centered; Y shared with machine slots)
        int accelerationButtonY = TemporalOverclockerMenu.ACCELERATION_BUTTON_Y;
        int accelerationButtonWidth = TemporalOverclockerMenu.ACCELERATION_BUTTON_WIDTH;
        int accelerationButtonHeight = TemporalOverclockerMenu.ACCELERATION_BUTTON_HEIGHT;
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
            int percentage = accelerationFactor * 100;
            boolean entropic = accelerationFactor > Config.temporalOverclockerAccelerationFactorMax;
            String pct = entropic ? "§5" + percentage + "§r" : String.valueOf(percentage);
            this.accelerationButton.setMessage(Component.literal("Overclock: " + pct + "%"));
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
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, GUI_WIDTH, GUI_HEIGHT);
        renderEnergyBar(guiGraphics);
        renderPersistentModeButton(guiGraphics, mouseX, mouseY);
        renderRedstoneModeButton(guiGraphics, mouseX, mouseY);
        renderEntries(guiGraphics, mouseX, mouseY);
        if (linkedBlocks.size() > VISIBLE_ENTRIES) {
            renderScrollbar(guiGraphics, mouseX, mouseY);
        }
        renderMachineSlots(guiGraphics);
        renderMachineSlotGhosts(guiGraphics);
    }

    private void renderMachineSlots(GuiGraphicsExtractor guiGraphics) {
        renderSlotBackground(guiGraphics, TemporalOverclockerMenu.UPGRADE_SLOT_X, TemporalOverclockerMenu.ENTROPY_ROW_Y);
        renderSlotBackground(guiGraphics, TemporalOverclockerMenu.FUEL_SLOT_X, TemporalOverclockerMenu.ENTROPY_ROW_Y);
    }

    private void renderMachineSlotGhosts(GuiGraphicsExtractor graphics) {
        renderMachineSlotGhostIfEmpty(graphics, TemporalOverclockerMenu.UPGRADE_SLOT_INDEX);
        renderMachineSlotGhostIfEmpty(graphics, TemporalOverclockerMenu.FUEL_SLOT_INDEX);
    }

    /** Same semi-transparent overlay as {@link AncientTableScreen} (26.1). */
    private void renderMachineSlotGhostIfEmpty(GuiGraphicsExtractor graphics, int slotIndex) {
        Slot slot = menu.getSlot(slotIndex);
        if (!slot.getItem().isEmpty()) {
            return;
        }
        ItemStack ghost = machineSlotGhost(slot);
        if (ghost.isEmpty()) {
            return;
        }
        int x = this.leftPos + TemporalOverclockerMenu.machineSlotItemX(slot.x);
        int y = this.topPos + TemporalOverclockerMenu.machineSlotItemY(slot.y);
        graphics.item(ghost, x, y);
        graphics.fill(
                x,
                y,
                x + TemporalOverclockerMenu.SLOT_ITEM_RENDER_SIZE,
                y + TemporalOverclockerMenu.SLOT_ITEM_RENDER_SIZE,
                0x80FFFFFF);
    }
    private static ItemStack machineSlotGhost(Slot slot) {
        if (slot.index == TemporalOverclockerMenu.UPGRADE_SLOT_INDEX) {
            return new ItemStack(ModItems.ENTROPIC_CLOCK.get());
        }
        if (slot.index == TemporalOverclockerMenu.FUEL_SLOT_INDEX) {
            return new ItemStack(ModItems.DROP_OF_ENTROPY.get());
        }
        return ItemStack.EMPTY;
    }

    private void renderMachineSlotItem(
            GuiGraphicsExtractor graphics, int x, int y, ItemStack stack, @Nullable String itemCount) {
        int seed = x + y * this.imageWidth;
        graphics.item(stack, x, y, seed);
        var font = net.neoforged.neoforge.client.extensions.common.IClientItemExtensions.of(stack)
                .getFont(stack, net.neoforged.neoforge.client.extensions.common.IClientItemExtensions.FontContext.ITEM_COUNT);
        graphics.itemDecorations(font != null ? font : this.font, stack, x, y, itemCount);
    }

    private void renderSlotBackground(GuiGraphicsExtractor guiGraphics, int slotX, int slotY) {
        guiGraphics.blit(
                RenderPipelines.GUI_TEXTURED,
                SINGLE_SLOT_TEXTURE,
                this.leftPos + slotX,
                this.topPos + slotY,
                0.0F,
                0.0F,
                18,
                18,
                18,
                18);
    }

    private void renderMachineSlotHighlight(GuiGraphicsExtractor graphics, Slot slot, Identifier sprite) {
        graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                sprite,
                TemporalOverclockerMenu.machineSlotHighlightX(slot.x),
                TemporalOverclockerMenu.machineSlotHighlightY(slot.y),
                TemporalOverclockerMenu.MACHINE_SLOT_HIGHLIGHT_SIZE,
                TemporalOverclockerMenu.MACHINE_SLOT_HIGHLIGHT_SIZE);
    }

    private static boolean isMachineSlot(Slot slot) {
        return slot.index == TemporalOverclockerMenu.UPGRADE_SLOT_INDEX
                || slot.index == TemporalOverclockerMenu.FUEL_SLOT_INDEX;
    }

    @Override
    protected void extractSlots(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Slot hovered = this.hoveredSlot;
        if (hovered != null && isMachineSlot(hovered) && hovered.isActive()) {
            renderMachineSlotHighlight(graphics, hovered, SLOT_HIGHLIGHT_BACK);
        }
        super.extractSlots(graphics, mouseX, mouseY);
        if (hovered != null && isMachineSlot(hovered) && hovered.isActive()) {
            renderMachineSlotHighlight(graphics, hovered, SLOT_HIGHLIGHT_FRONT);
        }
    }

    @Override
    protected void renderSlotContents(
            GuiGraphicsExtractor graphics, ItemStack itemStack, Slot slot, @Nullable String itemCount) {
        if (!isMachineSlot(slot)) {
            super.renderSlotContents(graphics, itemStack, slot, itemCount);
            return;
        }
        if (itemStack.isEmpty()) {
            return;
        }
        int x = TemporalOverclockerMenu.machineSlotItemX(slot.x);
        int y = TemporalOverclockerMenu.machineSlotItemY(slot.y);
        renderMachineSlotItem(graphics, x, y, itemStack, itemCount);
    }

    private int sideButtonsScreenX() {
        return this.leftPos + TemporalOverclockerMenu.sideButtonsX(ENTRIES_START_X, ENTRY_WIDTH);
    }

    private int energyBarScreenY() {
        return this.topPos + TemporalOverclockerMenu.energyBarY(this.imageHeight);
    }

    private int sideButtonsStartScreenY() {
        return this.topPos + TemporalOverclockerMenu.sideButtonsStartY(this.imageHeight);
    }

    private int redstoneButtonScreenY() {
        return sideButtonsStartScreenY();
    }

    private int persistentButtonScreenY() {
        return sideButtonsStartScreenY() + REDSTONE_BUTTON_SIZE + TemporalOverclockerMenu.SIDE_BUTTONS_GAP;
    }
    
    private void renderEnergyBar(GuiGraphicsExtractor guiGraphics) {
        // Position energy bar on the left side, centered in the space between left edge and entries start
        // ENTRIES_START_X is the space available (30px), center the bar in that space
        int energyBarX = this.leftPos + (ENTRIES_START_X - ENERGY_BAR_WIDTH) / 2;
        int energyBarY = energyBarScreenY();
        
        // Always draw empty energy bar background
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ENERGY_BAR, energyBarX, energyBarY,
                       8.0F, 0.0F,
                       ENERGY_BAR_WIDTH, ENERGY_BAR_HEIGHT,
                       16, 32); // Total texture size: 16x32
        
        // Calculate energy fill percentage
        int energy = this.menu.getEnergyStored();
        int maxEnergy = this.menu.getMaxEnergyStored();
        
        if (energy > 0 && maxEnergy > 0) {
            int energyHeight = (energy * ENERGY_BAR_HEIGHT) / maxEnergy;
            int energyY = energyBarY + (ENERGY_BAR_HEIGHT - energyHeight);
            
            // Draw filled energy bar
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ENERGY_BAR, energyBarX, energyY,
                           0.0F, (float)(ENERGY_BAR_HEIGHT - energyHeight),
                           ENERGY_BAR_WIDTH, energyHeight,
                           16, 32); // Total texture size: 16x32
        }
    }
    
    /**
     * Renders linked block entries
     */
    private void renderEntries(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        // Always draw entry backgrounds for visible entries
        for (int i = 0; i < VISIBLE_ENTRIES; i++) {
            int entryIndex = scrollOffset + i;
            int entryX = this.leftPos + ENTRIES_START_X;
            int entryY = this.topPos + ENTRIES_START_Y + i * ENTRY_HEIGHT;
            
            // Draw entry background
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ENTRY_TEXTURE, entryX, entryY, 0.0F, 0.0F,
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
    private void renderLinkedBlockEntry(GuiGraphicsExtractor guiGraphics, int entryX, int entryY, BlockPos pos, int mouseX, int mouseY) {
        // Draw single slot for block item
        int slotX = entryX + 3; // 3 pixels from left edge
        int slotY = entryY + 3; // 3 pixels from top edge
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SINGLE_SLOT_TEXTURE, slotX, slotY, 0.0F, 0.0F, 18, 18, 18, 18);
        
        // Get block state and render as item if possible
        if (this.minecraft != null && this.minecraft.level != null) {
            net.minecraft.world.level.block.state.BlockState blockState = this.minecraft.level.getBlockState(pos);
            if (blockState != null && !blockState.isAir()) {
                net.minecraft.world.item.ItemStack blockStack = new net.minecraft.world.item.ItemStack(blockState.getBlock().asItem());
                if (!blockStack.isEmpty()) {
                    guiGraphics.item(blockStack, slotX + 1, slotY + 1);
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
                
                guiGraphics.text(this.font, Component.literal(blockNameString), textX, textY, GuiTextColors.TITLE, false);
            }
        }
        
        // Buttons are now vanilla Button widgets, rendered automatically
    }
    
    /**
     * Renderizza il pulsante redstone mode
     */
    private void renderPersistentModeButton(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        // Check if mouse is over the button
        boolean isHovered = mouseX >= sideButtonsScreenX() && mouseX <= sideButtonsScreenX() + PERSISTENT_BUTTON_SIZE &&
                           mouseY >= persistentButtonScreenY() && mouseY <= persistentButtonScreenY() + PERSISTENT_BUTTON_SIZE;
        
        // Draw button background (normal or highlighted)
        int textureY = isHovered ? 16 : 0; // Highlighted version is below the normal one
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, MEDIUM_BUTTONS, sideButtonsScreenX(), persistentButtonScreenY(),
                        0.0F, (float)textureY, PERSISTENT_BUTTON_SIZE, PERSISTENT_BUTTON_SIZE,
                        96, 96); // Correct texture size: 96x96
        
        // Get current persistent mode from menu
        boolean isPersistent = this.menu.isPersistentMode();
        
        // Draw the appropriate icon (12x12 pixels, centered in the 16x16 button)
        int iconX = sideButtonsScreenX() + 2; // Center: (16-12)/2 = 2
        int iconY = persistentButtonScreenY() + 2; // Center: (16-12)/2 = 2
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
    
    private void renderRedstoneModeButton(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        // Check if mouse is over the button
        boolean isHovered = mouseX >= sideButtonsScreenX() && mouseX <= sideButtonsScreenX() + REDSTONE_BUTTON_SIZE &&
                           mouseY >= redstoneButtonScreenY() && mouseY <= redstoneButtonScreenY() + REDSTONE_BUTTON_SIZE;
        
        // Draw button background (normal or highlighted)
        int textureY = isHovered ? 16 : 0; // Highlighted version is below the normal one
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, MEDIUM_BUTTONS, sideButtonsScreenX(), redstoneButtonScreenY(),
                        0.0F, (float)textureY, REDSTONE_BUTTON_SIZE, REDSTONE_BUTTON_SIZE,
                        96, 96); // Correct texture size: 96x96
        
        // Get current redstone mode from menu (3 = legacy PULSE, treat as DISABLED for display)
        int redstoneMode = this.menu.getRedstoneMode();
        if (redstoneMode == 3) {
            redstoneMode = 4;
        }
        
        // Draw the appropriate icon (12x12 pixels, centered in the 16x16 button)
        int iconX = sideButtonsScreenX() + 2; // Center: (16-12)/2 = 2
        int iconY = redstoneButtonScreenY() + 2; // Center: (16-12)/2 = 2
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
            case 4 -> {
                // DISABLED mode: Barrier icon
                net.minecraft.world.item.ItemStack barrier = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BARRIER);
                renderScaledItem(guiGraphics, barrier, iconX, iconY, iconSize);
            }
            default -> {
                net.minecraft.world.item.ItemStack barrier = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BARRIER);
                renderScaledItem(guiGraphics, barrier, iconX, iconY, iconSize);
            }
        }
    }
    
    /**
     * Renders an item scaled to the specified size
     */
    private void renderScaledItem(GuiGraphicsExtractor guiGraphics, net.minecraft.world.item.ItemStack itemStack, int x, int y, int size) {
        guiGraphics.pose().pushMatrix();
        float scale = (float) size / 16.0f;
        guiGraphics.pose().translate(x, y);
        guiGraphics.pose().scale(scale, scale);
        guiGraphics.item(itemStack, 0, 0);
        guiGraphics.pose().popMatrix();
    }
    
    /**
     * Renders a texture scaled to the specified size (like an item)
     */
    private void renderScaledTexture(GuiGraphicsExtractor guiGraphics, Identifier texture, int x, int y, int size) {
        guiGraphics.pose().pushMatrix();
        float scale = (float) size / 16.0f;
        guiGraphics.pose().translate(x, y);
        guiGraphics.pose().scale(scale, scale);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, 0, 0, 0.0F, 0.0F, 16, 16, 16, 16);
        guiGraphics.pose().popMatrix();
    }
    
    
    /**
     * Renderizza la scrollbar
     */
    private void renderScrollbar(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        // Only show scrollbar if there are more entries than visible
        if (linkedBlocks.size() <= VISIBLE_ENTRIES) return;
        
        int scrollbarX = this.leftPos + SCROLLBAR_X;
        int scrollbarY = this.topPos + SCROLLBAR_Y;
        int buttonUpY = this.topPos + BUTTON_UP_Y;
        int buttonDownY = this.topPos + BUTTON_DOWN_Y;
        
        // Disegna la scrollbar completa
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SCROLLBAR_TEXTURE, scrollbarX, scrollbarY, 0.0F, 0.0F,
                        SCROLLBAR_WIDTH, SCROLLBAR_HEIGHT, 32, 34);
        
        // Pulsante SU
        boolean upHovered = mouseX >= scrollbarX && mouseX < scrollbarX + HANDLE_SIZE &&
                           mouseY >= buttonUpY && mouseY < buttonUpY + HANDLE_SIZE;
        int upTextureY = upHovered ? HANDLE_SIZE : 0;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SCROLLBAR_TEXTURE, scrollbarX, buttonUpY,
                        (float)(SCROLLBAR_WIDTH * 2), (float)upTextureY, HANDLE_SIZE, HANDLE_SIZE, 32, 34);
        
        // DOWN button
        boolean downHovered = mouseX >= scrollbarX && mouseX < scrollbarX + HANDLE_SIZE &&
                             mouseY >= buttonDownY && mouseY < buttonDownY + HANDLE_SIZE;
        int downTextureY = downHovered ? HANDLE_SIZE : 0;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SCROLLBAR_TEXTURE, scrollbarX, buttonDownY,
                        (float)(SCROLLBAR_WIDTH * 3), (float)downTextureY, HANDLE_SIZE, HANDLE_SIZE, 32, 34);
        
        // Handle
        float scrollRatio = 0;
        if (linkedBlocks.size() > VISIBLE_ENTRIES) {
            scrollRatio = (float) scrollOffset / (linkedBlocks.size() - VISIBLE_ENTRIES);
        }
        int handleY = scrollbarY + (int)(scrollRatio * (SCROLLBAR_HEIGHT - HANDLE_SIZE));
        
        boolean handleHovered = mouseX >= scrollbarX && mouseX < scrollbarX + HANDLE_SIZE &&
                               mouseY >= handleY && mouseY < handleY + HANDLE_SIZE;
        int handleTextureY = handleHovered ? HANDLE_SIZE : 0;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SCROLLBAR_TEXTURE, scrollbarX, handleY,
                        (float)SCROLLBAR_WIDTH, (float)handleTextureY, HANDLE_SIZE, HANDLE_SIZE, 32, 34);
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
    
    private boolean handleMouseClicked(double mouseX, double mouseY, int button) {
        // Handle acceleration button click FIRST (for all button types and modifiers)
        if (this.accelerationButton != null && this.accelerationButton.isMouseOver(mouseX, mouseY)) {
            boolean shift = isShiftDownNow();
            boolean ctrl = isCtrlDownNow();
            boolean alt = isAltDownNow();
            
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
            if (mouseX >= sideButtonsScreenX() && mouseX <= sideButtonsScreenX() + PERSISTENT_BUTTON_SIZE &&
                mouseY >= persistentButtonScreenY() && mouseY <= persistentButtonScreenY() + PERSISTENT_BUTTON_SIZE) {
                onPersistentModePressed();
                return true;
            }
        }

        if ((button == 0 || button == 1)
                && mouseX >= sideButtonsScreenX() && mouseX <= sideButtonsScreenX() + REDSTONE_BUTTON_SIZE
                && mouseY >= redstoneButtonScreenY() && mouseY <= redstoneButtonScreenY() + REDSTONE_BUTTON_SIZE) {
            onRedstoneModePressed(button == 1);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (handleMouseClicked(event.x(), event.y(), event.button())) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
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
    
    private void onRedstoneModePressed(boolean backward) {
        BlockPos machinePos = this.menu.getSyncedBlockPos();
        if (!machinePos.equals(BlockPos.ZERO)) {
            net.unfamily.iskautils.network.ModMessages.sendTemporalOverclockerRedstoneModePacket(machinePos, backward);
            playButtonSound();
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
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0 && isDraggingHandle) {
            isDraggingHandle = false;
            return true;
        }
        return super.mouseReleased(event);
    }
    
    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (event.button() == 0 && isDraggingHandle && linkedBlocks.size() > VISIBLE_ENTRIES) {
            int deltaY = (int) event.y() - dragStartY;
            float scrollRatio = (float) deltaY / (SCROLLBAR_HEIGHT - HANDLE_SIZE);
            
            int newScrollOffset = dragStartScrollOffset + (int)(scrollRatio * (linkedBlocks.size() - VISIBLE_ENTRIES));
            newScrollOffset = Math.max(0, Math.min(linkedBlocks.size() - VISIBLE_ENTRIES, newScrollOffset));
            
            scrollOffset = newScrollOffset;
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
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
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        // Draw the title (centered)
        Component titleComponent = Component.translatable("block.iska_utils.temporal_overclocker");
        String title = titleComponent.getString();
        int titleX = (this.imageWidth - this.font.width(title)) / 2;
        guiGraphics.text(this.font, Component.literal(title), titleX, 6, GuiTextColors.TITLE, false);

        int max = menu.getMaxStoredEntropy();
        if (max > 0) {
            int pct = (int) Math.floor(100.0 * menu.getStoredEntropy() / max);
            Component text = Component.literal(pct + "%");
            int tx = TemporalOverclockerMenu.machineSlotLabelX(TemporalOverclockerMenu.FUEL_SLOT_X, font.width(text));
            guiGraphics.text(font, text, tx, TemporalOverclockerMenu.machineSlotLabelY(TemporalOverclockerMenu.ENTROPY_ROW_Y), GuiTextColors.TITLE, false);
        }
    }
    
    private void renderEnergyTooltip(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        int energyBarX = this.leftPos + (ENTRIES_START_X - ENERGY_BAR_WIDTH) / 2;
        int energyBarY = energyBarScreenY();
        
        if (mouseX >= energyBarX && mouseX <= energyBarX + ENERGY_BAR_WIDTH &&
            mouseY >= energyBarY && mouseY <= energyBarY + ENERGY_BAR_HEIGHT) {
            
            int energy = this.menu.getEnergyStored();
            int maxEnergy = this.menu.getMaxEnergyStored();
            
            Component tooltip = Component.literal(String.format("%,d / %,d RF", energy, maxEnergy));
            guiGraphics.setTooltipForNextFrame(this.font, java.util.List.of(tooltip.getVisualOrderText()), DefaultTooltipPositioner.INSTANCE, mouseX, mouseY, true);
        }
    }
    
    private void renderPersistentModeTooltip(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        boolean isHovered = mouseX >= sideButtonsScreenX() && mouseX <= sideButtonsScreenX() + PERSISTENT_BUTTON_SIZE &&
                           mouseY >= persistentButtonScreenY() && mouseY <= persistentButtonScreenY() + PERSISTENT_BUTTON_SIZE;
        
        if (isHovered) {
            boolean isPersistent = this.menu.isPersistentMode();
            
            java.util.List<Component> tooltipLines = new java.util.ArrayList<>();
            if (isPersistent) {
                tooltipLines.add(Component.translatable("gui.iska_utils.temporal_overclocker.persistent.on"));
            } else {
                tooltipLines.add(Component.translatable("gui.iska_utils.temporal_overclocker.persistent.off"));
            }
            tooltipLines.add(Component.translatable("gui.iska_utils.temporal_overclocker.persistent.description"));

            java.util.List<FormattedCharSequence> lines = tooltipLines.stream().map(Component::getVisualOrderText).toList();
            guiGraphics.setTooltipForNextFrame(this.font, lines, DefaultTooltipPositioner.INSTANCE, mouseX, mouseY, true);
        }
    }
    
    private void renderRedstoneModeTooltip(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        boolean isHovered = mouseX >= sideButtonsScreenX() && mouseX <= sideButtonsScreenX() + REDSTONE_BUTTON_SIZE &&
                           mouseY >= redstoneButtonScreenY() && mouseY <= redstoneButtonScreenY() + REDSTONE_BUTTON_SIZE;
        
        if (isHovered) {
            int redstoneMode = this.menu.getRedstoneMode();
            if (redstoneMode == 3) {
                redstoneMode = 4;
            }
            
            Component tooltip = switch (redstoneMode) {
                case 0 -> Component.translatable("gui.iska_utils.generic.redstone_mode.none");
                case 1 -> Component.translatable("gui.iska_utils.generic.redstone_mode.low");
                case 2 -> Component.translatable("gui.iska_utils.generic.redstone_mode.high");
                case 4 -> Component.translatable("gui.iska_utils.generic.redstone_mode.disabled");
                default -> Component.literal("Unknown mode");
            };
            
            guiGraphics.setTooltipForNextFrame(this.font, java.util.List.of(tooltip.getVisualOrderText()), DefaultTooltipPositioner.INSTANCE, mouseX, mouseY, true);
        }
    }
    
    private void renderAccelerationTooltip(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        if (this.accelerationButton != null && this.accelerationButton.isMouseOver(mouseX, mouseY)) {
            java.util.List<Component> tooltipLines = new java.util.ArrayList<>();
            tooltipLines.add(Component.translatable("gui.iska_utils.temporal_overclocker.acceleration_tooltip.line1"));
            tooltipLines.add(Component.translatable("gui.iska_utils.temporal_overclocker.acceleration_tooltip.line2"));
            int defaultValue = net.unfamily.iskautils.Config.temporalOverclockerAccelerationFactor;
            tooltipLines.add(Component.translatable("gui.iska_utils.temporal_overclocker.acceleration_tooltip.line3", defaultValue));
            java.util.List<FormattedCharSequence> lines = tooltipLines.stream().map(Component::getVisualOrderText).toList();
            guiGraphics.setTooltipForNextFrame(this.font, lines, DefaultTooltipPositioner.INSTANCE, mouseX, mouseY, true);
        }
    }
    
    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        renderEnergyTooltip(guiGraphics, mouseX, mouseY);
        renderPersistentModeTooltip(guiGraphics, mouseX, mouseY);
        renderRedstoneModeTooltip(guiGraphics, mouseX, mouseY);
        renderAccelerationTooltip(guiGraphics, mouseX, mouseY);
    }
}


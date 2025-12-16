package net.unfamily.iskautils.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.network.ModMessages;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Screen for the Deep Drawers GUI
 * 
 * Features:
 * - 9x6 grid of visible slots (54 total visible)
 * - Scrollbar to navigate through all slots
 * - Mouse wheel support
 * - Drag-to-scroll support
 */
public class DeepDrawersScreen extends AbstractContainerScreen<DeepDrawersMenu> {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DeepDrawersScreen.class);
    
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/backgrounds/deep_drawer.png");
    private static final ResourceLocation SCROLLBAR_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/scrollbar.png");
    
    // GUI dimensions (measured from actual texture file: 197x235 pixels)
    private static final int TEXTURE_WIDTH = 197;
    private static final int TEXTURE_HEIGHT = 235;
    private static final int GUI_WIDTH = TEXTURE_WIDTH;
    private static final int GUI_HEIGHT = TEXTURE_HEIGHT;
    
    // Scrollbar constants (texture is 32x34: 8px wide elements in a 32px wide texture)
    private static final int SCROLLBAR_WIDTH = 8;      // Width of each scrollbar element
    private static final int SCROLLBAR_HEIGHT = 34;    // Height of scrollbar background in texture (NOT 108!)
    private static final int HANDLE_SIZE = 8;          // Size of UP/DOWN buttons and handle
    
    // Scrollbar positions - moved 4px left from 184
    // Last slot column ends at: 15 + (9*18) = 177 (with 7px shift)
    // Scrollbar at: 184 - 4 = 180
    private static final int SCROLLBAR_X = 180;        // 4px left from previous position
    private static final int BUTTON_UP_Y = 31;         // Aligned with first storage slot row (Y=31)
    private static final int SCROLLBAR_Y = BUTTON_UP_Y + HANDLE_SIZE; // Scrollbar starts after UP button (Y=39)
    private static final int BUTTON_DOWN_Y = SCROLLBAR_Y + SCROLLBAR_HEIGHT; // DOWN button after scrollbar (Y=73)
    
    // Scrolling state
    private int scrollOffset = 0;
    private boolean isDraggingHandle = false;
    private int dragStartY = 0;
    private int dragStartScrollOffset = 0;
    
    // Close button
    private Button closeButton;
    private static final int CLOSE_BUTTON_Y = 5;
    private static final int CLOSE_BUTTON_SIZE = 12;
    private static final int CLOSE_BUTTON_X = GUI_WIDTH - CLOSE_BUTTON_SIZE - 5; // 5px from right edge
    
    public DeepDrawersScreen(DeepDrawersMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        this.inventoryLabelY = 10000; // Hide "Inventory" label
    }
    
    @Override
    protected void init() {
        super.init();
        // Load current scroll offset from menu (synced from server)
        // This ensures the scrollbar starts at the saved position
        this.scrollOffset = menu.getScrollOffset();
        LOGGER.debug("DeepDrawersScreen.init: Initialized scrollOffset from menu: {}", this.scrollOffset);
        
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
        // Sync scroll offset from menu in case it was updated from server
        // This handles the case where ContainerData syncs after init()
        int menuOffset = menu.getScrollOffset();
        if (menuOffset != this.scrollOffset) {
            LOGGER.debug("DeepDrawersScreen.containerTick: Syncing scrollOffset from menu: {} -> {}", this.scrollOffset, menuOffset);
            this.scrollOffset = menuOffset;
        }
    }
    
    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int guiX = this.leftPos;
        int guiY = this.topPos;
        
        // Render main background (texture is 176x230 pixels)
        guiGraphics.blit(TEXTURE, guiX, guiY, 0, 0, this.imageWidth, this.imageHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        
        // Render scrollbar
        renderScrollbar(guiGraphics, mouseX, mouseY);
    }
    
    /**
     * Renders the scrollbar with UP/DOWN buttons and draggable handle
     * Identical to ShopScreen implementation
     */
    private void renderScrollbar(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int totalSlots = menu.getTotalSlots();
        int visibleSlots = DeepDrawersMenu.VISIBLE_SLOTS;
        
        // Only show scrollbar if there are more slots than can fit
        if (totalSlots <= visibleSlots) return;
        
        int guiX = this.leftPos;
        int guiY = this.topPos;
        
        // Draw scrollbar background (8 pixels wide, height as defined)
        guiGraphics.blit(SCROLLBAR_TEXTURE, guiX + SCROLLBAR_X, guiY + SCROLLBAR_Y, 0, 0, SCROLLBAR_WIDTH, SCROLLBAR_HEIGHT, 32, 34);
        
        // UP button (8x8 pixels) - above scrollbar
        boolean upButtonHovered = (mouseX >= guiX + SCROLLBAR_X && mouseX < guiX + SCROLLBAR_X + SCROLLBAR_WIDTH &&
                                  mouseY >= guiY + BUTTON_UP_Y && mouseY < guiY + BUTTON_UP_Y + HANDLE_SIZE);
        int upButtonV = upButtonHovered ? HANDLE_SIZE : 0;
        guiGraphics.blit(SCROLLBAR_TEXTURE, guiX + SCROLLBAR_X, guiY + BUTTON_UP_Y, SCROLLBAR_WIDTH * 2, upButtonV, HANDLE_SIZE, HANDLE_SIZE, 32, 34);
        
        // DOWN button (8x8 pixels) - below scrollbar
        boolean downButtonHovered = (mouseX >= guiX + SCROLLBAR_X && mouseX < guiX + SCROLLBAR_X + SCROLLBAR_WIDTH &&
                                    mouseY >= guiY + BUTTON_DOWN_Y && mouseY < guiY + BUTTON_DOWN_Y + HANDLE_SIZE);
        int downButtonV = downButtonHovered ? HANDLE_SIZE : 0;
        guiGraphics.blit(SCROLLBAR_TEXTURE, guiX + SCROLLBAR_X, guiY + BUTTON_DOWN_Y, SCROLLBAR_WIDTH * 3, downButtonV, HANDLE_SIZE, HANDLE_SIZE, 32, 34);
        
        // Handle (8x8 pixels) - position based on scroll offset
        int maxScrollOffset = menu.getMaxScrollOffset();
        if (maxScrollOffset > 0) {
            double scrollRatio = (double) scrollOffset / maxScrollOffset;
            int handleY = guiY + SCROLLBAR_Y + (int)(scrollRatio * (SCROLLBAR_HEIGHT - HANDLE_SIZE));
            
            boolean handleHovered = (mouseX >= guiX + SCROLLBAR_X && mouseX < guiX + SCROLLBAR_X + HANDLE_SIZE &&
                                    mouseY >= handleY && mouseY < handleY + HANDLE_SIZE);
            int handleTextureY = handleHovered ? HANDLE_SIZE : 0;
            guiGraphics.blit(SCROLLBAR_TEXTURE, guiX + SCROLLBAR_X, handleY, SCROLLBAR_WIDTH, handleTextureY, HANDLE_SIZE, HANDLE_SIZE, 32, 34);
        }
    }
    
    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Render title centered
        Component titleComponent = this.title;
        int titleWidth = this.font.width(titleComponent);
        int titleX = (this.imageWidth - titleWidth) / 2;
        guiGraphics.drawString(this.font, titleComponent, titleX, 7, 0x404040, false);
        
        // Don't render "Inventory" label (already hidden via inventoryLabelY)
    }
    
    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Sync scroll offset from menu (in case it was updated by server via ContainerData)
        // This ensures the scrollbar position is always up-to-date
        int menuOffset = menu.getScrollOffset();
        if (menuOffset != this.scrollOffset) {
            LOGGER.debug("DeepDrawersScreen.render: Syncing scrollOffset from menu: {} -> {}", this.scrollOffset, menuOffset);
            this.scrollOffset = menuOffset;
        }
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Left click
            // Handle scrollbar clicks first
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
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    /**
     * Handles clicks on UP/DOWN buttons
     */
    private boolean handleScrollButtonClick(double mouseX, double mouseY) {
        int totalSlots = menu.getTotalSlots();
        int visibleSlots = DeepDrawersMenu.VISIBLE_SLOTS;
        if (totalSlots <= visibleSlots) return false;
        
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
    
    /**
     * Handles clicks on the draggable handle
     */
    private boolean handleHandleClick(double mouseX, double mouseY) {
        int totalSlots = menu.getTotalSlots();
        int visibleSlots = DeepDrawersMenu.VISIBLE_SLOTS;
        if (totalSlots <= visibleSlots) return false;
        
        int guiX = this.leftPos;
        int guiY = this.topPos;
        
        int maxScrollOffset = menu.getMaxScrollOffset();
            if (maxScrollOffset > 0) {
                double scrollRatio = (double) scrollOffset / maxScrollOffset;
                int handleY = guiY + SCROLLBAR_Y + (int)(scrollRatio * (SCROLLBAR_HEIGHT - HANDLE_SIZE));
            
            if (mouseX >= guiX + SCROLLBAR_X && mouseX < guiX + SCROLLBAR_X + HANDLE_SIZE &&
                mouseY >= handleY && mouseY < handleY + HANDLE_SIZE) {
                
                isDraggingHandle = true;
                dragStartY = (int) mouseY;
                dragStartScrollOffset = scrollOffset;
                playButtonSound();
                return true;
            }
        }
        return false;
    }
    
    /**
     * Handles clicks on the scrollbar area (jump to position)
     */
    private boolean handleScrollbarClick(double mouseX, double mouseY) {
        int totalSlots = menu.getTotalSlots();
        int visibleSlots = DeepDrawersMenu.VISIBLE_SLOTS;
        if (totalSlots <= visibleSlots) return false;
        
        int guiX = this.leftPos;
        int guiY = this.topPos;
        
            if (mouseX >= guiX + SCROLLBAR_X && mouseX < guiX + SCROLLBAR_X + SCROLLBAR_WIDTH &&
                mouseY >= guiY + SCROLLBAR_Y && mouseY < guiY + SCROLLBAR_Y + SCROLLBAR_HEIGHT) {
            
            // Calculate new scroll position based on click
            float clickRatio = (float)(mouseY - (guiY + SCROLLBAR_Y)) / SCROLLBAR_HEIGHT;
            clickRatio = Math.max(0, Math.min(1, clickRatio));
            
            int maxScrollOffset = menu.getMaxScrollOffset();
            int newScrollOffset = (int)(clickRatio * maxScrollOffset);
            newScrollOffset = Math.max(0, Math.min(maxScrollOffset, newScrollOffset));
            
            if (newScrollOffset != scrollOffset) {
                setScrollOffset(newScrollOffset);
                playButtonSound();
            }
            return true;
        }
        return false;
    }
    
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
        int totalSlots = menu.getTotalSlots();
        int visibleSlots = DeepDrawersMenu.VISIBLE_SLOTS;
        
        if (button == 0 && isDraggingHandle && totalSlots > visibleSlots) {
            int deltaY = (int) mouseY - dragStartY;
            int maxScrollOffset = menu.getMaxScrollOffset();
            
            if (maxScrollOffset > 0) {
                float scrollRatio = (float) deltaY / (SCROLLBAR_HEIGHT - HANDLE_SIZE);
                
                int newScrollOffset = dragStartScrollOffset + (int)(scrollRatio * maxScrollOffset);
                newScrollOffset = Math.max(0, Math.min(maxScrollOffset, newScrollOffset));
                
                setScrollOffset(newScrollOffset);
            }
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
    
    /**
     * Scrolls up by one row (9 slots)
     */
    private void scrollUp() {
        if (scrollUpSilent()) {
            playButtonSound();
        }
    }
    
    /**
     * Scrolls down by one row (9 slots)
     */
    private void scrollDown() {
        if (scrollDownSilent()) {
            playButtonSound();
        }
    }
    
    /**
     * Scrolls up silently (without sound)
     */
    private boolean scrollUpSilent() {
        int totalSlots = menu.getTotalSlots();
        int visibleSlots = DeepDrawersMenu.VISIBLE_SLOTS;
        
        if (totalSlots > visibleSlots && scrollOffset > 0) {
            // Scroll by one row (9 slots)
            int newOffset = Math.max(0, scrollOffset - DeepDrawersMenu.COLUMNS);
            setScrollOffset(newOffset);
            return true;
        }
        return false;
    }
    
    /**
     * Scrolls down silently (without sound)
     */
    private boolean scrollDownSilent() {
        int totalSlots = menu.getTotalSlots();
        int visibleSlots = DeepDrawersMenu.VISIBLE_SLOTS;
        int maxScrollOffset = menu.getMaxScrollOffset();
        
        if (totalSlots > visibleSlots && scrollOffset < maxScrollOffset) {
            // Scroll by one row (9 slots)
            int newOffset = Math.min(maxScrollOffset, scrollOffset + DeepDrawersMenu.COLUMNS);
            setScrollOffset(newOffset);
            return true;
        }
        return false;
    }
    
    /**
     * Sets the scroll offset and updates the menu
     */
    private void setScrollOffset(int offset) {
        this.scrollOffset = offset;
        
        // Update the menu's scroll offset immediately (client-side)
        // This makes the slots show the correct items right away
        menu.setScrollOffset(offset);
        
        // Send update to server - the server will update the menu and send back the slot contents
        ModMessages.sendDeepDrawersScrollPacket(menu.getBlockPos(), offset);
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


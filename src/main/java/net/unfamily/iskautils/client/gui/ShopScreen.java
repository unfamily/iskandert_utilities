package net.unfamily.iskautils.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.unfamily.iskautils.IskaUtils;

public class ShopScreen extends AbstractContainerScreen<AbstractContainerMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/backgrounds/shop.png");
    private static final ResourceLocation ENTRY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/entry_wide.png");
    private static final ResourceLocation SCROLLBAR_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/scrollbar.png");

    private static final int GUI_WIDTH = 200;
    private static final int GUI_HEIGHT = 240;
    private static final int ENTRY_WIDTH = 140;
    private static final int ENTRY_HEIGHT = 24;
    private static final int ENTRIES = 5;
    private static final int ENTRY_START_X = 30; // (200-140)/2
    private static final int ENTRY_START_Y = 33;
    private static final int SCROLLBAR_WIDTH = 8;
    private static final int SCROLLBAR_HEIGHT = ENTRY_HEIGHT * ENTRIES;
    private static final int SCROLLBAR_X = ENTRY_START_X + ENTRY_WIDTH + 4;
    private static final int SCROLLBAR_Y = ENTRY_START_Y;
    private static final int BUTTON_SIZE = 8;
    
    // Variabili per la scrollbar
    private int scrollOffset = 0;
    private int maxScrollOffset = 0;
    private int totalEntries = 20; // Placeholder - sarà popolato dai dati reali
    private boolean isDragging = false;
    private int dragStartY = 0;

    public ShopScreen(AbstractContainerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        updateScrollbar();
    }
    
    private void updateScrollbar() {
        // Calcola il massimo offset di scroll
        maxScrollOffset = Math.max(0, totalEntries - ENTRIES);
        scrollOffset = Mth.clamp(scrollOffset, 0, maxScrollOffset);
    }
    
    /**
     * Aggiorna il numero totale di entry disponibili
     */
    public void setTotalEntries(int totalEntries) {
        this.totalEntries = totalEntries;
        updateScrollbar();
    }
    
    /**
     * Ritorna l'offset di scroll corrente
     */
    public int getScrollOffset() {
        return scrollOffset;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        // Sfondo
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight, GUI_WIDTH, GUI_HEIGHT);
        // Entry
        for (int i = 0; i < ENTRIES; i++) {
            int entryY = y + ENTRY_START_Y + i * ENTRY_HEIGHT;
            guiGraphics.blit(ENTRY_TEXTURE, x + ENTRY_START_X, entryY, 0, 0, ENTRY_WIDTH, ENTRY_HEIGHT, ENTRY_WIDTH, ENTRY_HEIGHT);
        }
        // Scrollbar: pulsante su
        guiGraphics.blit(SCROLLBAR_TEXTURE, x + SCROLLBAR_X, y + SCROLLBAR_Y - BUTTON_SIZE, 0, 0, SCROLLBAR_WIDTH, BUTTON_SIZE, SCROLLBAR_WIDTH, 50);
        // Scrollbar: barra
        guiGraphics.blit(SCROLLBAR_TEXTURE, x + SCROLLBAR_X, y + SCROLLBAR_Y, 0, BUTTON_SIZE, SCROLLBAR_WIDTH, SCROLLBAR_HEIGHT, SCROLLBAR_WIDTH, 50);
        // Scrollbar: handler (posizione basata su scroll)
        int handlerY = getHandlerY(y);
        guiGraphics.blit(SCROLLBAR_TEXTURE, x + SCROLLBAR_X, handlerY, 0, BUTTON_SIZE + SCROLLBAR_HEIGHT, SCROLLBAR_WIDTH, BUTTON_SIZE, SCROLLBAR_WIDTH, 50);
        // Scrollbar: pulsante giù
        guiGraphics.blit(SCROLLBAR_TEXTURE, x + SCROLLBAR_X, y + SCROLLBAR_Y + SCROLLBAR_HEIGHT, 0, BUTTON_SIZE * 2, SCROLLBAR_WIDTH, BUTTON_SIZE, SCROLLBAR_WIDTH, 50);
        // Inventario vanilla
        int invX = x + 19;
        int invY = y + 153;
        this.renderPlayerInventory(guiGraphics, invX, invY);
    }

    private void renderPlayerInventory(GuiGraphics guiGraphics, int x, int y) {
        // Solo placeholder: disegna 3 righe da 9 slot (18x18)
        int slotSize = 18;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotX = x + col * slotSize;
                int slotY = y + row * slotSize;
                guiGraphics.fill(slotX, slotY, slotX + slotSize, slotY + slotSize, 0xFF888888);
            }
        }
        // Hotbar
        int hotbarY = y + 3 * slotSize;
        for (int col = 0; col < 9; col++) {
            int slotX = x + col * slotSize;
            guiGraphics.fill(slotX, hotbarY, slotX + slotSize, hotbarY + slotSize, 0xFF888888);
        }
    }
    
    private int getHandlerY(int guiY) {
        if (maxScrollOffset == 0) {
            return guiY + SCROLLBAR_Y + 2; // Centrato se non c'è scroll
        }
        
        // Calcola posizione handler basata su scroll
        int scrollableArea = SCROLLBAR_HEIGHT - BUTTON_SIZE;
        float scrollProgress = (float) scrollOffset / maxScrollOffset;
        int handlerOffset = (int) (scrollProgress * scrollableArea);
        
        return guiY + SCROLLBAR_Y + 2 + handlerOffset;
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Click sinistro
            int x = (this.width - this.imageWidth) / 2;
            int y = (this.height - this.imageHeight) / 2;
            
            // Check pulsante su
            if (isMouseOverUpButton(mouseX, mouseY, x, y)) {
                scrollUp();
                return true;
            }
            
            // Check pulsante giù
            if (isMouseOverDownButton(mouseX, mouseY, x, y)) {
                scrollDown();
                return true;
            }
            
            // Check handler per drag
            if (isMouseOverHandler(mouseX, mouseY, x, y)) {
                isDragging = true;
                dragStartY = (int) mouseY;
                return true;
            }
            
            // Check area scrollbar per jump
            if (isMouseOverScrollbar(mouseX, mouseY, x, y)) {
                int handlerY = getHandlerY(y);
                if (mouseY < handlerY) {
                    scrollUp();
                } else {
                    scrollDown();
                }
                return true;
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && isDragging) {
            isDragging = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isDragging && button == 0) {
            int y = (this.height - this.imageHeight) / 2;
            int scrollableArea = SCROLLBAR_HEIGHT - BUTTON_SIZE;
            
            int currentHandlerY = getHandlerY(y);
            int newHandlerY = currentHandlerY + (int) deltaY;
            
            // Calcola nuovo scroll offset basato sulla posizione handler
            int minHandlerY = y + SCROLLBAR_Y + 2;
            int maxHandlerY = y + SCROLLBAR_Y + scrollableArea + 2;
            
            newHandlerY = Mth.clamp(newHandlerY, minHandlerY, maxHandlerY);
            
            if (maxScrollOffset > 0) {
                float progress = (float) (newHandlerY - minHandlerY) / scrollableArea;
                scrollOffset = Math.round(progress * maxScrollOffset);
                scrollOffset = Mth.clamp(scrollOffset, 0, maxScrollOffset);
            }
            
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (deltaY > 0) {
            scrollUp();
        } else if (deltaY < 0) {
            scrollDown();
        }
        return true;
    }
    
    private void scrollUp() {
        if (scrollOffset > 0) {
            scrollOffset--;
        }
    }
    
    private void scrollDown() {
        if (scrollOffset < maxScrollOffset) {
            scrollOffset++;
        }
    }
    
    private boolean isMouseOverUpButton(double mouseX, double mouseY, int guiX, int guiY) {
        int buttonX = guiX + SCROLLBAR_X;
        int buttonY = guiY + SCROLLBAR_Y - BUTTON_SIZE;
        return mouseX >= buttonX && mouseX < buttonX + SCROLLBAR_WIDTH &&
               mouseY >= buttonY && mouseY < buttonY + BUTTON_SIZE;
    }
    
    private boolean isMouseOverDownButton(double mouseX, double mouseY, int guiX, int guiY) {
        int buttonX = guiX + SCROLLBAR_X;
        int buttonY = guiY + SCROLLBAR_Y + SCROLLBAR_HEIGHT;
        return mouseX >= buttonX && mouseX < buttonX + SCROLLBAR_WIDTH &&
               mouseY >= buttonY && mouseY < buttonY + BUTTON_SIZE;
    }
    
    private boolean isMouseOverHandler(double mouseX, double mouseY, int guiX, int guiY) {
        int handlerX = guiX + SCROLLBAR_X;
        int handlerY = getHandlerY(guiY);
        return mouseX >= handlerX && mouseX < handlerX + SCROLLBAR_WIDTH &&
               mouseY >= handlerY && mouseY < handlerY + BUTTON_SIZE;
    }
    
    private boolean isMouseOverScrollbar(double mouseX, double mouseY, int guiX, int guiY) {
        int scrollbarX = guiX + SCROLLBAR_X;
        int scrollbarY = guiY + SCROLLBAR_Y;
        return mouseX >= scrollbarX && mouseX < scrollbarX + SCROLLBAR_WIDTH &&
               mouseY >= scrollbarY && mouseY < scrollbarY + SCROLLBAR_HEIGHT;
    }
} 
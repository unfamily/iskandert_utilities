package net.unfamily.iskautils.client.gui;

import net.unfamily.iskautils.util.ModLogger;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.network.ModMessages;
import org.jetbrains.annotations.NotNull;

/**
 * Screen for the Deep Drawers GUI
 *
 * Features:
 * - 9x5 grid of visible slots (45 total visible)
 * - Item name search bar above the grid
 * - Scrollbar aligned with storage rows
 * - Mouse wheel and drag-to-scroll support
 */
public class DeepDrawersScreen extends AbstractContainerScreen<DeepDrawersMenu> {

    private static final ModLogger LOGGER = ModLogger.of(DeepDrawersScreen.class);
    private static final int SEARCH_DEBOUNCE_TICKS = 4;

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/backgrounds/deep_drawer.png");
    private static final Identifier SCROLLBAR_TEXTURE =
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/scrollbar.png");

    private static final int TEXTURE_WIDTH = 197;
    private static final int TEXTURE_HEIGHT = 235;
    private static final int GUI_WIDTH = TEXTURE_WIDTH;
    private static final int GUI_HEIGHT = TEXTURE_HEIGHT;

    private static final int SCROLLBAR_WIDTH = 8;
    private static final int SCROLLBAR_HEIGHT = 34;
    private static final int HANDLE_SIZE = 8;

    private static final int SCROLLBAR_X = 180;
    private static final int BUTTON_UP_Y = DeepDrawersMenu.STORAGE_SLOTS_VISIBLE_Y;
    private static final int SCROLLBAR_Y = BUTTON_UP_Y + HANDLE_SIZE;
    private static final int BUTTON_DOWN_Y = SCROLLBAR_Y + SCROLLBAR_HEIGHT;

    private int scrollOffset = 0;
    private boolean isDraggingHandle = false;
    private int dragStartY = 0;
    private int dragStartScrollOffset = 0;

    private EditBox searchBox;
    private int searchDebounceTicks = 0;

    private Button closeButton;
    private static final int CLOSE_BUTTON_Y = 5;
    private static final int CLOSE_BUTTON_SIZE = 12;
    private static final int CLOSE_BUTTON_X = GUI_WIDTH - CLOSE_BUTTON_SIZE - 5;

    public DeepDrawersScreen(DeepDrawersMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, GUI_WIDTH, GUI_HEIGHT);
        inventoryLabelY = 10000;
    }

    @Override
    protected void init() {
        super.init();
        scrollOffset = menu.getEffectiveScrollOffset();

        searchBox = new EditBox(
                font,
                leftPos + DeepDrawersMenu.SEARCH_BAR_X,
                topPos + DeepDrawersMenu.SEARCH_BAR_Y,
                DeepDrawersMenu.SEARCH_BAR_WIDTH,
                DeepDrawersMenu.SEARCH_BAR_HEIGHT,
                Component.empty());
        searchBox.setMaxLength(256);
        searchBox.setBordered(true);
        searchBox.setResponder(text -> searchDebounceTicks = SEARCH_DEBOUNCE_TICKS);
        addRenderableWidget(searchBox);

        closeButton = Button.builder(Component.literal("✕"), button -> {
                    playButtonSound();
                    onClose();
                })
                .bounds(leftPos + CLOSE_BUTTON_X, topPos + CLOSE_BUTTON_Y, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE)
                .build();
        addRenderableWidget(closeButton);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (menu.isSearchFilterActive()) {
            scrollOffset = menu.getEffectiveScrollOffset();
        } else {
            int menuOffset = menu.getScrollOffset();
            if (menuOffset != scrollOffset) {
                scrollOffset = menuOffset;
            }
        }
        if (searchDebounceTicks > 0) {
            searchDebounceTicks--;
            if (searchDebounceTicks == 0) {
                applyItemSearch();
            }
        }
    }

    private void applyItemSearch() {
        if (searchBox == null) {
            return;
        }
        String query = searchBox.getValue() == null ? "" : searchBox.getValue();
        HolderLookup.Provider registryAccess = RegistryAccess.EMPTY;
        if (minecraft != null && minecraft.level != null) {
            registryAccess = minecraft.level.registryAccess();
        }
        menu.applySearchFilter(query, registryAccess);
        scrollOffset = menu.getEffectiveScrollOffset();
        syncSearchStateToServer();
    }

    private void syncSearchStateToServer() {
        if (searchBox == null) {
            return;
        }
        String query = searchBox.getValue() == null ? "" : searchBox.getValue();
        ModMessages.sendDeepDrawersSearchStatePacket(menu.getBlockPos(), query, menu.getEffectiveScrollOffset());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
        int guiX = leftPos;
        int guiY = topPos;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, guiX, guiY, 0.0F, 0.0F, imageWidth, imageHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        renderScrollbar(guiGraphics, mouseX, mouseY);
    }

    private void renderScrollbar(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        int guiX = leftPos;
        int guiY = topPos;

        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SCROLLBAR_TEXTURE, guiX + SCROLLBAR_X, guiY + SCROLLBAR_Y, 0.0F, 0.0F, SCROLLBAR_WIDTH, SCROLLBAR_HEIGHT, 32, 34);

        boolean upButtonHovered = mouseX >= guiX + SCROLLBAR_X && mouseX < guiX + SCROLLBAR_X + SCROLLBAR_WIDTH
                && mouseY >= guiY + BUTTON_UP_Y && mouseY < guiY + BUTTON_UP_Y + HANDLE_SIZE;
        int upButtonV = upButtonHovered ? HANDLE_SIZE : 0;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SCROLLBAR_TEXTURE, guiX + SCROLLBAR_X, guiY + BUTTON_UP_Y, (float) (SCROLLBAR_WIDTH * 2), (float) upButtonV, HANDLE_SIZE, HANDLE_SIZE, 32, 34);

        boolean downButtonHovered = mouseX >= guiX + SCROLLBAR_X && mouseX < guiX + SCROLLBAR_X + SCROLLBAR_WIDTH
                && mouseY >= guiY + BUTTON_DOWN_Y && mouseY < guiY + BUTTON_DOWN_Y + HANDLE_SIZE;
        int downButtonV = downButtonHovered ? HANDLE_SIZE : 0;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SCROLLBAR_TEXTURE, guiX + SCROLLBAR_X, guiY + BUTTON_DOWN_Y, (float) (SCROLLBAR_WIDTH * 3), (float) downButtonV, HANDLE_SIZE, HANDLE_SIZE, 32, 34);

        int maxScrollOffset = menu.getMaxScrollOffset();
        double scrollRatio = maxScrollOffset > 0 ? (double) scrollOffset / maxScrollOffset : 0.0;
        int handleY = guiY + SCROLLBAR_Y + (int) (scrollRatio * (SCROLLBAR_HEIGHT - HANDLE_SIZE));

        boolean handleHovered = mouseX >= guiX + SCROLLBAR_X && mouseX < guiX + SCROLLBAR_X + HANDLE_SIZE
                && mouseY >= handleY && mouseY < handleY + HANDLE_SIZE;
        int handleTextureY = handleHovered ? HANDLE_SIZE : 0;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SCROLLBAR_TEXTURE, guiX + SCROLLBAR_X, handleY, (float) SCROLLBAR_WIDTH, (float) handleTextureY, HANDLE_SIZE, HANDLE_SIZE, 32, 34);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        int titleWidth = font.width(title);
        int titleX = (imageWidth - titleWidth) / 2;
        guiGraphics.text(font, title, titleX, 7, GuiTextColors.TITLE, false);

        Component capacity = Component.translatable(
                "gui.iska_utils.deep_drawers.capacity",
                menu.getDisplayedOccupiedCount(),
                menu.getDisplayedMaxSlots());
        int capacityWidth = font.width(capacity);
        int capacityX = (imageWidth - capacityWidth) / 2;
        guiGraphics.text(font, capacity, capacityX, DeepDrawersMenu.CAPACITY_LABEL_Y, GuiTextColors.TITLE, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        scrollOffset = menu.getEffectiveScrollOffset();
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (MachineGuiInput.handleContainerKeyPressed(this, event, isDraggingHandle, searchBox)) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (searchBox != null && searchBox.isFocused() && searchBox.charTyped(event)) {
            return true;
        }
        return super.charTyped(event);
    }

    private boolean handleMouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (handleScrollButtonClick(mouseX, mouseY)) {
                MachineGuiInput.markScrollbarPressed();
                return true;
            }
            if (handleHandleClick(mouseX, mouseY)) {
                MachineGuiInput.markScrollbarPressed();
                return true;
            }
            if (handleScrollbarClick(mouseX, mouseY)) {
                MachineGuiInput.markScrollbarPressed();
                return true;
            }
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

    private boolean handleScrollButtonClick(double mouseX, double mouseY) {
        int guiX = leftPos;
        int guiY = topPos;

        if (mouseX >= guiX + SCROLLBAR_X && mouseX < guiX + SCROLLBAR_X + SCROLLBAR_WIDTH
                && mouseY >= guiY + BUTTON_UP_Y && mouseY < guiY + BUTTON_UP_Y + HANDLE_SIZE) {
            scrollUp();
            return true;
        }

        if (mouseX >= guiX + SCROLLBAR_X && mouseX < guiX + SCROLLBAR_X + SCROLLBAR_WIDTH
                && mouseY >= guiY + BUTTON_DOWN_Y && mouseY < guiY + BUTTON_DOWN_Y + HANDLE_SIZE) {
            scrollDown();
            return true;
        }

        return false;
    }

    private boolean handleHandleClick(double mouseX, double mouseY) {
        int guiX = leftPos;
        int guiY = topPos;
        int maxScrollOffset = menu.getMaxScrollOffset();
        double scrollRatio = maxScrollOffset > 0 ? (double) scrollOffset / maxScrollOffset : 0.0;
        int handleY = guiY + SCROLLBAR_Y + (int) (scrollRatio * (SCROLLBAR_HEIGHT - HANDLE_SIZE));

        if (mouseX >= guiX + SCROLLBAR_X && mouseX < guiX + SCROLLBAR_X + HANDLE_SIZE
                && mouseY >= handleY && mouseY < handleY + HANDLE_SIZE) {
            isDraggingHandle = true;
            dragStartY = (int) mouseY;
            dragStartScrollOffset = scrollOffset;
            playButtonSound();
            return true;
        }
        return false;
    }

    private boolean handleScrollbarClick(double mouseX, double mouseY) {
        int guiX = leftPos;
        int guiY = topPos;

        if (mouseX >= guiX + SCROLLBAR_X && mouseX < guiX + SCROLLBAR_X + SCROLLBAR_WIDTH
                && mouseY >= guiY + SCROLLBAR_Y && mouseY < guiY + SCROLLBAR_Y + SCROLLBAR_HEIGHT) {
            float clickRatio = (float) (mouseY - (guiY + SCROLLBAR_Y)) / SCROLLBAR_HEIGHT;
            clickRatio = Math.max(0, Math.min(1, clickRatio));

            int maxScrollOffset = menu.getMaxScrollOffset();
            int newScrollOffset = (int) (clickRatio * maxScrollOffset);
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
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0) {
            MachineGuiInput.clearScrollbarPressed();
            if (isDraggingHandle) {
                isDraggingHandle = false;
                return true;
            }
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (event.button() == 0 && isDraggingHandle) {
            int deltaY = (int) event.y() - dragStartY;
            int maxScrollOffset = menu.getMaxScrollOffset();
            float scrollRatio = (float) deltaY / (SCROLLBAR_HEIGHT - HANDLE_SIZE);
            int newScrollOffset = dragStartScrollOffset + (int) (scrollRatio * maxScrollOffset);
            newScrollOffset = Math.max(0, Math.min(maxScrollOffset, newScrollOffset));
            setScrollOffset(newScrollOffset);
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
        if (scrollOffset > 0) {
            int newOffset = Math.max(0, scrollOffset - DeepDrawersMenu.COLUMNS);
            setScrollOffset(newOffset);
            return true;
        }
        return false;
    }

    private boolean scrollDownSilent() {
        int maxScrollOffset = menu.getMaxScrollOffset();
        if (scrollOffset < maxScrollOffset) {
            int newOffset = Math.min(maxScrollOffset, scrollOffset + DeepDrawersMenu.COLUMNS);
            setScrollOffset(newOffset);
            return true;
        }
        return false;
    }

    private void setScrollOffset(int offset) {
        scrollOffset = offset;
        if (menu.isSearchFilterActive()) {
            menu.setFilterScrollOffset(offset);
            syncSearchStateToServer();
        } else {
            menu.setScrollOffset(offset);
            ModMessages.sendDeepDrawersScrollPacket(menu.getBlockPos(), offset);
        }
    }

    private void playButtonSound() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(
                    net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                            net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }
}

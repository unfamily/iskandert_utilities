package net.unfamily.iskautils.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.entity.FactoryBlockEntity;
import net.unfamily.iskautils.client.FactoryClientSourcesBootstrap;
import net.unfamily.iskautils.data.load.FactoryLoader;
import net.unfamily.iskautils.integration.jei.FactoryJeiRecipes;
import net.unfamily.iskautils.integration.jei.IskaUtilsJeiDynamicRefresh;
import net.unfamily.iskautils.network.ModMessages;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FactoryScreen extends AbstractContainerScreen<FactoryMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/backgrounds/factory.png");

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 190;

    private static final ResourceLocation SCROLLBAR_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/scrollbar.png");

    private static final ResourceLocation ENERGY_BAR_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("iska_utils", "textures/gui/energy_bar.png");
    private static final int GRID_COLS = 7;
    private static final int GRID_ROWS_VISIBLE = 3;
    private static final int CELL_STEP = 18;
    private static final int GRID_BTN_SIZE = 16;

    private static final int GRID_PIXEL_W = GRID_COLS * CELL_STEP;
    private static final int GRID_PIXEL_H = GRID_ROWS_VISIBLE * CELL_STEP;
    private static final int GRID_START_X = (GUI_WIDTH - GRID_PIXEL_W) / 2;
    private static final int GRID_START_Y =
            FactoryMenu.SLOT_INPUT_Y
                    + FactoryMenu.SLOT_SIZE
                    + 2
                    + Math.max(
                            0,
                            (FactoryMenu.PLAYER_INV_Y
                                            - 8
                                            - (FactoryMenu.SLOT_INPUT_Y + FactoryMenu.SLOT_SIZE + 2)
                                            - GRID_PIXEL_H)
                                    / 2);

    private static final int SCROLLBAR_WIDTH = 8;
    private static final int HANDLE_SIZE = 8;
    /** Middle track height (same as DeepDrawersScreen / SoundMufflerFilterScreen). */
    private static final int SCROLLBAR_HEIGHT = 34;

    /** Scrollbar to the right of the grid; whole column vertically centered on the 3 button rows. */
    private static final int SCROLLBAR_GAP_X = 2;
    private static final int SCROLLBAR_X = GRID_START_X + GRID_PIXEL_W + SCROLLBAR_GAP_X;

    private static final int ENERGY_BAR_WIDTH = 8;
    private static final int ENERGY_BAR_HEIGHT = 32;
    private static final int ENERGY_BAR_X = GRID_START_X - SCROLLBAR_GAP_X - ENERGY_BAR_WIDTH;

    private static final int REDSTONE_BUTTON_SIZE = 16;

    private static final int SCROLLBAR_COLUMN_TOTAL = HANDLE_SIZE + SCROLLBAR_HEIGHT + HANDLE_SIZE;
    private static final int BUTTON_UP_Y = GRID_START_Y + (GRID_PIXEL_H - SCROLLBAR_COLUMN_TOTAL) / 2;
    private static final int SCROLLBAR_Y = BUTTON_UP_Y + HANDLE_SIZE;
    private static final int BUTTON_DOWN_Y = SCROLLBAR_Y + SCROLLBAR_HEIGHT;

    /** Horizontally centered on the scroll-down button; vertically centered on the output slot row. */
    private static final int REDSTONE_BUTTON_X =
            SCROLLBAR_X + (SCROLLBAR_WIDTH - REDSTONE_BUTTON_SIZE) / 2;
    private static final int REDSTONE_BUTTON_Y =
            FactoryMenu.yCenteredInSlotRow(FactoryMenu.SLOT_OUTPUT_Y, REDSTONE_BUTTON_SIZE);

    private final List<AbstractButton> colorGridButtons = new ArrayList<>();
    private ItemIconButton redstoneModeButton;

    private int scrollOffset = 0;
    private boolean isDraggingHandle = false;
    private int dragStartY = 0;
    private int dragStartScrollOffset = 0;
    private int selectedIndex = -1;

    private int lastGridRebuildKey = Integer.MIN_VALUE;

    public FactoryScreen(FactoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        this.inventoryLabelY = 10000;
    }

    @Override
    public void containerTick() {
        super.containerTick();
        this.scrollOffset = menu.getScrollOffset();
        this.selectedIndex = menu.getSelectedColorIndex();
    }

    @Override
    protected void init() {
        if (minecraft != null) {
            FactoryJeiRecipes.reloadForClient(minecraft);
            FactoryClientSourcesBootstrap.ensureLoaded();
            IskaUtilsJeiDynamicRefresh.scheduleRefresh(minecraft);
        }
        super.init();
        this.scrollOffset = menu.getScrollOffset();
        this.selectedIndex = menu.getSelectedColorIndex();
        lastGridRebuildKey = Integer.MIN_VALUE;
        clearColorGridButtons();

        redstoneModeButton = addRenderableWidget(MachineGuiButtons.redstoneIconButton(
                leftPos + REDSTONE_BUTTON_X,
                topPos + REDSTONE_BUTTON_Y,
                b -> onRedstoneModePressed(false),
                menu::getRedstoneMode,
                false));
    }

    @Override
    public void removed() {
        clearColorGridButtons();
        super.removed();
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        ItemStack grid = findHoveredColorGridStack(mouseX, mouseY);
        if (!grid.isEmpty()) {
            guiGraphics.renderComponentTooltip(this.font, this.getTooltipFromContainerItem(grid), mouseX, mouseY);
            return;
        }
        if (menu.getMaxEnergyStored() > 0 && tryEnergyTooltip(guiGraphics, mouseX, mouseY)) {
            return;
        }
        super.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private int energyBarScreenY() {
        return topPos + BUTTON_UP_Y + (SCROLLBAR_COLUMN_TOTAL - ENERGY_BAR_HEIGHT) / 2;
    }

    private boolean tryEnergyTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int barX = leftPos + ENERGY_BAR_X;
        int barY = energyBarScreenY();
        if (mouseX >= barX && mouseX <= barX + ENERGY_BAR_WIDTH && mouseY >= barY && mouseY <= barY + ENERGY_BAR_HEIGHT) {
            int energy = menu.getEnergyStored();
            int maxEnergy = menu.getMaxEnergyStored();
            guiGraphics.renderTooltip(
                    this.font, Component.literal(String.format("%,d / %,d RF", energy, maxEnergy)), mouseX, mouseY);
            return true;
        }
        return false;
    }

    private void renderEnergyBar(GuiGraphics guiGraphics) {
        int maxEnergy = menu.getMaxEnergyStored();
        if (maxEnergy <= 0) {
            return;
        }
        int barX = leftPos + ENERGY_BAR_X;
        int barY = energyBarScreenY();
        guiGraphics.blit(ENERGY_BAR_TEXTURE, barX, barY, 8, 0, ENERGY_BAR_WIDTH, ENERGY_BAR_HEIGHT, 16, 32);
        int energy = menu.getEnergyStored();
        if (energy > 0) {
            int energyHeight = (energy * ENERGY_BAR_HEIGHT) / maxEnergy;
            int energyY = barY + (ENERGY_BAR_HEIGHT - energyHeight);
            guiGraphics.blit(ENERGY_BAR_TEXTURE, barX, energyY, 0, ENERGY_BAR_HEIGHT - energyHeight, ENERGY_BAR_WIDTH, energyHeight, 16, 32);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, GUI_WIDTH, GUI_HEIGHT);
        renderEnergyBar(guiGraphics);
        renderScrollbar(guiGraphics, mouseX, mouseY);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        ensureColorGridButtons();
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        if (redstoneModeButton != null && redstoneModeButton.isHovered()) {
            guiGraphics.renderTooltip(this.font,
                    MachineGuiButtons.redstoneTooltip(menu.getRedstoneMode(), false), mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    }

    private int gridRebuildKey() {
        ItemStack in = menu.getSlot(0).getItem();
        int inKey = in.isEmpty() ? 0 : ItemStack.hashItemAndComponents(in);
        return Objects.hash(leftPos, topPos, scrollOffset, getCurrentEntries().size(), inKey);
    }

    private void ensureColorGridButtons() {
        int k = gridRebuildKey();
        if (k == lastGridRebuildKey && colorGridButtons.size() == GRID_COLS * GRID_ROWS_VISIBLE) {
            return;
        }
        lastGridRebuildKey = k;
        rebuildColorGridButtons();
    }

    private void clearColorGridButtons() {
        for (AbstractButton b : colorGridButtons) {
            removeWidget(b);
        }
        colorGridButtons.clear();
    }

    private void rebuildColorGridButtons() {
        clearColorGridButtons();
        List<ItemStack> entries = getCurrentEntries();
        int visible = GRID_COLS * GRID_ROWS_VISIBLE;
        for (int i = 0; i < visible; i++) {
            int idx = scrollOffset + i;
            int col = i % GRID_COLS;
            int row = i / GRID_COLS;
            int cellX = leftPos + GRID_START_X + col * CELL_STEP;
            int cellY = topPos + GRID_START_Y + row * CELL_STEP;
            int bx = cellX + (CELL_STEP - GRID_BTN_SIZE) / 2;
            int by = cellY + (CELL_STEP - GRID_BTN_SIZE) / 2;
            boolean has = idx < entries.size();
            FactoryColorChoiceButton btn = new FactoryColorChoiceButton(bx, by, idx);
            if (!has) {
                btn.active = false;
            }
            colorGridButtons.add(btn);
            addRenderableWidget(btn);
        }
    }

    private ItemStack findHoveredColorGridStack(int mouseX, int mouseY) {
        List<ItemStack> entries = getCurrentEntries();
        int start = scrollOffset;
        int visible = GRID_COLS * GRID_ROWS_VISIBLE;
        for (int i = 0; i < visible; i++) {
            int idx = start + i;
            if (idx >= entries.size()) continue;
            int col = i % GRID_COLS;
            int row = i / GRID_COLS;
            int cellX = leftPos + GRID_START_X + col * CELL_STEP;
            int cellY = topPos + GRID_START_Y + row * CELL_STEP;
            int bx = cellX + (CELL_STEP - GRID_BTN_SIZE) / 2;
            int by = cellY + (CELL_STEP - GRID_BTN_SIZE) / 2;
            if (mouseX >= bx && mouseX < bx + GRID_BTN_SIZE && mouseY >= by && mouseY < by + GRID_BTN_SIZE) {
                return entries.get(idx);
            }
        }
        return ItemStack.EMPTY;
    }

    private List<ItemStack> getCurrentEntries() {
        ItemStack input = menu.getSlot(0).getItem();
        if (input.isEmpty()) {
            return List.of();
        }
        net.minecraft.server.level.ServerPlayer gatePlayer = null;
        FactoryBlockEntity be = menu.getBlockEntityOrNull();
        if (be != null) {
            gatePlayer = be.resolveOwnerPlayer();
        }
        if (gatePlayer == null
                && minecraft != null
                && minecraft.getSingleplayerServer() != null
                && minecraft.player != null) {
            gatePlayer = minecraft.getSingleplayerServer().getPlayerList().getPlayer(minecraft.player.getUUID());
        }
        return FactoryLoader.previewOutputs(input, minecraft.level, gatePlayer);
    }

    private void renderScrollbar(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int total = getCurrentEntries().size();
        int visible = GRID_COLS * GRID_ROWS_VISIBLE;
        if (total <= visible) return;

        int guiX = leftPos;
        int guiY = topPos;

        // Middle track: full 8×34 strip (same as DeepDrawersScreen).
        guiGraphics.blit(SCROLLBAR_TEXTURE, guiX + SCROLLBAR_X, guiY + SCROLLBAR_Y, 0, 0, SCROLLBAR_WIDTH, SCROLLBAR_HEIGHT, 32, 34);

        boolean upHovered = mouseX >= guiX + SCROLLBAR_X && mouseX < guiX + SCROLLBAR_X + SCROLLBAR_WIDTH
                && mouseY >= guiY + BUTTON_UP_Y && mouseY < guiY + BUTTON_UP_Y + HANDLE_SIZE;
        int upV = upHovered ? HANDLE_SIZE : 0;
        guiGraphics.blit(SCROLLBAR_TEXTURE, guiX + SCROLLBAR_X, guiY + BUTTON_UP_Y, SCROLLBAR_WIDTH * 2, upV, HANDLE_SIZE, HANDLE_SIZE, 32, 34);

        boolean downHovered = mouseX >= guiX + SCROLLBAR_X && mouseX < guiX + SCROLLBAR_X + SCROLLBAR_WIDTH
                && mouseY >= guiY + BUTTON_DOWN_Y && mouseY < guiY + BUTTON_DOWN_Y + HANDLE_SIZE;
        int downV = downHovered ? HANDLE_SIZE : 0;
        guiGraphics.blit(SCROLLBAR_TEXTURE, guiX + SCROLLBAR_X, guiY + BUTTON_DOWN_Y, SCROLLBAR_WIDTH * 3, downV, HANDLE_SIZE, HANDLE_SIZE, 32, 34);

        int maxScroll = getMaxScrollOffset(total);
        if (maxScroll > 0) {
            double ratio = (double) scrollOffset / maxScroll;
            int handleY = guiY + SCROLLBAR_Y + (int) (ratio * (SCROLLBAR_HEIGHT - HANDLE_SIZE));
            boolean handleHovered = mouseX >= guiX + SCROLLBAR_X && mouseX < guiX + SCROLLBAR_X + HANDLE_SIZE
                    && mouseY >= handleY && mouseY < handleY + HANDLE_SIZE;
            int handleV = handleHovered ? HANDLE_SIZE : 0;
            guiGraphics.blit(SCROLLBAR_TEXTURE, guiX + SCROLLBAR_X, handleY, SCROLLBAR_WIDTH, handleV, HANDLE_SIZE, HANDLE_SIZE, 32, 34);
        }
    }

    private int getMaxScrollOffset(int totalEntries) {
        int rows = (totalEntries + GRID_COLS - 1) / GRID_COLS;
        int maxRowOffset = Math.max(0, rows - GRID_ROWS_VISIBLE);
        return maxRowOffset * GRID_COLS;
    }

    private void onRedstoneModePressed(boolean backward) {
        ModMessages.sendFactoryRedstoneMode(menu.getSyncedBlockPos(), backward);
        playClick();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1 && redstoneModeButton != null && redstoneModeButton.isHovered()) {
            onRedstoneModePressed(true);
            return true;
        }
        if (button == 0) {
            if (handleScrollButtonClick(mouseX, mouseY)) return true;
            if (handleHandleClick(mouseX, mouseY)) return true;
            if (handleScrollbarClick(mouseX, mouseY)) return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void setSelectedIndex(int idx) {
        if (this.selectedIndex == idx) return;
        this.selectedIndex = idx;
        playClick();
        ModMessages.sendFactorySelectColor(menu.getSyncedBlockPos(), idx);
    }

    private void playClick() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F));
        }
    }

    private boolean handleScrollButtonClick(double mouseX, double mouseY) {
        int total = getCurrentEntries().size();
        int visible = GRID_COLS * GRID_ROWS_VISIBLE;
        if (total <= visible) return false;

        int guiX = this.leftPos;
        int guiY = this.topPos;
        if (mouseX >= guiX + SCROLLBAR_X && mouseX < guiX + SCROLLBAR_X + SCROLLBAR_WIDTH &&
            mouseY >= guiY + BUTTON_UP_Y && mouseY < guiY + BUTTON_UP_Y + HANDLE_SIZE) {
            int old = this.scrollOffset;
            scrollBy(-GRID_COLS);
            if (this.scrollOffset != old) {
                playClick();
            }
            return true;
        }
        if (mouseX >= guiX + SCROLLBAR_X && mouseX < guiX + SCROLLBAR_X + SCROLLBAR_WIDTH &&
            mouseY >= guiY + BUTTON_DOWN_Y && mouseY < guiY + BUTTON_DOWN_Y + HANDLE_SIZE) {
            int old = this.scrollOffset;
            scrollBy(GRID_COLS);
            if (this.scrollOffset != old) {
                playClick();
            }
            return true;
        }
        return false;
    }

    private boolean handleHandleClick(double mouseX, double mouseY) {
        int total = getCurrentEntries().size();
        int visible = GRID_COLS * GRID_ROWS_VISIBLE;
        if (total <= visible) return false;
        int maxScroll = getMaxScrollOffset(total);
        if (maxScroll <= 0) return false;

        int guiX = this.leftPos;
        int guiY = this.topPos;
        double ratio = (double) scrollOffset / maxScroll;
        int handleY = guiY + SCROLLBAR_Y + (int) (ratio * (SCROLLBAR_HEIGHT - HANDLE_SIZE));
        if (mouseX >= guiX + SCROLLBAR_X && mouseX < guiX + SCROLLBAR_X + HANDLE_SIZE &&
            mouseY >= handleY && mouseY < handleY + HANDLE_SIZE) {
            isDraggingHandle = true;
            dragStartY = (int) mouseY;
            dragStartScrollOffset = scrollOffset;
            return true;
        }
        return false;
    }

    private boolean handleScrollbarClick(double mouseX, double mouseY) {
        int total = getCurrentEntries().size();
        int visible = GRID_COLS * GRID_ROWS_VISIBLE;
        if (total <= visible) return false;
        int maxScroll = getMaxScrollOffset(total);
        if (maxScroll <= 0) return false;

        int guiX = this.leftPos;
        int guiY = this.topPos;
        if (mouseX >= guiX + SCROLLBAR_X && mouseX < guiX + SCROLLBAR_X + SCROLLBAR_WIDTH &&
            mouseY >= guiY + SCROLLBAR_Y && mouseY < guiY + SCROLLBAR_Y + SCROLLBAR_HEIGHT) {
            double clickTrack = (mouseY - (guiY + SCROLLBAR_Y)) - (HANDLE_SIZE / 2.0);
            double denom = Math.max(1.0, (double) (SCROLLBAR_HEIGHT - HANDLE_SIZE));
            double ratio = Math.max(0.0, Math.min(1.0, clickTrack / denom));
            int newOffset = (int) Math.round(ratio * maxScroll);
            newOffset = (newOffset / GRID_COLS) * GRID_COLS;
            int old = this.scrollOffset;
            setScrollOffset(newOffset);
            if (this.scrollOffset != old) {
                playClick();
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
        if (button == 0 && isDraggingHandle) {
            int total = getCurrentEntries().size();
            int maxScroll = getMaxScrollOffset(total);
            int deltaY = (int) mouseY - dragStartY;
            if (maxScroll > 0) {
                float ratio = (float) deltaY / (SCROLLBAR_HEIGHT - HANDLE_SIZE);
                int newOffset = dragStartScrollOffset + (int) (ratio * maxScroll);
                newOffset = (newOffset / GRID_COLS) * GRID_COLS;
                setScrollOffset(newOffset);
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (deltaY > 0) scrollBy(-GRID_COLS);
        else if (deltaY < 0) scrollBy(GRID_COLS);
        return true;
    }

    private void scrollBy(int delta) {
        int total = getCurrentEntries().size();
        int visible = GRID_COLS * GRID_ROWS_VISIBLE;
        if (total <= visible) return;
        int maxScroll = getMaxScrollOffset(total);
        setScrollOffset(Math.max(0, Math.min(maxScroll, scrollOffset + delta)));
    }

    private void setScrollOffset(int offset) {
        int total = getCurrentEntries().size();
        int maxScroll = getMaxScrollOffset(total);
        offset = Math.max(0, Math.min(maxScroll, offset));
        offset = (offset / GRID_COLS) * GRID_COLS;
        if (this.scrollOffset != offset) {
            this.scrollOffset = offset;
            ModMessages.sendFactoryScroll(menu.getSyncedBlockPos(), offset);
        }
    }

    /** 16x16 slot with dye icon; selected looks pressed (inset / darker). */
    private final class FactoryColorChoiceButton extends AbstractButton {
        private final int absoluteColorIndex;

        FactoryColorChoiceButton(int x, int y, int absoluteColorIndex) {
            super(x, y, GRID_BTN_SIZE, GRID_BTN_SIZE, Component.empty());
            this.absoluteColorIndex = absoluteColorIndex;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            List<ItemStack> entries = getCurrentEntries();
            boolean has = absoluteColorIndex >= 0 && absoluteColorIndex < entries.size();
            boolean selected = has && selectedIndex == absoluteColorIndex;
            int x = getX();
            int y = getY();
            int w = getWidth();
            int h = getHeight();
            if (selected) {
                graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF3A3A3A);
                graphics.fill(x, y, x + w, y + 1, 0xFF1A1A1A);
                graphics.fill(x, y + h - 1, x + w, y + h, 0xFF5A5A5A);
                graphics.fill(x, y, x + 1, y + h, 0xFF1A1A1A);
                graphics.fill(x + w - 1, y, x + w, y + h, 0xFF5A5A5A);
            } else {
                graphics.fill(x, y, x + w, y + 1, 0xFF8B8B8B);
                graphics.fill(x, y + h - 1, x + w, y + h, 0xFF2A2A2A);
                graphics.fill(x, y, x + 1, y + h, 0xFF8B8B8B);
                graphics.fill(x + w - 1, y, x + w, y + h, 0xFF2A2A2A);
                graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF404040);
            }
            if (has) {
                ItemStack stack = entries.get(absoluteColorIndex);
                // Keep item position fixed; pressed look is only the frame (no icon shift).
                graphics.renderItem(stack, x, y);
                graphics.renderItemDecorations(FactoryScreen.this.font, stack, x, y);
            }
        }

        @Override
        public void onPress() {
            List<ItemStack> entries = getCurrentEntries();
            if (absoluteColorIndex < 0 || absoluteColorIndex >= entries.size()) {
                return;
            }
            setSelectedIndex(absoluteColorIndex);
        }

        @Override
        protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput narration) {
            defaultButtonNarrationText(narration);
        }
    }
}

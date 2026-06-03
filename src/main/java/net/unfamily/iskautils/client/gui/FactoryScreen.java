package net.unfamily.iskautils.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.entity.FactoryBlockEntity;
import net.unfamily.iskautils.client.FactoryClientSourcesBootstrap;
import net.unfamily.iskautils.data.load.FactoryLoader;
import net.unfamily.iskautils.integration.jei.FactoryJeiRecipes;
import net.unfamily.iskautils.network.ModMessages;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FactoryScreen extends AbstractContainerScreen<FactoryMenu> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/backgrounds/factory.png");

    private static final Identifier SCROLLBAR_TEXTURE =
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/scrollbar.png");

    private static final Identifier ENERGY_BAR_TEXTURE =
            Identifier.fromNamespaceAndPath("iska_utils", "textures/gui/energy_bar.png");
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 190;

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

    /** Same scrollbar column geometry as DeepDrawersScreen / SoundMufflerFilterScreen (8 + 34 + 8 px). */
    private static final int SCROLLBAR_WIDTH = 8;
    private static final int HANDLE_SIZE = 8;
    private static final int SCROLLBAR_HEIGHT = 34;
    private static final int SCROLLBAR_ATLAS_W = 32;
    private static final int SCROLLBAR_ATLAS_H = 34;

    private static final int SCROLLBAR_GAP_X = 2;
    private static final int SCROLLBAR_X = GRID_START_X + GRID_PIXEL_W + SCROLLBAR_GAP_X;
    /** Mirrored column to the left of the color grid (same gap as scrollbar). */
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
        super(menu, playerInventory, title, GUI_WIDTH, GUI_HEIGHT);
        this.inventoryLabelY = 10000;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, GUI_WIDTH, GUI_HEIGHT);
        renderEnergyBar(guiGraphics);
        renderScrollbar(guiGraphics, mouseX, mouseY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        ensureColorGridButtons();
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        if (redstoneModeButton != null && redstoneModeButton.isMouseOver(mouseX, mouseY)) {
            MachineGuiButtons.renderTooltipLine(
                    guiGraphics, font, mouseX, mouseY,
                    MachineGuiButtons.redstoneTooltip(menu.getRedstoneMode(), false));
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {}

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        ItemStack grid = findHoveredColorGridStack(mouseX, mouseY);
        if (!grid.isEmpty()) {
            graphics.setTooltipForNextFrame(
                    this.font,
                    this.getTooltipFromContainerItem(grid),
                    grid.getTooltipImage(),
                    grid,
                    mouseX,
                    mouseY,
                    grid.get(DataComponents.TOOLTIP_STYLE));
            return;
        }
        if (tryEnergyTooltip(graphics, mouseX, mouseY)) {
            return;
        }
        super.extractTooltip(graphics, mouseX, mouseY);
    }

    private int energyBarScreenY() {
        return topPos + BUTTON_UP_Y + (SCROLLBAR_COLUMN_TOTAL - ENERGY_BAR_HEIGHT) / 2;
    }

    private boolean tryEnergyTooltip(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        int maxEnergy = menu.getMaxEnergyStored();
        if (maxEnergy <= 0) {
            return false;
        }
        int barX = leftPos + ENERGY_BAR_X;
        int barY = energyBarScreenY();
        if (mouseX >= barX && mouseX <= barX + ENERGY_BAR_WIDTH && mouseY >= barY && mouseY <= barY + ENERGY_BAR_HEIGHT) {
            int energy = menu.getEnergyStored();
            Component line = Component.literal(String.format("%,d / %,d RF", energy, maxEnergy));
            guiGraphics.setTooltipForNextFrame(
                    this.font,
                    List.of(line.getVisualOrderText()),
                    DefaultTooltipPositioner.INSTANCE,
                    mouseX,
                    mouseY,
                    true);
            return true;
        }
        return false;
    }

    private void renderEnergyBar(GuiGraphicsExtractor guiGraphics) {
        int maxEnergy = menu.getMaxEnergyStored();
        if (maxEnergy <= 0) {
            return;
        }
        int barX = leftPos + ENERGY_BAR_X;
        int barY = energyBarScreenY();
        guiGraphics.blit(
                RenderPipelines.GUI_TEXTURED,
                ENERGY_BAR_TEXTURE,
                barX,
                barY,
                8.0F,
                0.0F,
                ENERGY_BAR_WIDTH,
                ENERGY_BAR_HEIGHT,
                16,
                32);
        int energy = menu.getEnergyStored();
        if (energy > 0) {
            int energyHeight = (energy * ENERGY_BAR_HEIGHT) / maxEnergy;
            int energyY = barY + (ENERGY_BAR_HEIGHT - energyHeight);
            guiGraphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    ENERGY_BAR_TEXTURE,
                    barX,
                    energyY,
                    0.0F,
                    (float) (ENERGY_BAR_HEIGHT - energyHeight),
                    ENERGY_BAR_WIDTH,
                    energyHeight,
                    16,
                    32);
        }
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
            net.unfamily.iskautils.integration.jei.IskaUtilsJeiDynamicRefresh.scheduleRefresh(minecraft);
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

    private void onRedstoneModePressed(boolean backward) {
        ModMessages.sendFactoryRedstoneMode(menu.getSyncedBlockPos(), backward);
        playClick();
    }

    @Override
    public void removed() {
        clearColorGridButtons();
        super.removed();
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

    private int getMaxScrollOffset(int totalEntries) {
        int rows = (totalEntries + GRID_COLS - 1) / GRID_COLS;
        int maxRowOffset = Math.max(0, rows - GRID_ROWS_VISIBLE);
        return maxRowOffset * GRID_COLS;
    }

    private void renderScrollbar(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        int total = getCurrentEntries().size();
        int visible = GRID_COLS * GRID_ROWS_VISIBLE;
        if (total <= visible) return;

        int guiX = leftPos;
        int guiY = topPos;

        guiGraphics.blit(
                RenderPipelines.GUI_TEXTURED,
                SCROLLBAR_TEXTURE,
                guiX + SCROLLBAR_X,
                guiY + SCROLLBAR_Y,
                0.0F,
                0.0F,
                SCROLLBAR_WIDTH,
                SCROLLBAR_HEIGHT,
                SCROLLBAR_ATLAS_W,
                SCROLLBAR_ATLAS_H);

        boolean upHovered = mouseX >= guiX + SCROLLBAR_X && mouseX < guiX + SCROLLBAR_X + SCROLLBAR_WIDTH
                && mouseY >= guiY + BUTTON_UP_Y && mouseY < guiY + BUTTON_UP_Y + HANDLE_SIZE;
        int upV = upHovered ? HANDLE_SIZE : 0;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SCROLLBAR_TEXTURE, guiX + SCROLLBAR_X, guiY + BUTTON_UP_Y, (float) (SCROLLBAR_WIDTH * 2), (float) upV, HANDLE_SIZE, HANDLE_SIZE, 32, 34);

        boolean downHovered = mouseX >= guiX + SCROLLBAR_X && mouseX < guiX + SCROLLBAR_X + SCROLLBAR_WIDTH
                && mouseY >= guiY + BUTTON_DOWN_Y && mouseY < guiY + BUTTON_DOWN_Y + HANDLE_SIZE;
        int downV = downHovered ? HANDLE_SIZE : 0;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SCROLLBAR_TEXTURE, guiX + SCROLLBAR_X, guiY + BUTTON_DOWN_Y, (float) (SCROLLBAR_WIDTH * 3), (float) downV, HANDLE_SIZE, HANDLE_SIZE, 32, 34);

        int maxScroll = getMaxScrollOffset(total);
        if (maxScroll > 0) {
            double ratio = (double) scrollOffset / maxScroll;
            int handleY = guiY + SCROLLBAR_Y + (int) (ratio * (SCROLLBAR_HEIGHT - HANDLE_SIZE));
            boolean handleHovered = mouseX >= guiX + SCROLLBAR_X && mouseX < guiX + SCROLLBAR_X + HANDLE_SIZE
                    && mouseY >= handleY && mouseY < handleY + HANDLE_SIZE;
            int handleV = handleHovered ? HANDLE_SIZE : 0;
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SCROLLBAR_TEXTURE, guiX + SCROLLBAR_X, handleY, (float) SCROLLBAR_WIDTH, (float) handleV, HANDLE_SIZE, HANDLE_SIZE, 32, 34);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (handleMouseClicked(event.x(), event.y(), event.button())) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private boolean handleMouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1 && redstoneModeButton != null && redstoneModeButton.isMouseOver(mouseX, mouseY)) {
            onRedstoneModePressed(true);
            return true;
        }
        if (button != 0 && button != 1) return false;
        if (handleScrollButtonClick(mouseX, mouseY)) return true;
        if (handleHandleClick(mouseX, mouseY)) return true;
        if (handleScrollbarClick(mouseX, mouseY)) return true;
        return false;
    }

    private void setSelectedIndex(int idx) {
        if (this.selectedIndex == idx) return;
        this.selectedIndex = idx;
        playClick();
        ModMessages.sendFactorySelectColor(menu.getSyncedBlockPos(), idx);
    }

    private void playClick() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(
                    net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                            net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
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
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0 && isDraggingHandle) {
            isDraggingHandle = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (event.button() == 0 && isDraggingHandle) {
            int total = getCurrentEntries().size();
            int maxScroll = getMaxScrollOffset(total);
            int deltaY = (int) event.y() - dragStartY;
            if (maxScroll > 0) {
                float ratio = (float) deltaY / (SCROLLBAR_HEIGHT - HANDLE_SIZE);
                int newOffset = dragStartScrollOffset + (int) (ratio * maxScroll);
                newOffset = (newOffset / GRID_COLS) * GRID_COLS;
                setScrollOffset(newOffset);
            }
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
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

    private final class FactoryColorChoiceButton extends AbstractButton {
        private final int absoluteColorIndex;

        FactoryColorChoiceButton(int x, int y, int absoluteColorIndex) {
            super(x, y, GRID_BTN_SIZE, GRID_BTN_SIZE, Component.empty());
            this.absoluteColorIndex = absoluteColorIndex;
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
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
                graphics.item(stack, x, y);
                graphics.itemDecorations(FactoryScreen.this.font, stack, x, y);
            }
        }

        @Override
        public void onPress(InputWithModifiers input) {
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

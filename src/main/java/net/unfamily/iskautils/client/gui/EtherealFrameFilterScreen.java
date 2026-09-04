package net.unfamily.iskautils.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.entity.EtherealFrameBlockEntity;
import net.unfamily.iskautils.network.ModMessages;
import net.unfamily.iskautils.util.EtherealFrameFilterMatcher;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;

/**
 * Filter screen for Ethereal Frame: searchable entity-type list with Allow/Deny toggle.
 * UX cloned from SoundMufflerFilterScreen adapted for entity types.
 */
public class EtherealFrameFilterScreen extends AbstractContainerScreen<EtherealFrameFilterMenu> {

    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(
            IskaUtils.MOD_ID, "textures/gui/backgrounds/sound_muffler.png");
    private static final ResourceLocation ENTRY_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            IskaUtils.MOD_ID, "textures/gui/entry_low_wide_wide.png");
    private static final ResourceLocation SCROLLBAR_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            IskaUtils.MOD_ID, "textures/gui/scrollbar.png");

    private static final int GUI_WIDTH = 230;
    private static final int GUI_HEIGHT = 180;
    private static final int BORDER_MARGIN = 9;
    private static final int ENTRY_WIDTH = 200;
    private static final int ENTRY_HEIGHT = 12;
    private static final int ENTRY_TEX_WIDTH = 200;
    private static final int ENTRY_TEX_HEIGHT = 12;
    private static final int ENTRIES_START_X = BORDER_MARGIN;
    private static final int ENTRIES_START_Y = 30;
    private static final int GAP_ENTRY_SCROLLBAR = 4;
    private static final int SCROLLBAR_WIDTH = 8;
    private static final int HANDLE_SIZE = 8;
    private static final int SCROLLBAR_X = ENTRIES_START_X + ENTRY_WIDTH + GAP_ENTRY_SCROLLBAR;
    private static final int SEARCH_BAR_HEIGHT = 20;
    private static final int LIST_ENTRIES_START_Y = ENTRIES_START_Y + SEARCH_BAR_HEIGHT + 2;
    private static final int BUTTON_UP_Y = LIST_ENTRIES_START_Y;
    private static final int SCROLLBAR_Y = BUTTON_UP_Y + HANDLE_SIZE;
    private static final int SCROLLBAR_HEIGHT = 34;
    private static final int BUTTON_DOWN_Y = SCROLLBAR_Y + SCROLLBAR_HEIGHT;
    private static final int VISIBLE_ENTRIES = 8;
    private static final int BOTTOM_ROW_Y = 154;
    private static final int BOTTOM_BUTTON_H = 18;
    private static final int BOTTOM_BUTTON_GAP = 4;
    private static final int LIGHT_BUTTON_SIZE = 18;
    /** Bottom row flush with entries: Deny/Allow, Apply, Cancel, Light. */
    private static final int BOTTOM_ROW_START_X = ENTRIES_START_X;
    private static final int BOTTOM_BUTTON_W =
            (ENTRY_WIDTH - LIGHT_BUTTON_SIZE - BOTTOM_BUTTON_GAP * 3) / 3;
    private static final int CLOSE_BUTTON_MARGIN = 5;
    private static final int CLOSE_BUTTON_SIZE = 12;
    private static final int CLOSE_BUTTON_X = GUI_WIDTH - CLOSE_BUTTON_SIZE - CLOSE_BUTTON_MARGIN;
    private static final int CLOSE_BUTTON_Y = CLOSE_BUTTON_MARGIN;

    private final List<String> allEntityTypeIds = new ArrayList<>();
    private final List<String> filteredIds = new ArrayList<>();
    private final Set<String> selectedIds = new HashSet<>();
    private int scrollOffset = 0;
    private boolean isDraggingHandle = false;
    private int dragStartY = 0;
    private int dragStartScrollOffset = 0;
    private String lastSearchText = "";

    private EditBox searchBox;
    private Button denyAllowButton;
    private Button saveButton;
    private Button cancelButton;
    private ItemIconButton lightButton;
    private Button closeButton;
    private final Button[] dotButtons = new Button[VISIBLE_ENTRIES];

    public EtherealFrameFilterScreen(EtherealFrameFilterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    private void loadEntityTypeIds() {
        allEntityTypeIds.clear();
        allEntityTypeIds.addAll(EtherealFrameFilterMatcher.SPECIAL_KEYS);
        if (minecraft != null && minecraft.level != null) {
            minecraft.level.registryAccess()
                    .lookupOrThrow(Registries.ENTITY_TYPE)
                    .listTagIds()
                    .map(tagKey -> "#" + tagKey.location())
                    .sorted()
                    .forEach(allEntityTypeIds::add);
        } else {
            allEntityTypeIds.addAll(EtherealFrameFilterMatcher.COMMON_ENTITY_TAGS);
        }
        StreamSupport.stream(BuiltInRegistries.ENTITY_TYPE.spliterator(), false)
                .map(et -> BuiltInRegistries.ENTITY_TYPE.getKey(et).toString())
                .sorted()
                .forEach(allEntityTypeIds::add);
        EtherealFrameBlockEntity be = menu.getBlockEntityFromLevel(minecraft != null ? minecraft.level : null);
        selectedIds.clear();
        if (be != null) {
            selectedIds.addAll(be.getFilterEntityTypes());
            for (String id : be.getFilterEntityTypes()) {
                if (!allEntityTypeIds.contains(id)) {
                    allEntityTypeIds.add(id);
                }
            }
        }
        applySearch();
    }

    private void applySearch() {
        String q = searchBox != null ? searchBox.getValue() : "";
        if (q == null) q = "";
        String lower = q.toLowerCase().trim();
        filteredIds.clear();
        for (String id : allEntityTypeIds) {
            if (!(lower.isEmpty() || id.toLowerCase().contains(lower))) continue;
            if (selectedIds.contains(id)) filteredIds.add(id);
        }
        for (String id : allEntityTypeIds) {
            if (!(lower.isEmpty() || id.toLowerCase().contains(lower))) continue;
            if (!selectedIds.contains(id)) filteredIds.add(id);
        }
        scrollOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, filteredIds.size() - VISIBLE_ENTRIES)));
    }

    @Override
    protected void init() {
        super.init();
        searchBox = new EditBox(font, leftPos + ENTRIES_START_X, topPos + ENTRIES_START_Y,
                ENTRY_WIDTH, SEARCH_BAR_HEIGHT,
                Component.translatable("gui.iska_utils.ethereal_frame.search"));
        searchBox.setMaxLength(256);
        searchBox.setBordered(true);
        searchBox.setHint(Component.translatable("gui.iska_utils.ethereal_frame.search_hint"));
        searchBox.setResponder(s -> applySearch());
        addRenderableWidget(searchBox);
        loadEntityTypeIds();
        lastSearchText = searchBox.getValue() != null ? searchBox.getValue() : "";

        int buttonY = topPos + BOTTOM_ROW_Y;
        int x1 = leftPos + BOTTOM_ROW_START_X;
        int x2 = x1 + BOTTOM_BUTTON_W + BOTTOM_BUTTON_GAP;
        int x3 = x2 + BOTTOM_BUTTON_W + BOTTOM_BUTTON_GAP;
        int x4 = x3 + BOTTOM_BUTTON_W + BOTTOM_BUTTON_GAP;

        denyAllowButton = Button.builder(
                Component.translatable("gui.iska_utils.ethereal_frame.allow_list"),
                btn -> onAllowDenyClicked())
                .bounds(x1, buttonY, BOTTOM_BUTTON_W, BOTTOM_BUTTON_H).build();
        saveButton = Button.builder(
                Component.translatable("gui.iska_utils.structure_placer.apply"),
                btn -> handleApply())
                .bounds(x2, buttonY, BOTTOM_BUTTON_W, BOTTOM_BUTTON_H).build();
        cancelButton = Button.builder(
                Component.translatable("gui.iska_utils.structure_placer.cancel"),
                btn -> onClose())
                .bounds(x3, buttonY, BOTTOM_BUTTON_W, BOTTOM_BUTTON_H).build();
        lightButton = new ItemIconButton(
                x4,
                buttonY,
                LIGHT_BUTTON_SIZE,
                btn -> onLightClicked(),
                this::lightIconStack,
                Component.empty());
        addRenderableWidget(denyAllowButton);
        addRenderableWidget(saveButton);
        addRenderableWidget(cancelButton);
        addRenderableWidget(lightButton);
        refreshModeButton();
        refreshLightButton();

        closeButton = Button.builder(Component.literal("✕"), btn -> onClose())
                .bounds(leftPos + CLOSE_BUTTON_X, topPos + CLOSE_BUTTON_Y, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE)
                .build();
        addRenderableWidget(closeButton);

        for (int i = 0; i < VISIBLE_ENTRIES; i++) {
            final int row = i;
            dotButtons[i] = addRenderableWidget(
                    MachineGuiButtons.selectionDot(0, 0, false, b -> onDotPressed(row)));
            dotButtons[i].visible = false;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (MachineGuiInput.handleContainerKeyPressed(this, keyCode, scanCode, modifiers, isDraggingHandle, searchBox))
            return true;
        if (keyCode == 256) { onClose(); return true; }
        if (minecraft != null && minecraft.options.keyInventory.matches(keyCode, scanCode)) { onClose(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void handleApply() {
        BlockPos pos = menu.getBlockPos();
        if (pos.equals(BlockPos.ZERO)) return;
        ModMessages.sendEtherealFrameFilterUpdatePacket(pos, new ArrayList<>(selectedIds));
        onClose();
    }

    private void onAllowDenyClicked() {
        BlockPos pos = menu.getBlockPos();
        if (pos.equals(BlockPos.ZERO)) return;
        ModMessages.sendEtherealFrameModeTogglePacket(pos);
        // Optimistic UI flip; server sync will confirm
        EtherealFrameBlockEntity be = menu.getBlockEntityFromLevel(minecraft != null ? minecraft.level : null);
        if (be != null) {
            be.setAllowMode(!be.isAllowMode());
        }
        refreshModeButton();
        playClick();
    }

    private void onLightClicked() {
        BlockPos pos = menu.getBlockPos();
        if (pos.equals(BlockPos.ZERO)) {
            return;
        }
        ModMessages.sendEtherealFrameLightTogglePacket(pos);
        EtherealFrameBlockEntity be = menu.getBlockEntityFromLevel(minecraft != null ? minecraft.level : null);
        if (be != null) {
            be.setBlocksLight(!be.blocksLight());
        }
        refreshLightButton();
        playClick();
    }

    private ItemStack lightIconStack() {
        EtherealFrameBlockEntity be = menu.getBlockEntityFromLevel(minecraft != null ? minecraft.level : null);
        boolean blocks = be != null && be.blocksLight();
        return new ItemStack(blocks ? Items.TINTED_GLASS : Items.GLASS);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (searchBox != null) {
            String current = searchBox.getValue() != null ? searchBox.getValue() : "";
            if (!current.equals(lastSearchText)) {
                lastSearchText = current;
                applySearch();
            }
        }
        if (denyAllowButton != null) refreshModeButton();
        if (lightButton != null) refreshLightButton();
        layoutDots();
    }

    private void refreshModeButton() {
        EtherealFrameBlockEntity be = menu.getBlockEntityFromLevel(minecraft != null ? minecraft.level : null);
        boolean allow = be == null || be.isAllowMode();
        denyAllowButton.setMessage(Component.translatable(
                allow ? "gui.iska_utils.ethereal_frame.allow_list"
                      : "gui.iska_utils.ethereal_frame.deny_list"));
        denyAllowButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable(allow
                        ? "gui.iska_utils.ethereal_frame.mode.allow.tooltip"
                        : "gui.iska_utils.ethereal_frame.mode.deny.tooltip")));
    }

    private void refreshLightButton() {
        EtherealFrameBlockEntity be = menu.getBlockEntityFromLevel(minecraft != null ? minecraft.level : null);
        boolean blocks = be != null && be.blocksLight();
        lightButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable(blocks
                        ? "gui.iska_utils.ethereal_frame.light.block.tooltip"
                        : "gui.iska_utils.ethereal_frame.light.pass.tooltip")));
    }

    private void layoutDots() {
        for (int i = 0; i < VISIBLE_ENTRIES; i++) {
            int idx = scrollOffset + i;
            Button dot = dotButtons[i];
            if (idx >= filteredIds.size()) { dot.visible = false; continue; }
            int ex = leftPos + ENTRIES_START_X;
            int ey = topPos + LIST_ENTRIES_START_Y + i * ENTRY_HEIGHT;
            dot.setX(MachineGuiButtons.filterSelectionDotX(ex, ENTRY_WIDTH));
            dot.setY(MachineGuiButtons.structureSelectionDotY(ey, ENTRY_HEIGHT));
            dot.visible = true;
            MachineGuiButtons.updateSelectionDot(dot, selectedIds.contains(filteredIds.get(idx)));
        }
    }

    private void onDotPressed(int visibleRow) {
        int idx = scrollOffset + visibleRow;
        if (idx < 0 || idx >= filteredIds.size()) return;
        String id = filteredIds.get(idx);
        if (selectedIds.contains(id)) selectedIds.remove(id);
        else selectedIds.add(id);
        playClick();
    }

    private void playClick() {
        if (minecraft != null)
            minecraft.getSoundManager().play(
                    net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                            SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        g.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, GUI_WIDTH, GUI_HEIGHT);
        renderEntries(g, mx, my);
        renderScrollbar(g, mx, my);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        Component title = Component.translatable("gui.iska_utils.ethereal_frame.filter_title");
        int tx = (imageWidth - font.width(title)) / 2;
        g.drawString(font, title, tx, 8, 0x404040, false);
    }

    private void renderEntries(GuiGraphics g, int mx, int my) {
        for (int i = 0; i < VISIBLE_ENTRIES; i++) {
            int idx = scrollOffset + i;
            int ex = leftPos + ENTRIES_START_X;
            int ey = topPos + LIST_ENTRIES_START_Y + i * ENTRY_HEIGHT;
            g.blit(ENTRY_TEXTURE, ex, ey, 0, 0, ENTRY_WIDTH, ENTRY_HEIGHT, ENTRY_TEX_WIDTH, ENTRY_TEX_HEIGHT);
            if (idx < filteredIds.size()) {
                String id = filteredIds.get(idx);
                int maxW = ENTRY_WIDTH - 8 - MachineGuiButtons.DOT_SIZE - 6;
                String display = font.plainSubstrByWidth(id, maxW);
                if (display.length() < id.length()) display += "..";
                g.drawString(font, display, ex + 4, ey + (ENTRY_HEIGHT - font.lineHeight) / 2, 0x404040, false);
            }
        }
    }

    private void renderScrollbar(GuiGraphics g, int mx, int my) {
        int sbx = leftPos + SCROLLBAR_X;
        int sby = topPos + SCROLLBAR_Y;
        int upY = topPos + BUTTON_UP_Y;
        int downY = topPos + BUTTON_DOWN_Y;
        g.blit(SCROLLBAR_TEXTURE, sbx, sby, 0, 0, SCROLLBAR_WIDTH, SCROLLBAR_HEIGHT, 32, 34);
        boolean upH = mx >= sbx && mx < sbx + HANDLE_SIZE && my >= upY && my < upY + HANDLE_SIZE;
        g.blit(SCROLLBAR_TEXTURE, sbx, upY, SCROLLBAR_WIDTH * 2, upH ? HANDLE_SIZE : 0, HANDLE_SIZE, HANDLE_SIZE, 32, 34);
        boolean downH = mx >= sbx && mx < sbx + HANDLE_SIZE && my >= downY && my < downY + HANDLE_SIZE;
        g.blit(SCROLLBAR_TEXTURE, sbx, downY, SCROLLBAR_WIDTH * 3, downH ? HANDLE_SIZE : 0, HANDLE_SIZE, HANDLE_SIZE, 32, 34);
        int total = filteredIds.size();
        float ratio = total <= VISIBLE_ENTRIES ? 0 : (float) scrollOffset / (total - VISIBLE_ENTRIES);
        int hy = sby + (int) (ratio * (SCROLLBAR_HEIGHT - HANDLE_SIZE));
        boolean hH = mx >= sbx && mx < sbx + HANDLE_SIZE && my >= hy && my < hy + HANDLE_SIZE;
        g.blit(SCROLLBAR_TEXTURE, sbx, hy, SCROLLBAR_WIDTH, hH ? HANDLE_SIZE : 0, HANDLE_SIZE, HANDLE_SIZE, 32, 34);
    }

    private void scrollUp() { if (scrollOffset > 0) scrollOffset--; }
    private void scrollDown() {
        if (filteredIds.size() > VISIBLE_ENTRIES && scrollOffset < filteredIds.size() - VISIBLE_ENTRIES)
            scrollOffset++;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (MachineGuiInput.clearEditBoxOnRightClick(mx, my, button, searchBox)) {
            return true;
        }
        if (button == 0) {
            int sbx = leftPos + SCROLLBAR_X;
            if (filteredIds.size() > VISIBLE_ENTRIES) {
                int upY = topPos + BUTTON_UP_Y;
                if (mx >= sbx && mx < sbx + HANDLE_SIZE && my >= upY && my < upY + HANDLE_SIZE) {
                    scrollUp(); playClick(); MachineGuiInput.markScrollbarPressed(); return true;
                }
                int downY = topPos + BUTTON_DOWN_Y;
                if (mx >= sbx && mx < sbx + HANDLE_SIZE && my >= downY && my < downY + HANDLE_SIZE) {
                    scrollDown(); playClick(); MachineGuiInput.markScrollbarPressed(); return true;
                }
            }
            if (mx >= sbx && mx < sbx + HANDLE_SIZE && my >= topPos + SCROLLBAR_Y && my < topPos + BUTTON_DOWN_Y) {
                isDraggingHandle = true;
                dragStartY = (int) my;
                dragStartScrollOffset = scrollOffset;
                MachineGuiInput.markScrollbarPressed();
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button == 0) { MachineGuiInput.clearScrollbarPressed(); isDraggingHandle = false; }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        int areaX = leftPos + ENTRIES_START_X;
        int areaY = topPos + LIST_ENTRIES_START_Y;
        if (mx >= areaX && mx < areaX + ENTRY_WIDTH + 20
                && my >= areaY && my < areaY + VISIBLE_ENTRIES * ENTRY_HEIGHT) {
            if (sy > 0) scrollUp();
            else if (sy < 0) scrollDown();
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public void mouseMoved(double mx, double my) {
        if (isDraggingHandle && filteredIds.size() > VISIBLE_ENTRIES) {
            int delta = (int) my - dragStartY;
            int maxOff = filteredIds.size() - VISIBLE_ENTRIES;
            int range = SCROLLBAR_HEIGHT - HANDLE_SIZE;
            if (range > 0)
                scrollOffset = Math.max(0, Math.min(maxOff,
                        dragStartScrollOffset + Math.round((float) delta / range * maxOff)));
        }
        super.mouseMoved(mx, my);
    }
}

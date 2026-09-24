package net.unfamily.iskautils.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.entity.SoundMufflerBlockEntity;
import net.unfamily.iskautils.network.ModMessages;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;

public class SoundMufflerFilterScreen extends AbstractContainerScreen<SoundMufflerFilterMenu> {

    // Same background as main Sound Muffler GUI (230x180)
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/backgrounds/sound_muffler.png");
    private static final ResourceLocation ENTRY_TEXTURE = ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/entry_low_wide_wide.png");
    // Same size as main muffler; 9px margin each side => entry = 230 - 9 - 4 - 8 - 9 = 200
    private static final int BORDER_MARGIN = 9;
    private static final int GUI_WIDTH = 230;
    private static final int GUI_HEIGHT = 180;
    private static final int ENTRY_WIDTH = 200;
    private static final int ENTRY_HEIGHT = 12;
    /** Entry texture is 200x12 (no stretch) */
    private static final int ENTRY_TEX_WIDTH = 200;
    private static final int ENTRY_TEX_HEIGHT = 12;
    private static final int ENTRIES_START_X = BORDER_MARGIN;
    private static final int ENTRIES_START_Y = 30;
    private static final int ENTRY_SPACING = 0;
    private static final int GAP_ENTRY_SCROLLBAR = 4;
    private static final int SCROLLBAR_WIDTH = GuiScroller.SCROLLER_WIDTH;
    private static final int SCROLLER_HEIGHT = GuiScroller.SCROLLER_HEIGHT;
    private static final int SCROLL_ARROW_SIZE = GuiScroller.SCROLL_ARROW_SIZE;
    private static final int SCROLLBAR_X = ENTRIES_START_X + ENTRY_WIDTH + GAP_ENTRY_SCROLLBAR;
    private static final int SEARCH_BAR_Y = ENTRIES_START_Y;
    private static final int SEARCH_BAR_HEIGHT = 20;
    private static final int LIST_ENTRIES_START_Y = ENTRIES_START_Y + SEARCH_BAR_HEIGHT + 2;
    // 8 entries fit in 180px height
    private static final int VISIBLE_ENTRIES = 8;
    private static final int BUTTON_UP_Y = LIST_ENTRIES_START_Y;
    private static final int BUTTON_DOWN_Y = GuiScroller.buttonDownY(BUTTON_UP_Y, VISIBLE_ENTRIES, ENTRY_HEIGHT);
    private static final int SCROLLBAR_Y = GuiScroller.trackY(BUTTON_UP_Y);
    private static final int SCROLLBAR_HEIGHT = GuiScroller.trackHeight(BUTTON_UP_Y, BUTTON_DOWN_Y);
    // Same height as the two buttons on main Sound Muffler screen (BOTTOM_BUTTONS_Y = 154)
    private static final int BOTTOM_ROW_Y = 154;
    private static final int BOTTOM_BUTTON_W = 52;
    private static final int BOTTOM_BUTTON_H = 18;
    private static final int BOTTOM_BUTTON_GAP = 6;
    // Three buttons: Deny/Allow, Apply, Cancel (narrower to fit inside filter area)
    private static final int THREE_BUTTONS_W = BOTTOM_BUTTON_W * 3 + BOTTOM_BUTTON_GAP * 2;
    private static final int BOTTOM_ROW_START_X = (GUI_WIDTH - THREE_BUTTONS_W) / 2;
    // Same position as main Sound Muffler GUI (CLOSE_BUTTON_MARGIN = 5 there)
    private static final int CLOSE_BUTTON_MARGIN = 5;
    private static final int CLOSE_BUTTON_SIZE = 12;
    private static final int CLOSE_BUTTON_X = GUI_WIDTH - CLOSE_BUTTON_SIZE - CLOSE_BUTTON_MARGIN;
    private static final int CLOSE_BUTTON_Y = CLOSE_BUTTON_MARGIN;

    private final List<String> allSoundIds = new ArrayList<>();
    private final List<String> filteredSoundIds = new ArrayList<>();
    private final Set<String> selectedSoundIds = new HashSet<>();
    private int scrollOffset = 0;
    private boolean isDraggingHandle = false;
    private int dragStartY = 0;
    private int dragStartScrollOffset = 0;
    private String lastSearchText = "";

    private EditBox searchBox;
    private Button denyAllowListButton;
    private Button saveButton;
    private Button cancelButton;
    private Button closeButton;
    private Button scrollUpButton;
    private Button scrollDownButton;
    private final Button[] selectionDotButtons = new Button[VISIBLE_ENTRIES];

    /** Parent screen to return to on Apply/Cancel (e.g. main Sound Muffler screen). If null, onClose() is used. */
    private final Screen parentScreen;

    public SoundMufflerFilterScreen(SoundMufflerFilterMenu menu, Inventory playerInventory, Component title) {
        this(menu, playerInventory, title, null);
    }

    public SoundMufflerFilterScreen(SoundMufflerFilterMenu menu, Inventory playerInventory, Component title, Screen parentScreen) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        this.parentScreen = parentScreen;
    }

    private void loadSoundIds() {
        allSoundIds.clear();
        StreamSupport.stream(BuiltInRegistries.SOUND_EVENT.spliterator(), false)
                .map(se -> BuiltInRegistries.SOUND_EVENT.getKey(se).toString())
                .sorted()
                .forEach(allSoundIds::add);
        SoundMufflerBlockEntity be = menu.getBlockEntityFromLevel(minecraft != null ? minecraft.level : null);
        selectedSoundIds.clear();
        if (be != null) selectedSoundIds.addAll(be.getFilterSoundIds());
        selectedSoundIds.retainAll(allSoundIds);
        applySearchFilter();
    }

    private void applySearchFilter() {
        String q = searchBox != null ? searchBox.getValue() : "";
        filteredSoundIds.clear();
        if (q == null) q = "";
        String lower = q.toLowerCase().trim();
        for (String id : allSoundIds) {
            if (!(lower.isEmpty() || id.toLowerCase().contains(lower))) continue;
            if (selectedSoundIds.contains(id)) filteredSoundIds.add(id);
        }
        for (String id : allSoundIds) {
            if (!(lower.isEmpty() || id.toLowerCase().contains(lower))) continue;
            if (!selectedSoundIds.contains(id)) filteredSoundIds.add(id);
        }
        scrollOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, filteredSoundIds.size() - VISIBLE_ENTRIES)));
        updateScrollArrowState();
    }

    @Override
    protected void init() {
        super.init();
        int searchH = 20;
        searchBox = new EditBox(font, leftPos + ENTRIES_START_X, topPos + SEARCH_BAR_Y, ENTRY_WIDTH, searchH,
                Component.translatable("gui.iska_utils.sound_muffler.search"));
        searchBox.setMaxLength(256);
        searchBox.setBordered(true);
        searchBox.setHint(Component.translatable("gui.iska_utils.sound_muffler.search_hint"));
        searchBox.setResponder(s -> applySearchFilter());
        addRenderableWidget(searchBox);
        loadSoundIds();
        lastSearchText = searchBox.getValue() != null ? searchBox.getValue() : "";
        int buttonY = topPos + BOTTOM_ROW_Y;
        int x1 = leftPos + BOTTOM_ROW_START_X;
        int x2 = x1 + BOTTOM_BUTTON_W + BOTTOM_BUTTON_GAP;
        int x3 = x2 + BOTTOM_BUTTON_W + BOTTOM_BUTTON_GAP;
        denyAllowListButton = Button.builder(Component.translatable("gui.iska_utils.sound_muffler.deny_list"), btn -> onDenyAllowListClicked())
                .bounds(x1, buttonY, BOTTOM_BUTTON_W, BOTTOM_BUTTON_H)
                .build();
        saveButton = Button.builder(Component.translatable("gui.iska_utils.structure_placer.apply"), btn -> handleApply())
                .bounds(x2, buttonY, BOTTOM_BUTTON_W, BOTTOM_BUTTON_H)
                .build();
        cancelButton = Button.builder(Component.translatable("gui.iska_utils.structure_placer.cancel"), btn -> handleCancel())
                .bounds(x3, buttonY, BOTTOM_BUTTON_W, BOTTOM_BUTTON_H)
                .build();
        addRenderableWidget(denyAllowListButton);
        addRenderableWidget(saveButton);
        addRenderableWidget(cancelButton);
        refreshModeButton();
        closeButton = Button.builder(Component.literal("✕"), btn -> handleCancel())
                .bounds(leftPos + CLOSE_BUTTON_X, topPos + CLOSE_BUTTON_Y, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE)
                .build();
        addRenderableWidget(closeButton);

        scrollUpButton = addRenderableWidget(GuiScroller.createUpButton(
                leftPos + SCROLLBAR_X, topPos + BUTTON_UP_Y, this::scrollUp));
        scrollDownButton = addRenderableWidget(GuiScroller.createDownButton(
                leftPos + SCROLLBAR_X, topPos + BUTTON_DOWN_Y, this::scrollDown));
        updateScrollArrowState();

        for (int i = 0; i < VISIBLE_ENTRIES; i++) {
            final int row = i;
            selectionDotButtons[i] = addRenderableWidget(MachineGuiButtons.selectionDot(0, 0, false, b -> onSelectionDotPressed(row)));
            selectionDotButtons[i].visible = false;
        }
    }

    private void updateScrollArrowState() {
        GuiScroller.setArrowActive(scrollUpButton, scrollDownButton, filteredSoundIds.size() > VISIBLE_ENTRIES);
    }

    /**
     * ESC returns to parent; inventory key (E) returns to parent unless search box is focused.
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (MachineGuiInput.handleContainerKeyPressed(this, keyCode, scanCode, modifiers, isDraggingHandle, searchBox)) {
            return true;
        }
        if (keyCode == 256) {
            handleCancel();
            return true;
        }
        if (minecraft != null && minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            handleCancel();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void handleApply() {
        BlockPos pos = menu.getBlockPos();
        if (pos.equals(BlockPos.ZERO)) return;
        ModMessages.sendSoundMufflerFilterUpdatePacket(pos, new ArrayList<>(selectedSoundIds));
        returnToParent();
    }

    private void handleCancel() {
        returnToParent();
    }

    private void returnToParent() {
        if (minecraft != null && parentScreen != null) {
            minecraft.setScreen(parentScreen);
        } else {
            onClose();
        }
    }

    /** Play click for custom UI only (scrollbar, entry toggle). Vanilla Button widgets already play their own. */
    private void playButtonSound() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(
                    net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    private void onDenyAllowListClicked() {
        BlockPos pos = menu.getBlockPos();
        if (pos.equals(BlockPos.ZERO)) return;
        ModMessages.sendSoundMufflerModeTogglePacket(pos);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (searchBox != null) {
            String current = searchBox.getValue() != null ? searchBox.getValue() : "";
            if (!current.equals(lastSearchText)) {
                lastSearchText = current;
                applySearchFilter();
            }
        }
        if (denyAllowListButton != null) {
            refreshModeButton();
        }
        layoutSelectionDots();
    }

    private void refreshModeButton() {
        SoundMufflerBlockEntity be = menu.getBlockEntityFromLevel(minecraft != null ? minecraft.level : null);
        boolean allowList = be != null && be.isAllowList();
        denyAllowListButton.setMessage(
                allowList ? Component.translatable("gui.iska_utils.sound_muffler.allow_list")
                        : Component.translatable("gui.iska_utils.sound_muffler.deny_list"));
        denyAllowListButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable(allowList
                        ? "gui.iska_utils.sound_muffler.mode.allow.tooltip"
                        : "gui.iska_utils.sound_muffler.mode.deny.tooltip")));
    }

    private void layoutSelectionDots() {
        for (int i = 0; i < VISIBLE_ENTRIES; i++) {
            int entryIndex = scrollOffset + i;
            Button dot = selectionDotButtons[i];
            if (entryIndex >= filteredSoundIds.size()) {
                dot.visible = false;
                continue;
            }
            int entryX = leftPos + ENTRIES_START_X;
            int entryY = topPos + LIST_ENTRIES_START_Y + i * (ENTRY_HEIGHT + ENTRY_SPACING);
            dot.setX(MachineGuiButtons.filterSelectionDotX(entryX, ENTRY_WIDTH));
            dot.setY(MachineGuiButtons.structureSelectionDotY(entryY, ENTRY_HEIGHT));
            dot.visible = true;
            boolean selected = selectedSoundIds.contains(filteredSoundIds.get(entryIndex));
            MachineGuiButtons.updateSelectionDot(dot, selected);
        }
    }

    private void onSelectionDotPressed(int visibleRow) {
        int entryIndex = scrollOffset + visibleRow;
        if (entryIndex < 0 || entryIndex >= filteredSoundIds.size()) {
            return;
        }
        String id = filteredSoundIds.get(entryIndex);
        if (selectedSoundIds.contains(id)) {
            selectedSoundIds.remove(id);
        } else {
            selectedSoundIds.add(id);
        }
        playButtonSound();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, GUI_WIDTH, GUI_HEIGHT);
        renderEntries(guiGraphics, mouseX, mouseY);
        renderScrollbar(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component titleComponent = Component.translatable("gui.iska_utils.sound_muffler.filter_title");
        int titleX = (imageWidth - font.width(titleComponent)) / 2;
        guiGraphics.drawString(font, titleComponent, titleX, 8, 0x404040, false);
    }

    private void renderEntries(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        for (int i = 0; i < VISIBLE_ENTRIES; i++) {
            int entryIndex = scrollOffset + i;
            int entryX = leftPos + ENTRIES_START_X;
            int entryY = topPos + LIST_ENTRIES_START_Y + i * (ENTRY_HEIGHT + ENTRY_SPACING);
            guiGraphics.blit(ENTRY_TEXTURE, entryX, entryY, 0, 0, ENTRY_WIDTH, ENTRY_HEIGHT, ENTRY_TEX_WIDTH, ENTRY_TEX_HEIGHT);
            if (entryIndex < filteredSoundIds.size()) {
                String soundId = filteredSoundIds.get(entryIndex);
                int maxW = ENTRY_WIDTH - 8 - MachineGuiButtons.DOT_SIZE - 6;
                String display = font.plainSubstrByWidth(soundId, maxW);
                if (display.length() < soundId.length()) display = display + "..";
                guiGraphics.drawString(font, display, entryX + 4, entryY + (ENTRY_HEIGHT - font.lineHeight) / 2, 0x404040, false);
            }
        }
    }

    private void renderScrollbar(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        updateScrollArrowState();
        GuiScroller.draw(
                guiGraphics,
                leftPos + SCROLLBAR_X,
                topPos + BUTTON_UP_Y,
                topPos + BUTTON_DOWN_Y,
                scrollOffset,
                Math.max(0, filteredSoundIds.size() - VISIBLE_ENTRIES));
    }

    private void scrollUp() {
        if (scrollOffset > 0) scrollOffset--;
    }

    private void scrollDown() {
        if (filteredSoundIds.size() > VISIBLE_ENTRIES && scrollOffset < filteredSoundIds.size() - VISIBLE_ENTRIES)
            scrollOffset++;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (MachineGuiInput.clearEditBoxOnRightClick(mouseX, mouseY, button, searchBox)) {
            return true;
        }
        if (button == 0 && handleScrollbarInteraction(mouseX, mouseY)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** Handle drag on thumb; track click jumps then continues as drag. */
    private boolean handleScrollbarInteraction(double mouseX, double mouseY) {
        if (filteredSoundIds.size() <= VISIBLE_ENTRIES) {
            return false;
        }
        int scrollbarX = leftPos + SCROLLBAR_X;
        int trackY = topPos + SCROLLBAR_Y;
        if (mouseX < scrollbarX || mouseX >= scrollbarX + SCROLLBAR_WIDTH
                || mouseY < trackY || mouseY >= trackY + SCROLLBAR_HEIGHT) {
            return false;
        }
        int maxOffset = filteredSoundIds.size() - VISIBLE_ENTRIES;
        float ratio = maxOffset > 0 ? (float) scrollOffset / maxOffset : 0f;
        int handleY = trackY + (int) (ratio * GuiScroller.handleRange(SCROLLBAR_HEIGHT));
        boolean onHandle = mouseY >= handleY && mouseY < handleY + SCROLLER_HEIGHT;
        if (!onHandle) {
            scrollOffset = GuiScroller.scrollOffsetFromTrackClick(
                    mouseY, trackY, SCROLLBAR_HEIGHT, maxOffset);
            playButtonSound();
        }
        isDraggingHandle = true;
        dragStartY = (int) mouseY;
        dragStartScrollOffset = scrollOffset;
        MachineGuiInput.markScrollbarPressed();
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            MachineGuiInput.clearScrollbarPressed();
            isDraggingHandle = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= leftPos + ENTRIES_START_X && mouseX < leftPos + ENTRIES_START_X + ENTRY_WIDTH + 20 &&
                mouseY >= topPos + LIST_ENTRIES_START_Y && mouseY < topPos + LIST_ENTRIES_START_Y + VISIBLE_ENTRIES * (ENTRY_HEIGHT + ENTRY_SPACING)) {
            if (scrollY > 0) scrollUp();
            else if (scrollY < 0) scrollDown();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (isDraggingHandle && filteredSoundIds.size() > VISIBLE_ENTRIES) {
            int deltaY = (int) mouseY - dragStartY;
            int maxOffset = filteredSoundIds.size() - VISIBLE_ENTRIES;
            int handleRange = GuiScroller.handleRange(SCROLLBAR_HEIGHT);
            if (handleRange > 0) {
                int deltaScroll = Math.round((float) deltaY / handleRange * maxOffset);
                scrollOffset = Math.max(0, Math.min(maxOffset, dragStartScrollOffset + deltaScroll));
            }
        }
        super.mouseMoved(mouseX, mouseY);
    }
}

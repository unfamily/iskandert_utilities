package net.unfamily.iskautils.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
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
    private static final ResourceLocation SCROLLBAR_TEXTURE = ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/scrollbar.png");
    private static final ResourceLocation TINY_BUTTONS_TEXTURE = ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/tiny_buttons.png");

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
    private static final int SCROLLBAR_WIDTH = 8;
    private static final int HANDLE_SIZE = 8;
    private static final int BUTTON_SIZE = 8;
    private static final int BUTTON_EMPTY_U = 8;
    private static final int BUTTON_FILLED_U = 16;
    private static final int BUTTON_NORMAL_V = 0;
    private static final int BUTTON_HOVERED_V = 8;
    private static final int SCROLLBAR_X = ENTRIES_START_X + ENTRY_WIDTH + GAP_ENTRY_SCROLLBAR;
    private static final int SEARCH_BAR_Y = ENTRIES_START_Y;
    private static final int SEARCH_BAR_HEIGHT = 20;
    private static final int LIST_ENTRIES_START_Y = ENTRIES_START_Y + SEARCH_BAR_HEIGHT + 2;
    private static final int BUTTON_UP_Y = LIST_ENTRIES_START_Y;
    private static final int SCROLLBAR_Y = BUTTON_UP_Y + HANDLE_SIZE;
    private static final int SCROLLBAR_HEIGHT = 34;
    private static final int BUTTON_DOWN_Y = SCROLLBAR_Y + SCROLLBAR_HEIGHT;
    // 8 entries fit in 180px height (52 + 8*12 + 8 + 20)
    private static final int VISIBLE_ENTRIES = 8;
    private static final int SAVE_CANCEL_Y = LIST_ENTRIES_START_Y + VISIBLE_ENTRIES * (ENTRY_HEIGHT + ENTRY_SPACING) + 8;
    private static final int SAVE_BUTTON_X = ENTRIES_START_X + 20;
    private static final int CANCEL_BUTTON_X = ENTRIES_START_X + ENTRY_WIDTH - 60;
    private static final int SAVE_CANCEL_BUTTON_WIDTH = 40;
    private static final int SAVE_CANCEL_BUTTON_HEIGHT = 20;
    private static final int CLOSE_BUTTON_Y = BORDER_MARGIN;
    private static final int CLOSE_BUTTON_SIZE = 12;
    private static final int CLOSE_BUTTON_X = GUI_WIDTH - CLOSE_BUTTON_SIZE - BORDER_MARGIN;

    private final List<String> allSoundIds = new ArrayList<>();
    private final List<String> filteredSoundIds = new ArrayList<>();
    private final Set<String> selectedSoundIds = new HashSet<>();
    private int scrollOffset = 0;
    private boolean isDraggingHandle = false;
    private int dragStartY = 0;
    private int dragStartScrollOffset = 0;
    private String lastSearchText = "";

    private EditBox searchBox;
    private Button saveButton;
    private Button cancelButton;
    private Button closeButton;

    public SoundMufflerFilterScreen(SoundMufflerFilterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
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
        int saveX = leftPos + SAVE_BUTTON_X;
        int cancelX = leftPos + CANCEL_BUTTON_X;
        int buttonY = topPos + SAVE_CANCEL_Y;
        saveButton = Button.builder(Component.translatable("gui.iska_utils.structure_placer.apply"), btn -> handleApply())
                .bounds(saveX, buttonY, SAVE_CANCEL_BUTTON_WIDTH, SAVE_CANCEL_BUTTON_HEIGHT)
                .build();
        cancelButton = Button.builder(Component.translatable("gui.iska_utils.structure_placer.cancel"), btn -> handleCancel())
                .bounds(cancelX, buttonY, SAVE_CANCEL_BUTTON_WIDTH, SAVE_CANCEL_BUTTON_HEIGHT)
                .build();
        addRenderableWidget(saveButton);
        addRenderableWidget(cancelButton);
        closeButton = Button.builder(Component.literal("✕"), btn -> handleCancel())
                .bounds(leftPos + CLOSE_BUTTON_X, topPos + CLOSE_BUTTON_Y, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE)
                .build();
        addRenderableWidget(closeButton);
    }

    /**
     * Prevents closing with inventory key (E) when search EditBox is focused (like Structure Saver).
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchBox != null && searchBox.isFocused()) {
            if (searchBox.keyPressed(keyCode, scanCode, modifiers)) return true;
            if (minecraft != null && minecraft.options.keyInventory.matches(keyCode, scanCode)) return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void playButtonSound() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    private void handleApply() {
        playButtonSound();
        BlockPos pos = menu.getBlockPos();
        if (pos.equals(BlockPos.ZERO)) return;
        ModMessages.sendSoundMufflerFilterUpdatePacket(pos, new ArrayList<>(selectedSoundIds));
        onClose();
    }

    private void handleCancel() {
        playButtonSound();
        onClose();
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
                int maxW = ENTRY_WIDTH - 8 - BUTTON_SIZE - 6;
                String display = font.plainSubstrByWidth(soundId, maxW);
                if (display.length() < soundId.length()) display = display + "..";
                guiGraphics.drawString(font, display, entryX + 4, entryY + (ENTRY_HEIGHT - font.lineHeight) / 2, 0x404040, false);
                renderSelectionButton(guiGraphics, entryX, entryY, entryIndex, mouseX, mouseY);
            }
        }
    }

    private void renderSelectionButton(GuiGraphics guiGraphics, int entryX, int entryY, int entryIndex, int mouseX, int mouseY) {
        int buttonX = entryX + ENTRY_WIDTH - BUTTON_SIZE - 4;
        int buttonY = entryY + (ENTRY_HEIGHT - BUTTON_SIZE) / 2;
        boolean isHovered = mouseX >= buttonX && mouseX < buttonX + BUTTON_SIZE && mouseY >= buttonY && mouseY < buttonY + BUTTON_SIZE;
        boolean isSelected = entryIndex < filteredSoundIds.size() && selectedSoundIds.contains(filteredSoundIds.get(entryIndex));
        int buttonU = isSelected ? BUTTON_FILLED_U : BUTTON_EMPTY_U;
        int buttonV = isHovered ? BUTTON_HOVERED_V : BUTTON_NORMAL_V;
        guiGraphics.blit(TINY_BUTTONS_TEXTURE, buttonX, buttonY, buttonU, buttonV, BUTTON_SIZE, BUTTON_SIZE, 64, 96);
    }

    private void renderScrollbar(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int scrollbarX = leftPos + SCROLLBAR_X;
        int scrollbarY = topPos + SCROLLBAR_Y;
        int buttonUpY = topPos + BUTTON_UP_Y;
        int buttonDownY = topPos + BUTTON_DOWN_Y;
        guiGraphics.blit(SCROLLBAR_TEXTURE, scrollbarX, scrollbarY, 0, 0, SCROLLBAR_WIDTH, SCROLLBAR_HEIGHT, 32, 34);
        boolean upHovered = mouseX >= scrollbarX && mouseX < scrollbarX + HANDLE_SIZE && mouseY >= buttonUpY && mouseY < buttonUpY + HANDLE_SIZE;
        guiGraphics.blit(SCROLLBAR_TEXTURE, scrollbarX, buttonUpY, SCROLLBAR_WIDTH * 2, upHovered ? HANDLE_SIZE : 0, HANDLE_SIZE, HANDLE_SIZE, 32, 34);
        boolean downHovered = mouseX >= scrollbarX && mouseX < scrollbarX + HANDLE_SIZE && mouseY >= buttonDownY && mouseY < buttonDownY + HANDLE_SIZE;
        guiGraphics.blit(SCROLLBAR_TEXTURE, scrollbarX, buttonDownY, SCROLLBAR_WIDTH * 3, downHovered ? HANDLE_SIZE : 0, HANDLE_SIZE, HANDLE_SIZE, 32, 34);
        int total = filteredSoundIds.size();
        float scrollRatio = total <= VISIBLE_ENTRIES ? 0 : (float) scrollOffset / (total - VISIBLE_ENTRIES);
        int handleY = scrollbarY + (int) (scrollRatio * (SCROLLBAR_HEIGHT - HANDLE_SIZE));
        boolean handleHovered = mouseX >= scrollbarX && mouseX < scrollbarX + HANDLE_SIZE && mouseY >= handleY && mouseY < handleY + HANDLE_SIZE;
        guiGraphics.blit(SCROLLBAR_TEXTURE, scrollbarX, handleY, SCROLLBAR_WIDTH, handleHovered ? HANDLE_SIZE : 0, HANDLE_SIZE, HANDLE_SIZE, 32, 34);
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
        if (button == 0) {
            int scrollbarX = leftPos + SCROLLBAR_X;
            if (filteredSoundIds.size() > VISIBLE_ENTRIES) {
                int upButtonY = topPos + BUTTON_UP_Y;
                if (mouseX >= scrollbarX && mouseX < scrollbarX + HANDLE_SIZE && mouseY >= upButtonY && mouseY < upButtonY + HANDLE_SIZE) {
                    scrollUp();
                    playButtonSound();
                    return true;
                }
                int downButtonY = topPos + BUTTON_DOWN_Y;
                if (mouseX >= scrollbarX && mouseX < scrollbarX + HANDLE_SIZE && mouseY >= downButtonY && mouseY < downButtonY + HANDLE_SIZE) {
                    scrollDown();
                    playButtonSound();
                    return true;
                }
            }
            for (int i = 0; i < VISIBLE_ENTRIES; i++) {
                int entryIndex = scrollOffset + i;
                if (entryIndex >= filteredSoundIds.size()) continue;
                int entryX = leftPos + ENTRIES_START_X;
                int entryY = topPos + LIST_ENTRIES_START_Y + i * (ENTRY_HEIGHT + ENTRY_SPACING);
                int buttonX = entryX + ENTRY_WIDTH - BUTTON_SIZE - 4;
                int buttonY = entryY + (ENTRY_HEIGHT - BUTTON_SIZE) / 2;
                if (mouseX >= buttonX && mouseX < buttonX + BUTTON_SIZE && mouseY >= buttonY && mouseY < buttonY + BUTTON_SIZE) {
                    String id = filteredSoundIds.get(entryIndex);
                    if (selectedSoundIds.contains(id)) selectedSoundIds.remove(id);
                    else selectedSoundIds.add(id);
                    playButtonSound();
                    return true;
                }
            }
            if (mouseX >= scrollbarX && mouseX < scrollbarX + HANDLE_SIZE && mouseY >= topPos + SCROLLBAR_Y && mouseY < topPos + BUTTON_DOWN_Y) {
                isDraggingHandle = true;
                dragStartY = (int) mouseY;
                dragStartScrollOffset = scrollOffset;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) isDraggingHandle = false;
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
            int handleRange = SCROLLBAR_HEIGHT - HANDLE_SIZE;
            if (handleRange > 0) {
                int deltaScroll = Math.round((float) deltaY / handleRange * maxOffset);
                scrollOffset = Math.max(0, Math.min(maxOffset, dragStartScrollOffset + deltaScroll));
            }
        }
        super.mouseMoved(mouseX, mouseY);
    }
}

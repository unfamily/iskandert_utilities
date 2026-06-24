package net.unfamily.iskautils.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.entity.MobReaperBlockEntity;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.network.ModMessages;
import net.unfamily.iskautils.util.MachineTargetType;

public class MobReaperScreen extends AbstractContainerScreen<MobReaperMenu> {

    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(
            IskaUtils.MOD_ID, "textures/gui/backgrounds/mob_reaper.png");
    private static final Identifier SINGLE_SLOT_TEXTURE = Identifier.fromNamespaceAndPath(
            IskaUtils.MOD_ID, "textures/gui/single_slot.png");
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 200;
    private static final int REDSTONE_BUTTON_SIZE = 16;
    private static final int RIGHT_BUTTON_MARGIN = 10;
    private static final int BUTTON_SPACING_Y = 4;
    private static final int CLOSE_BUTTON_SIZE = 12;
    private static final int CLOSE_BUTTON_X = GUI_WIDTH - CLOSE_BUTTON_SIZE - 5;
    private static final int CLOSE_BUTTON_Y = 5;
    private static final int SUBVIEW_BTN_HEIGHT = 16;
    private static final int SUBVIEW_BTN_SPACING = 4;

    private static final ItemStack GHOST_NORMAL = new ItemStack(ModItems.NORMAL_DAMAGE_MODULE.get());
    private static final ItemStack GHOST_LETHAL = new ItemStack(ModItems.LETHAL_DAMAGE_MODULE.get());
    private static final ItemStack GHOST_ENCHANT = new ItemStack(ModItems.ENCHANT_MODULE.get());
    private static final ItemStack GHOST_BEHEADING = new ItemStack(ModItems.BEHEADING_MODULE.get());
    private static final ItemStack GHOST_LUCK = new ItemStack(ModItems.LUCK_MODULE.get());
    private static final ItemStack GHOST_EXPERIENCE = new ItemStack(ModItems.EXPERIENCE_MODULE.get());

    private Button closeButton;
    private ItemIconButton redstoneModeButton;
    private ItemIconButton targetTypeButton;
    private ItemIconButton ageFilterButton;
    private long ghostCycleTime;
    private boolean showLethalGhost;

    public MobReaperScreen(MobReaperMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, GUI_WIDTH, GUI_HEIGHT);
        inventoryLabelY = 10000;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;

        closeButton = Button.builder(Component.literal("✕"), button -> {
            if (this.minecraft != null) {
                this.minecraft.player.closeContainer();
            }
        }).bounds(this.leftPos + CLOSE_BUTTON_X, this.topPos + CLOSE_BUTTON_Y, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE).build();
        addRenderableWidget(closeButton);

        int rightButtonX = this.leftPos + this.imageWidth - RIGHT_BUTTON_MARGIN - REDSTONE_BUTTON_SIZE;
        int centerY = this.topPos + 70;
        int redstoneY = centerY - REDSTONE_BUTTON_SIZE - BUTTON_SPACING_Y - REDSTONE_BUTTON_SIZE / 2;
        int targetY = centerY - REDSTONE_BUTTON_SIZE / 2;
        int ageFilterY = centerY + BUTTON_SPACING_Y + REDSTONE_BUTTON_SIZE / 2;

        redstoneModeButton = addRenderableWidget(MachineGuiButtons.redstoneIconButton(
                rightButtonX, redstoneY, b -> onRedstoneModePressed(false), menu::getRedstoneMode, false));
        targetTypeButton = addRenderableWidget(new ItemIconButton(
                rightButtonX, targetY, REDSTONE_BUTTON_SIZE,
                b -> onTargetTypePressed(false),
                () -> MachineGuiButtons.targetTypeIcon(menu.getTargetType()),
                Component.empty()));
        ageFilterButton = addRenderableWidget(new ItemIconButton(
                rightButtonX, ageFilterY, REDSTONE_BUTTON_SIZE,
                b -> onAgeFilterPressed(false),
                () -> MachineGuiButtons.mobAgeFilterIcon(menu.getAgeFilter()),
                Component.empty()));
    }

    @Override
    public void containerTick() {
        super.containerTick();
        long now = System.currentTimeMillis();
        if (now - ghostCycleTime > 1000L) {
            ghostCycleTime = now;
            showLethalGhost = !showLethalGhost;
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        renderGhostModules(guiGraphics);
        renderButtonTooltips(guiGraphics, mouseX, mouseY);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, this.leftPos, this.topPos, 0.0F, 0.0F,
                this.imageWidth, this.imageHeight, GUI_WIDTH, GUI_HEIGHT);

        renderModuleSlotBackgrounds(guiGraphics);
        renderStats(guiGraphics);
    }

    private void renderModuleSlotBackgrounds(GuiGraphicsExtractor guiGraphics) {
        for (int i = 0; i < 5; i++) {
            renderSlotBackground(guiGraphics, MobReaperMenu.MODULE_SLOTS_X, MobReaperMenu.MODULE_SLOT_Y[i]);
        }
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

    private void drawCenteredText(GuiGraphicsExtractor guiGraphics, Component text, int centerX, int y, int color) {
        int width = this.font.width(text);
        guiGraphics.text(this.font, text, centerX - width / 2, y, color, false);
    }

    private void renderStats(GuiGraphicsExtractor guiGraphics) {
        int centerX = this.leftPos + this.imageWidth / 2;
        int y = this.topPos + 28;
        Component damageLine = Component.translatable("gui.iska_utils.mob_reaper.damage", String.format("%.1f", menu.getEffectiveDamage()));
        drawCenteredText(guiGraphics, damageLine, centerX, y, GuiTextColors.TITLE);
        y += 12;

        if (menu.isLethalActive()) {
            drawCenteredText(guiGraphics,
                    Component.translatable("gui.iska_utils.mob_reaper.lethal_active"), centerX, y, 0xAA0000);
            y += 12;
        }
        if (menu.getBeheadingChance() > 0.0f) {
            drawCenteredText(guiGraphics,
                    Component.translatable("gui.iska_utils.mob_reaper.beheading", (int) (menu.getBeheadingChance() * 100)), centerX, y, 0x606060);
            y += 12;
        }
        if (menu.getLuckLevel() > 0) {
            drawCenteredText(guiGraphics,
                    Component.translatable("gui.iska_utils.mob_reaper.luck", menu.getLuckLevel()), centerX, y, 0x606060);
            y += 12;
        }
        if (menu.getExperienceMultiplier() > 1.0f) {
            drawCenteredText(guiGraphics,
                    Component.translatable("gui.iska_utils.mob_reaper.experience", String.format("%.1f", menu.getExperienceMultiplier())), centerX, y, 0x606060);
        }
    }

    private void renderButtonTooltips(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        if (redstoneModeButton != null && redstoneModeButton.isMouseOver(mouseX, mouseY)) {
            MachineGuiButtons.renderTooltipLine(
                    guiGraphics, font, mouseX, mouseY,
                    MachineGuiButtons.redstoneTooltip(menu.getRedstoneMode(), false));
        } else if (targetTypeButton != null && targetTypeButton.isMouseOver(mouseX, mouseY)) {
            MachineTargetType targetType = MachineTargetType.fromId(menu.getTargetType());
            MachineGuiButtons.renderTooltipLine(
                    guiGraphics, font, mouseX, mouseY,
                    Component.translatable("gui.iska_utils.mob_reaper.target_type." + targetType.getName()));
        } else if (ageFilterButton != null && ageFilterButton.isMouseOver(mouseX, mouseY)) {
            MobReaperBlockEntity.MobAgeFilter ageFilter = MobReaperBlockEntity.MobAgeFilter.fromId(menu.getAgeFilter());
            MachineGuiButtons.renderTooltipLine(
                    guiGraphics, font, mouseX, mouseY,
                    Component.translatable("gui.iska_utils.mob_reaper.age_filter." + ageFilter.getName()));
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        super.extractTooltip(guiGraphics, mouseX, mouseY);
        renderEmptyModuleSlotTooltips(guiGraphics, mouseX, mouseY);
    }

    private void renderEmptyModuleSlotTooltips(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        ItemStack[] ghosts = {
                showLethalGhost ? GHOST_LETHAL : GHOST_NORMAL,
                GHOST_ENCHANT,
                GHOST_BEHEADING,
                GHOST_LUCK,
                GHOST_EXPERIENCE
        };
        for (int i = 0; i < ghosts.length; i++) {
            Slot slot = menu.getSlot(i);
            if (!slot.getItem().isEmpty() || !isMouseOverSlot(slot, mouseX, mouseY)) {
                continue;
            }
            java.util.List<FormattedCharSequence> lines = java.util.List.of(ghosts[i].getHoverName().getVisualOrderText());
            guiGraphics.setTooltipForNextFrame(this.font, lines, DefaultTooltipPositioner.INSTANCE, mouseX, mouseY, true);
            return;
        }
    }

    private boolean isMouseOverSlot(Slot slot, int mouseX, int mouseY) {
        int x = this.leftPos + slot.x;
        int y = this.topPos + slot.y;
        return mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16;
    }

    private void renderGhostItem(GuiGraphicsExtractor guiGraphics, ItemStack itemStack, int slotX, int slotY) {
        GhostItemRenderer.render(guiGraphics, itemStack, this.leftPos + slotX, this.topPos + slotY, GuiGhostItem.DEFAULT_ARGB);
    }

    private void renderGhostModules(GuiGraphicsExtractor guiGraphics) {
        ItemStack[] ghosts = {
                showLethalGhost ? GHOST_LETHAL : GHOST_NORMAL,
                GHOST_ENCHANT,
                GHOST_BEHEADING,
                GHOST_LUCK,
                GHOST_EXPERIENCE
        };
        for (int i = 0; i < 5; i++) {
            Slot slot = menu.getSlot(i);
            if (slot.getItem().isEmpty()) {
                renderGhostItem(guiGraphics, ghosts[i], slot.x, slot.y);
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        if (event.button() == 1) {
            if (redstoneModeButton != null && redstoneModeButton.isMouseOver(mouseX, mouseY)) {
                onRedstoneModePressed(true);
                return true;
            }
            if (targetTypeButton != null && targetTypeButton.isMouseOver(mouseX, mouseY)) {
                onTargetTypePressed(true);
                return true;
            }
            if (ageFilterButton != null && ageFilterButton.isMouseOver(mouseX, mouseY)) {
                onAgeFilterPressed(true);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void onTargetTypePressed(boolean backward) {
        BlockPos pos = menu.getSyncedBlockPos();
        if (pos != null && !pos.equals(BlockPos.ZERO)) {
            ModMessages.sendMobReaperTargetTypePacket(pos, backward);
            playButtonSound();
        }
    }

    private void onAgeFilterPressed(boolean backward) {
        BlockPos pos = menu.getSyncedBlockPos();
        if (pos != null && !pos.equals(BlockPos.ZERO)) {
            ModMessages.sendMobReaperAgeFilterPacket(pos, backward);
            playButtonSound();
        }
    }

    private void onRedstoneModePressed(boolean backward) {
        BlockPos pos = menu.getSyncedBlockPos();
        if (pos != null && !pos.equals(BlockPos.ZERO)) {
            ModMessages.sendMobReaperRedstoneModePacket(pos, backward);
            playButtonSound();
        }
    }

    private void playButtonSound() {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        int titleWidth = this.font.width(this.title);
        int titleX = (this.imageWidth - titleWidth) / 2;
        guiGraphics.text(this.font, this.title, titleX, 8, GuiTextColors.TITLE, false);
    }
}

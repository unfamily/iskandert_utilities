package net.unfamily.iskautils.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.network.ModMessages;
import net.unfamily.iskautils.util.MachineTargetType;

public class MobReaperScreen extends AbstractContainerScreen<MobReaperMenu> {

    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(
            IskaUtils.MOD_ID, "textures/gui/backgrounds/mob_reaper.png");
    private static final ResourceLocation SINGLE_SLOT_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            IskaUtils.MOD_ID, "textures/gui/single_slot.png");
    private static final ResourceLocation MEDIUM_BUTTONS = ResourceLocation.fromNamespaceAndPath(
            IskaUtils.MOD_ID, "textures/gui/medium_buttons.png");
    private static final ResourceLocation REDSTONE_GUI = ResourceLocation.fromNamespaceAndPath(
            IskaUtils.MOD_ID, "textures/gui/redstone_gui.png");

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 200;
    private static final int TITLE_COLOR = 0x404040;
    private static final int REDSTONE_BUTTON_SIZE = 16;
    private static final int RIGHT_BUTTON_MARGIN = 10;
    private static final int BUTTON_SPACING_Y = 4;
    private static final int CLOSE_BUTTON_SIZE = 12;
    private static final int CLOSE_BUTTON_X = GUI_WIDTH - CLOSE_BUTTON_SIZE - 5;
    private static final int CLOSE_BUTTON_Y = 5;

    private static final ItemStack GHOST_NORMAL = new ItemStack(ModItems.NORMAL_DAMAGE_MODULE.get());
    private static final ItemStack GHOST_LETHAL = new ItemStack(ModItems.LETHAL_DAMAGE_MODULE.get());
    private static final ItemStack GHOST_ENCHANT = new ItemStack(ModItems.ENCHANT_MODULE.get());
    private static final ItemStack GHOST_BEHEADING = new ItemStack(ModItems.BEHEADING_MODULE.get());
    private static final ItemStack GHOST_LUCK = new ItemStack(ModItems.LUCK_MODULE.get());
    private static final ItemStack GHOST_EXPERIENCE = new ItemStack(ModItems.EXPERIENCE_MODULE.get());

    private Button closeButton;
    private int redstoneModeButtonX;
    private int redstoneModeButtonY;
    private int targetTypeButtonX;
    private int targetTypeButtonY;
    private long ghostCycleTime;
    private boolean showLethalGhost;

    public MobReaperScreen(MobReaperMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        this.inventoryLabelY = 10000;
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
        redstoneModeButtonY = centerY - REDSTONE_BUTTON_SIZE - BUTTON_SPACING_Y / 2;
        targetTypeButtonY = centerY + BUTTON_SPACING_Y / 2;
        redstoneModeButtonX = rightButtonX;
        targetTypeButtonX = rightButtonX;
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
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, GUI_WIDTH, GUI_HEIGHT);
        renderModuleSlotBackgrounds(guiGraphics);
        renderStats(guiGraphics);
        renderRedstoneModeButton(guiGraphics, mouseX, mouseY);
        renderTargetTypeButton(guiGraphics, mouseX, mouseY);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderGhostModules(guiGraphics);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        renderButtonTooltips(guiGraphics, mouseX, mouseY);
    }

    private void renderModuleSlotBackgrounds(GuiGraphics guiGraphics) {
        for (int i = 0; i < 5; i++) {
            guiGraphics.blit(SINGLE_SLOT_TEXTURE,
                    this.leftPos + MobReaperMenu.MODULE_SLOTS_X,
                    this.topPos + MobReaperMenu.MODULE_SLOT_Y[i],
                    0, 0, 18, 18, 18, 18);
        }
    }

    private void drawCenteredText(GuiGraphics guiGraphics, Component text, int centerX, int y, int color) {
        int width = this.font.width(text);
        guiGraphics.drawString(this.font, text, centerX - width / 2, y, color, false);
    }

    private void renderStats(GuiGraphics guiGraphics) {
        int centerX = this.leftPos + this.imageWidth / 2;
        int y = this.topPos + 28;
        Component damageLine = Component.translatable("gui.iska_utils.mob_reaper.damage", String.format("%.1f", menu.getEffectiveDamage()));
        drawCenteredText(guiGraphics, damageLine, centerX, y, TITLE_COLOR);
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

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderTooltip(guiGraphics, mouseX, mouseY);
        renderEmptyModuleSlotTooltips(guiGraphics, mouseX, mouseY);
    }

    private void renderEmptyModuleSlotTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
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
            guiGraphics.renderTooltip(this.font, ghosts[i].getHoverName(), mouseX, mouseY);
            return;
        }
    }

    private boolean isMouseOverSlot(Slot slot, int mouseX, int mouseY) {
        int x = this.leftPos + slot.x;
        int y = this.topPos + slot.y;
        return mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16;
    }

    private void renderButtonTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (mouseX >= redstoneModeButtonX && mouseX <= redstoneModeButtonX + REDSTONE_BUTTON_SIZE
                && mouseY >= redstoneModeButtonY && mouseY <= redstoneModeButtonY + REDSTONE_BUTTON_SIZE) {
            int redstoneMode = menu.getRedstoneMode();
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
            guiGraphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
        } else if (mouseX >= targetTypeButtonX && mouseX <= targetTypeButtonX + REDSTONE_BUTTON_SIZE
                && mouseY >= targetTypeButtonY && mouseY <= targetTypeButtonY + REDSTONE_BUTTON_SIZE) {
            MachineTargetType targetType = MachineTargetType.fromId(menu.getTargetType());
            Component tooltip = Component.translatable("gui.iska_utils.mob_reaper.target_type." + targetType.getName());
            guiGraphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }

    private void renderGhostItem(GuiGraphics guiGraphics, ItemStack itemStack, int slotX, int slotY) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(this.leftPos + slotX, this.topPos + slotY, 0);
        guiGraphics.renderItem(itemStack, 0, 0);
        guiGraphics.fill(0, 0, 16, 16, 0x80000000);
        guiGraphics.pose().popPose();
    }

    private void renderGhostModules(GuiGraphics guiGraphics) {
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

    private void renderRedstoneModeButton(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        boolean isHovered = mouseX >= redstoneModeButtonX && mouseX <= redstoneModeButtonX + REDSTONE_BUTTON_SIZE
                && mouseY >= redstoneModeButtonY && mouseY <= redstoneModeButtonY + REDSTONE_BUTTON_SIZE;
        int textureY = isHovered ? 16 : 0;
        guiGraphics.blit(MEDIUM_BUTTONS, redstoneModeButtonX, redstoneModeButtonY,
                0, textureY, REDSTONE_BUTTON_SIZE, REDSTONE_BUTTON_SIZE, 96, 96);

        int iconX = redstoneModeButtonX + 2;
        int iconY = redstoneModeButtonY + 2;
        int iconSize = 12;
        switch (menu.getRedstoneMode()) {
            case 0 -> renderScaledItem(guiGraphics, new ItemStack(net.minecraft.world.item.Items.GUNPOWDER), iconX, iconY, iconSize);
            case 1 -> renderScaledItem(guiGraphics, new ItemStack(net.minecraft.world.item.Items.REDSTONE), iconX, iconY, iconSize);
            case 2 -> renderScaledTexture(guiGraphics, REDSTONE_GUI, iconX, iconY, iconSize);
            case 4 -> renderScaledItem(guiGraphics, new ItemStack(net.minecraft.world.item.Items.BARRIER), iconX, iconY, iconSize);
            default -> {}
        }
    }

    private void renderTargetTypeButton(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        boolean isHovered = mouseX >= targetTypeButtonX && mouseX <= targetTypeButtonX + REDSTONE_BUTTON_SIZE
                && mouseY >= targetTypeButtonY && mouseY <= targetTypeButtonY + REDSTONE_BUTTON_SIZE;
        int textureY = isHovered ? 16 : 0;
        guiGraphics.blit(MEDIUM_BUTTONS, targetTypeButtonX, targetTypeButtonY,
                0, textureY, REDSTONE_BUTTON_SIZE, REDSTONE_BUTTON_SIZE, 96, 96);

        int iconX = targetTypeButtonX + 2;
        int iconY = targetTypeButtonY + 2;
        int iconSize = 12;
        MachineTargetType type = MachineTargetType.fromId(menu.getTargetType());
        switch (type) {
            case MOBS_ONLY -> renderScaledItem(guiGraphics, new ItemStack(net.minecraft.world.item.Items.CREEPER_HEAD), iconX, iconY, iconSize);
            case MOBS_AND_PLAYERS -> renderScaledItem(guiGraphics, new ItemStack(net.minecraft.world.item.Items.TNT), iconX, iconY, iconSize);
            case PLAYERS_ONLY -> renderScaledItem(guiGraphics, new ItemStack(net.minecraft.world.item.Items.PLAYER_HEAD), iconX, iconY, iconSize);
        }
    }

    private void renderScaledItem(GuiGraphics guiGraphics, ItemStack itemStack, int x, int y, int size) {
        guiGraphics.pose().pushPose();
        float scale = (float) size / 16.0f;
        guiGraphics.pose().translate(x, y, 0);
        guiGraphics.pose().scale(scale, scale, 1.0f);
        guiGraphics.renderItem(itemStack, 0, 0);
        guiGraphics.pose().popPose();
    }

    private void renderScaledTexture(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y, int size) {
        guiGraphics.pose().pushPose();
        float scale = (float) size / 16.0f;
        guiGraphics.pose().translate(x, y, 0);
        guiGraphics.pose().scale(scale, scale, 1.0f);
        guiGraphics.blit(texture, 0, 0, 0, 0, 16, 16, 16, 16);
        guiGraphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (handleMouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleMouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 || button == 1) {
            if (mouseX >= redstoneModeButtonX && mouseX <= redstoneModeButtonX + REDSTONE_BUTTON_SIZE
                    && mouseY >= redstoneModeButtonY && mouseY <= redstoneModeButtonY + REDSTONE_BUTTON_SIZE) {
                onRedstoneModePressed(button == 1);
                return true;
            }
            if (mouseX >= targetTypeButtonX && mouseX <= targetTypeButtonX + REDSTONE_BUTTON_SIZE
                    && mouseY >= targetTypeButtonY && mouseY <= targetTypeButtonY + REDSTONE_BUTTON_SIZE) {
                onTargetTypePressed(button == 1);
                return true;
            }
        }
        return false;
    }

    private void onRedstoneModePressed(boolean backward) {
        BlockPos pos = menu.getSyncedBlockPos();
        if (!pos.equals(BlockPos.ZERO)) {
            ModMessages.sendMobReaperRedstoneModePacket(pos, backward);
            playButtonSound();
        }
    }

    private void onTargetTypePressed(boolean backward) {
        BlockPos pos = menu.getSyncedBlockPos();
        if (!pos.equals(BlockPos.ZERO)) {
            ModMessages.sendMobReaperTargetTypePacket(pos, backward);
            playButtonSound();
        }
    }

    private void playButtonSound() {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int titleWidth = this.font.width(this.title);
        int titleX = (this.imageWidth - titleWidth) / 2;
        guiGraphics.drawString(this.font, this.title, titleX, 8, TITLE_COLOR, false);
    }
}

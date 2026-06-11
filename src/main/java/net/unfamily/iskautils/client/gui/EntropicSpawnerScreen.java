package net.unfamily.iskautils.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.entity.EntropicSpawnerBlockEntity;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.network.ModMessages;
import org.jetbrains.annotations.Nullable;

/**
 * Entropic Spawner GUI — single Fan-style screen (176×200).
 */
public class EntropicSpawnerScreen extends AbstractContainerScreen<EntropicSpawnerMenu> {
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(
            IskaUtils.MOD_ID, "textures/gui/backgrounds/entropic_spawner.png");
    private static final ResourceLocation SINGLE_SLOT_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            IskaUtils.MOD_ID, "textures/gui/single_slot.png");

    private static final int GUI_WIDTH = EntropicSpawnerMenu.GUI_WIDTH;
    private static final int GUI_HEIGHT = EntropicSpawnerMenu.GUI_HEIGHT;
    private static final int TITLE_COLOR = 0x404040;
    private static final int GRID_START_X = (GUI_WIDTH - 50) / 2;
    private static final int GRID_START_Y = 30;
    private static final int REDSTONE_BUTTON_SIZE = 16;
    private static final int RIGHT_BUTTON_MARGIN = 10;
    private static final int CLOSE_BUTTON_SIZE = 12;
    private static final int CLOSE_BUTTON_X = GUI_WIDTH - CLOSE_BUTTON_SIZE - 5;
    private static final int CLOSE_BUTTON_Y = 5;

    private static final ItemStack GHOST_CLOCK = new ItemStack(ModItems.ENTROPIC_CLOCK.get());
    private static final ItemStack GHOST_PRODUCTION = new ItemStack(ModItems.PRODUCTION_MODULE.get());
    private static final ItemStack GHOST_FUEL = new ItemStack(ModItems.DROP_OF_ENTROPY.get());

    private Button closeButton;
    private ItemIconButton redstoneModeButton;

    public EntropicSpawnerScreen(EntropicSpawnerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;

        closeButton = Button.builder(Component.literal("✕"), button -> {
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.closeContainer();
            }
        }).bounds(leftPos + CLOSE_BUTTON_X, topPos + CLOSE_BUTTON_Y, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE).build();
        addRenderableWidget(closeButton);

        int rightButtonX = leftPos + imageWidth - RIGHT_BUTTON_MARGIN - REDSTONE_BUTTON_SIZE;
        int gridCenterY = topPos + GRID_START_Y + 25;
        int redstoneY = gridCenterY - REDSTONE_BUTTON_SIZE / 2;

        redstoneModeButton = addRenderableWidget(MachineGuiButtons.redstoneIconButton(
                rightButtonX, redstoneY, b -> onRedstoneModePressed(false), menu::getRedstoneMode, true));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, GUI_WIDTH, GUI_HEIGHT);
        graphics.blit(SINGLE_SLOT_TEXTURE,
                leftPos + EntropicSpawnerMenu.FUEL_SLOT_X,
                topPos + EntropicSpawnerMenu.FUEL_SLOT_Y,
                0, 0, 18, 18, 18, 18);
        renderCenterInfo(graphics);
        renderPlaceholderOverlay(graphics);
    }

    private void renderPlaceholderOverlay(GuiGraphics graphics) {
        Slot slot = menu.getSlot(EntropicSpawnerBlockEntity.PLACEHOLDER_SLOT_INDEX);
        int x = leftPos + slot.x;
        int y = topPos + slot.y;
        graphics.fill(x, y, x + 16, y + 16, 0xAA000000);
    }

    private void renderCenterInfo(GuiGraphics graphics) {
        int centerX = leftPos + GRID_START_X + 25;
        int y = topPos + GRID_START_Y + 52;
        var type = menu.getSyncedEntityType();
        Component mobLine = type == null
                ? Component.translatable("gui.iska_utils.entropic_spawner.spawn_mob.empty")
                : Component.translatable("gui.iska_utils.entropic_spawner.spawn_mob", type.getDescription());
        drawCenteredText(graphics, mobLine, centerX, y);
        y += 12;
        if (menu.isLifetimeSpawnCapReached()) {
            drawCenteredText(graphics,
                    Component.translatable("gui.iska_utils.entropic_spawner.lifetime_cap_reached",
                            menu.getLifetimeSpawnCount(), menu.getLifetimeSpawnMax()),
                    centerX, y);
        } else {
            int seconds = (menu.getSpawnDelayTicks() + 19) / 20;
            drawCenteredText(graphics,
                    Component.translatable("gui.iska_utils.entropic_spawner.spawn_in", seconds),
                    centerX, y);
        }
    }

    private void drawCenteredText(GuiGraphics graphics, Component text, int centerX, int y) {
        int width = font.width(text);
        graphics.drawString(font, text, centerX - width / 2, y, TITLE_COLOR, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderGhostItems(graphics);
        renderTooltip(graphics, mouseX, mouseY);
        renderButtonTooltips(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderTooltip(graphics, mouseX, mouseY);
        renderEmptySlotTooltips(graphics, mouseX, mouseY);
    }

    private void renderEmptySlotTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        ItemStack[] ghosts = {GHOST_CLOCK, GHOST_PRODUCTION, GHOST_FUEL};
        int[] indices = {
                EntropicSpawnerBlockEntity.CLOCK_SLOT_INDEX,
                EntropicSpawnerBlockEntity.PRODUCTION_SLOT_INDEX,
                EntropicSpawnerBlockEntity.FUEL_SLOT_INDEX
        };
        for (int i = 0; i < indices.length; i++) {
            Slot slot = menu.getSlot(indices[i]);
            if (slot.getItem().isEmpty() && isMouseOverSlot(slot, mouseX, mouseY)) {
                graphics.renderTooltip(font, ghosts[i].getHoverName(), mouseX, mouseY);
                return;
            }
        }
    }

    private boolean isMouseOverSlot(Slot slot, int mouseX, int mouseY) {
        int x = leftPos + slot.x;
        int y = topPos + slot.y;
        return mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16;
    }

    private void renderButtonTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        if (redstoneModeButton.isHovered()) {
            graphics.renderTooltip(font,
                    MachineGuiButtons.redstoneTooltip(menu.getRedstoneMode(), true),
                    mouseX, mouseY);
        }
    }

    private void renderGhostItem(GuiGraphics graphics, ItemStack stack, int slotX, int slotY) {
        GhostItemRenderer.render(graphics, stack, leftPos + slotX, topPos + slotY, GuiGhostItem.DEFAULT_ARGB);
    }

    private void renderGhostItems(GuiGraphics graphics) {
        Slot clockSlot = menu.getSlot(EntropicSpawnerBlockEntity.CLOCK_SLOT_INDEX);
        if (clockSlot.getItem().isEmpty()) {
            renderGhostItem(graphics, GHOST_CLOCK, clockSlot.x, clockSlot.y);
        }
        Slot prodSlot = menu.getSlot(EntropicSpawnerBlockEntity.PRODUCTION_SLOT_INDEX);
        if (prodSlot.getItem().isEmpty()) {
            renderGhostItem(graphics, GHOST_PRODUCTION, prodSlot.x, prodSlot.y);
        }
        Slot fuelSlot = menu.getSlot(EntropicSpawnerBlockEntity.FUEL_SLOT_INDEX);
        if (fuelSlot.getItem().isEmpty()) {
            renderGhostItem(graphics, GHOST_FUEL, fuelSlot.x, fuelSlot.y);
        }
    }

    @Override
    protected void renderSlotContents(GuiGraphics graphics, ItemStack stack, Slot slot, @Nullable String count) {
        if (slot.index == EntropicSpawnerBlockEntity.PLACEHOLDER_SLOT_INDEX) {
            return;
        }
        super.renderSlotContents(graphics, stack, slot, count);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        int titleX = (imageWidth - font.width(title)) / 2;
        graphics.drawString(font, title, titleX, 8, TITLE_COLOR, false);

        int max = menu.getFuelChargesMax();
        int cur = menu.getFuelCharges();
        if (max > 0) {
            int pct = (int) Math.floor(100.0 * cur / max);
            String text = pct + "%";
            int tx = EntropicSpawnerMenu.FUEL_SLOT_X + 9 - font.width(text) / 2;
            graphics.drawString(font, text, tx, EntropicSpawnerMenu.FUEL_SLOT_Y + 20, TITLE_COLOR, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1 && redstoneModeButton.isHovered()) {
            onRedstoneModePressed(true);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void onRedstoneModePressed(boolean backward) {
        ModMessages.sendEntropicSpawnerRedstoneMode(menu.getSyncedBlockPos(), backward);
        playButtonSound();
    }

    private void playButtonSound() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(
                    net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                            net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }
}

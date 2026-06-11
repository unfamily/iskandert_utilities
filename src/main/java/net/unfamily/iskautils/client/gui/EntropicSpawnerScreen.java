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
import net.unfamily.iskautils.block.entity.EntropicSpawnerBlockEntity;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.network.ModMessages;
import org.jspecify.annotations.Nullable;

/**
 * Entropic Spawner GUI — single Fan-style screen (176×200).
 */
public class EntropicSpawnerScreen extends AbstractContainerScreen<EntropicSpawnerMenu> {
    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(
            IskaUtils.MOD_ID, "textures/gui/backgrounds/entropic_spawner.png");
    private static final Identifier SINGLE_SLOT_TEXTURE = Identifier.fromNamespaceAndPath(
            IskaUtils.MOD_ID, "textures/gui/single_slot.png");

    private static final int GUI_WIDTH = EntropicSpawnerMenu.GUI_WIDTH;
    private static final int GUI_HEIGHT = EntropicSpawnerMenu.GUI_HEIGHT;
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
        super(menu, playerInventory, title, GUI_WIDTH, GUI_HEIGHT);
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
        int gridCenterY = this.topPos + GRID_START_Y + 25;
        int redstoneY = gridCenterY - REDSTONE_BUTTON_SIZE / 2;

        redstoneModeButton = addRenderableWidget(MachineGuiButtons.redstoneIconButton(
                rightButtonX, redstoneY, b -> onRedstoneModePressed(false), menu::getRedstoneMode, true));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, this.leftPos, this.topPos, 0.0F, 0.0F,
                this.imageWidth, this.imageHeight, GUI_WIDTH, GUI_HEIGHT);
        graphics.blit(RenderPipelines.GUI_TEXTURED, SINGLE_SLOT_TEXTURE,
                this.leftPos + EntropicSpawnerMenu.FUEL_SLOT_X,
                this.topPos + EntropicSpawnerMenu.FUEL_SLOT_Y,
                0.0F, 0.0F, 18, 18, 18, 18);
        renderCenterInfo(graphics);
        renderPlaceholderOverlay(graphics);
    }

    private void renderPlaceholderOverlay(GuiGraphicsExtractor graphics) {
        Slot slot = menu.getSlot(EntropicSpawnerBlockEntity.PLACEHOLDER_SLOT_INDEX);
        int x = this.leftPos + slot.x;
        int y = this.topPos + slot.y;
        graphics.fill(x, y, x + 16, y + 16, 0xAA000000);
    }

    private void renderCenterInfo(GuiGraphicsExtractor graphics) {
        int centerX = this.leftPos + GRID_START_X + 25;
        int y = this.topPos + GRID_START_Y + 52;
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

    private void drawCenteredText(GuiGraphicsExtractor graphics, Component text, int centerX, int y) {
        int width = this.font.width(text);
        graphics.text(this.font, text, centerX - width / 2, y, GuiTextColors.TITLE, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        renderGhostItems(graphics);
        renderButtonTooltips(graphics, mouseX, mouseY);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        renderEmptySlotTooltips(graphics, mouseX, mouseY);
    }

    private void renderEmptySlotTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        ItemStack[] ghosts = {GHOST_CLOCK, GHOST_PRODUCTION, GHOST_FUEL};
        int[] indices = {
                EntropicSpawnerBlockEntity.CLOCK_SLOT_INDEX,
                EntropicSpawnerBlockEntity.PRODUCTION_SLOT_INDEX,
                EntropicSpawnerBlockEntity.FUEL_SLOT_INDEX
        };
        for (int i = 0; i < indices.length; i++) {
            Slot slot = menu.getSlot(indices[i]);
            if (!slot.getItem().isEmpty() || !isMouseOverSlot(slot, mouseX, mouseY)) {
                continue;
            }
            java.util.List<FormattedCharSequence> lines = java.util.List.of(ghosts[i].getHoverName().getVisualOrderText());
            graphics.setTooltipForNextFrame(this.font, lines, DefaultTooltipPositioner.INSTANCE, mouseX, mouseY, true);
            return;
        }
    }

    private boolean isMouseOverSlot(Slot slot, int mouseX, int mouseY) {
        int x = this.leftPos + slot.x;
        int y = this.topPos + slot.y;
        return mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16;
    }

    private void renderButtonTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (redstoneModeButton != null && redstoneModeButton.isMouseOver(mouseX, mouseY)) {
            MachineGuiButtons.renderTooltipLine(
                    graphics, this.font, mouseX, mouseY,
                    MachineGuiButtons.redstoneTooltip(menu.getRedstoneMode(), true));
        }
    }

    private void renderGhostItem(GuiGraphicsExtractor graphics, ItemStack stack, int slotX, int slotY) {
        GhostItemRenderer.render(graphics, stack, this.leftPos + slotX, this.topPos + slotY, GuiGhostItem.DEFAULT_ARGB);
    }

    private void renderGhostItems(GuiGraphicsExtractor graphics) {
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
    protected void renderSlotContents(GuiGraphicsExtractor graphics, ItemStack stack, Slot slot, @Nullable String count) {
        if (slot.index == EntropicSpawnerBlockEntity.PLACEHOLDER_SLOT_INDEX) {
            return;
        }
        super.renderSlotContents(graphics, stack, slot, count);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int titleWidth = this.font.width(this.title);
        int titleX = (this.imageWidth - titleWidth) / 2;
        graphics.text(this.font, this.title, titleX, 8, GuiTextColors.TITLE, false);

        int max = menu.getFuelChargesMax();
        int cur = menu.getFuelCharges();
        if (max > 0) {
            int pct = (int) Math.floor(100.0 * cur / max);
            String text = pct + "%";
            int tx = EntropicSpawnerMenu.FUEL_SLOT_X + 9 - this.font.width(text) / 2;
            graphics.text(this.font, text, tx, EntropicSpawnerMenu.FUEL_SLOT_Y + 20, GuiTextColors.TITLE, false);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        if (event.button() == 1 && redstoneModeButton != null && redstoneModeButton.isMouseOver(mouseX, mouseY)) {
            onRedstoneModePressed(true);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void onRedstoneModePressed(boolean backward) {
        BlockPos pos = menu.getSyncedBlockPos();
        if (pos != null && !pos.equals(BlockPos.ZERO)) {
            ModMessages.sendEntropicSpawnerRedstoneMode(pos, backward);
            playButtonSound();
        }
    }

    private void playButtonSound() {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
        }
    }
}

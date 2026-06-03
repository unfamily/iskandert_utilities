package net.unfamily.iskautils.client.gui;

import net.minecraft.core.component.DataComponents;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.BlazingAltarSpawnMode;
import net.unfamily.iskautils.client.FlameVisibilityClient;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.network.packet.BlazingAltarConfigC2SPacket;
import net.unfamily.iskautils.network.packet.FlameVisionToggleC2SPacket;

import java.util.List;

public class BlazingAltarScreen extends AbstractContainerScreen<BlazingAltarMenu> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/backgrounds/blazing_altar.png");
    private static final Identifier REDSTONE_GUI =
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/redstone_gui.png");
    private static final Identifier SINGLE_SLOT_TEXTURE =
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/single_slot.png");

    private static final int GUI_WIDTH = BlazingAltarMenu.GUI_WIDTH;
    private static final int GUI_HEIGHT = BlazingAltarMenu.GUI_HEIGHT;
    private static final int TEXTURE_WIDTH = BlazingAltarMenu.TEXTURE_WIDTH;
    private static final int TEXTURE_HEIGHT = BlazingAltarMenu.TEXTURE_HEIGHT;

    private static final int BTN = 16;
    private static final int CLOSE_BUTTON_SIZE = 12;
    private static final int CLOSE_BUTTON_X = GUI_WIDTH - CLOSE_BUTTON_SIZE - 5;
    private static final int CLOSE_BUTTON_Y = 5;

    private static final int ROW_SLOT_Y = BlazingAltarMenu.PLACER_Y;
    private static final int ROW_BTN_GAP = 2;
    private static final int GROUND_BTN_X = 8;
    private static final int VISION_BTN_X = GUI_WIDTH - 8 - BTN;
    /** Same X columns as ground / vision; on the slot row, 1px above previous side-btn Y. */
    private static final int REDSTONE_BTN_X = GROUND_BTN_X;
    private static final int SPAWN_BTN_X = VISION_BTN_X;
    private static final int EXTINGUISH_BTN_X = SPAWN_BTN_X - BTN - ROW_BTN_GAP;
    private static final int SLOT_SIDE_BTN_Y = ROW_SLOT_Y - 1;

    /** Last button row: 2px gap above player inventory (inv starts at {@link BlazingAltarMenu#PLAYER_INV_Y}). */
    private static final int CHUNK_ROW_Y = BlazingAltarMenu.PLAYER_INV_Y - BTN - 2;
    /** Shared line above the chunk/range button row (illumination or removal progress). */
    private static final int CHUNK_PROGRESS_Y = CHUNK_ROW_Y - 9;
    private static final int CHUNK_RANGE_BTN_X = GROUND_BTN_X + BTN + ROW_BTN_GAP;
    private static final int CHUNK_RANGE_BTN_WIDTH = VISION_BTN_X - CHUNK_RANGE_BTN_X - ROW_BTN_GAP;

    private static final String TOOLTIP_RIGHT_CLICK_BACK = "gui.iska_utils.blazing_altar.tooltip.right_click_back";

    private static final ItemStack GHOST_RANGE_MODULE = new ItemStack(ModItems.RANGE_MODULE.get());

    private final GuiCycleTimer placerGhostCycle = new GuiCycleTimer(() -> 1000);
    private final List<ItemStack> placerGhostStacks = List.of(
            new ItemStack(ModItems.BURNING_BRAZIER.get()),
            new ItemStack(ModItems.CURSED_CANDLE.get()));

    private ItemIconButton spawnModeButton;
    private ItemIconButton extinguishButton;
    private Button chunkRangeButton;
    private ItemIconButton groundButton;
    private ItemIconButton visionButton;
    private ItemIconButton redstoneButton;
    private Button closeButton;

    public BlazingAltarScreen(BlazingAltarMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, GUI_WIDTH, GUI_HEIGHT);
        inventoryLabelY = 10000;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;

        closeButton = Button.builder(Component.literal("\u2715"), b -> {
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.closeContainer();
            }
        }).bounds(leftPos + CLOSE_BUTTON_X, topPos + CLOSE_BUTTON_Y, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE).build();
        addRenderableWidget(closeButton);

        spawnModeButton = addRenderableWidget(new ItemIconButton(
                leftPos + SPAWN_BTN_X,
                topPos + SLOT_SIDE_BTN_Y,
                BTN,
                b -> sendConfig(0),
                this::spawnModeIcon,
                Component.empty()));

        extinguishButton = addRenderableWidget(new ItemIconButton(
                leftPos + EXTINGUISH_BTN_X,
                topPos + SLOT_SIDE_BTN_Y,
                BTN,
                b -> sendConfig(8),
                this::extinguishIcon,
                Component.empty()));

        chunkRangeButton = addRenderableWidget(Button.builder(Component.empty(), b -> {})
                .bounds(leftPos + CHUNK_RANGE_BTN_X, topPos + CHUNK_ROW_Y, CHUNK_RANGE_BTN_WIDTH, BTN)
                .build());

        groundButton = addRenderableWidget(new ItemIconButton(
                leftPos + GROUND_BTN_X,
                topPos + CHUNK_ROW_Y,
                BTN,
                b -> sendConfig(3),
                this::groundIcon,
                Component.empty()));

        visionButton = addRenderableWidget(new ItemIconButton(
                leftPos + VISION_BTN_X,
                topPos + CHUNK_ROW_Y,
                BTN,
                b -> toggleFlameVision(),
                this::visionIcon,
                Component.empty()));

        redstoneButton = addRenderableWidget(new ItemIconButton(
                leftPos + REDSTONE_BTN_X,
                topPos + SLOT_SIDE_BTN_Y,
                BTN,
                b -> sendConfig(5),
                this::redstoneIcon,
                () -> menu.getRedstoneMode() == 2 ? REDSTONE_GUI : null,
                Component.empty()));
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (chunkRangeButton != null) {
            chunkRangeButton.setMessage(chunkRangeButtonLabel());
        }
    }

    private Component chunkRangeButtonLabel() {
        return Component.translatable(
                "gui.iska_utils.blazing_altar.chunk_radius.label",
                menu.getChunkRadius(),
                menu.getMaxChunkRadius());
    }

    private boolean isShiftDownNow() {
        if (minecraft == null) {
            return false;
        }
        var window = minecraft.getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        if (chunkRangeButton != null && chunkRangeButton.isMouseOver(mouseX, mouseY)) {
            boolean shift = isShiftDownNow();
            if (event.button() == 0) {
                sendConfig(shift ? 9 : 1);
                return true;
            }
            if (event.button() == 1) {
                sendConfig(shift ? 10 : 2);
                return true;
            }
        }
        if (event.button() == 1) {
            if (spawnModeButton != null && spawnModeButton.isMouseOver(mouseX, mouseY)) {
                sendConfig(7);
                return true;
            }
            if (groundButton != null && groundButton.isMouseOver(mouseX, mouseY)) {
                sendConfig(3);
                return true;
            }
            if (redstoneButton != null && redstoneButton.isMouseOver(mouseX, mouseY)) {
                sendConfig(6);
                return true;
            }
            if (visionButton != null && visionButton.isMouseOver(mouseX, mouseY)) {
                toggleFlameVision();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void toggleFlameVision() {
        FlameVisibilityClient.toggleGlobalFlameVision();
        ClientPacketDistributor.sendToServer(
                new FlameVisionToggleC2SPacket(FlameVisibilityClient.isGlobalFlameVisionEnabled()));
    }

    private void sendConfig(int action) {
        BlockPos pos = menu.getSyncedBlockPos();
        if (pos.equals(BlockPos.ZERO)) {
            return;
        }
        ClientPacketDistributor.sendToServer(new BlazingAltarConfigC2SPacket(pos, action, false));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        renderButtonTooltips(graphics, mouseX, mouseY);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                leftPos,
                topPos,
                0.0F,
                0.0F,
                imageWidth,
                imageHeight,
                TEXTURE_WIDTH,
                TEXTURE_HEIGHT);
        renderPlacerGhost(graphics);
        renderModuleSlotBackground(graphics);
        renderModuleGhost(graphics);
    }

    private void renderModuleSlotBackground(GuiGraphicsExtractor graphics) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                SINGLE_SLOT_TEXTURE,
                leftPos + BlazingAltarMenu.MODULE_SLOT_FRAME_X,
                topPos + BlazingAltarMenu.MODULE_SLOT_FRAME_Y,
                0.0F,
                0.0F,
                18,
                18,
                18,
                18);
    }

    private void renderPlacerGhost(GuiGraphicsExtractor graphics) {
        Slot slot = menu.getSlot(BlazingAltarMenu.PLACER_SLOT_INDEX);
        GuiGhostItem.renderCycling(graphics, leftPos, topPos, slot, placerGhostStacks, placerGhostCycle, GuiGhostItem.DEFAULT_ARGB);
    }

    private void renderModuleGhost(GuiGraphicsExtractor graphics) {
        GuiGhostItem.render(graphics, leftPos, topPos, menu.getSlot(BlazingAltarMenu.MODULE_SLOT_INDEX), GHOST_RANGE_MODULE);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Component titleText = Component.translatable("gui.iska_utils.blazing_altar.title");
        int titleWidth = font.width(titleText);
        graphics.text(font, titleText, (imageWidth - titleWidth) / 2, 8, GuiTextColors.TITLE, false);
        renderChunkProgressLabels(graphics);
    }

    private void renderChunkProgressLabels(GuiGraphicsExtractor graphics) {
        if (menu.isExtinguishing() && menu.getExtinguishChunkTotal() > 0) {
            drawCenteredProgressLine(graphics, Component.translatable(
                    "gui.iska_utils.blazing_altar.extinguish_chunks_progress",
                    menu.getExtinguishChunkProgress(),
                    menu.getExtinguishChunkTotal()));
            return;
        }
        if (!menu.isExtinguishing()
                && menu.getRedstoneMode() != 4
                && menu.getPlacementChunkTotal() > 0) {
            int current = Math.min(menu.getPlacementChunkProgress() + 1, menu.getPlacementChunkTotal());
            drawCenteredProgressLine(graphics, Component.translatable(
                    "gui.iska_utils.blazing_altar.placement_chunks_progress",
                    current,
                    menu.getPlacementChunkTotal()));
        }
    }

    private void drawCenteredProgressLine(GuiGraphicsExtractor graphics, Component text) {
        drawCenteredProgressLine(graphics, text, CHUNK_PROGRESS_Y);
    }

    private void drawCenteredProgressLine(GuiGraphicsExtractor graphics, Component text, int y) {
        int width = font.width(text);
        graphics.text(font, text, (imageWidth - width) / 2, y, GuiTextColors.TITLE, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (tryPlacerGhostTooltip(graphics, mouseX, mouseY)) {
            return;
        }
        if (tryModuleGhostTooltip(graphics, mouseX, mouseY)) {
            return;
        }
        super.extractTooltip(graphics, mouseX, mouseY);
    }

    private boolean tryPlacerGhostTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Slot placerSlot = menu.getSlot(BlazingAltarMenu.PLACER_SLOT_INDEX);
        if (!placerSlot.getItem().isEmpty() || !isMouseOverSlot(placerSlot, mouseX, mouseY)) {
            return false;
        }
        ItemStack ghost = placerGhostCycle.getOrDefault(placerGhostStacks, placerGhostStacks.getFirst());
        graphics.setTooltipForNextFrame(
                font,
                getTooltipFromContainerItem(ghost),
                ghost.getTooltipImage(),
                ghost,
                mouseX,
                mouseY,
                ghost.get(DataComponents.TOOLTIP_STYLE));
        return true;
    }

    private boolean tryModuleGhostTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Slot moduleSlot = menu.getSlot(BlazingAltarMenu.MODULE_SLOT_INDEX);
        if (!moduleSlot.getItem().isEmpty() || !isMouseOverSlot(moduleSlot, mouseX, mouseY)) {
            return false;
        }
        graphics.setTooltipForNextFrame(
                font,
                getTooltipFromContainerItem(GHOST_RANGE_MODULE),
                GHOST_RANGE_MODULE.getTooltipImage(),
                GHOST_RANGE_MODULE,
                mouseX,
                mouseY,
                GHOST_RANGE_MODULE.get(DataComponents.TOOLTIP_STYLE));
        return true;
    }

    private void renderButtonTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (chunkRangeButton != null && chunkRangeButton.isMouseOver(mouseX, mouseY)) {
            renderChunkRangeTooltip(graphics, mouseX, mouseY);
        } else if (spawnModeButton != null && spawnModeButton.isMouseOver(mouseX, mouseY)) {
            renderButtonTooltip(graphics, mouseX, mouseY, spawnModeStateTooltip(), true);
        } else if (extinguishButton != null && extinguishButton.isMouseOver(mouseX, mouseY)) {
            renderButtonTooltip(graphics, mouseX, mouseY, extinguishStateTooltip(), false);
        } else if (groundButton != null && groundButton.isMouseOver(mouseX, mouseY)) {
            renderButtonTooltip(graphics, mouseX, mouseY, groundStateTooltip(), true);
        } else if (visionButton != null && visionButton.isMouseOver(mouseX, mouseY)) {
            renderButtonTooltip(graphics, mouseX, mouseY, flameVisionStateTooltip(), true);
        } else if (redstoneButton != null && redstoneButton.isMouseOver(mouseX, mouseY)) {
            renderButtonTooltip(graphics, mouseX, mouseY, redstoneStateLine(), true);
        }
    }

    private void renderChunkRangeTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        List<FormattedCharSequence> lines = List.of(
                chunkRangeButtonLabel().getVisualOrderText(),
                Component.translatable("gui.iska_utils.blazing_altar.chunk_radius.controls").getVisualOrderText());
        graphics.setTooltipForNextFrame(
                font,
                lines,
                DefaultTooltipPositioner.INSTANCE,
                mouseX,
                mouseY,
                true);
    }

    private void renderButtonTooltip(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            Component primary,
            boolean withBack) {
        List<FormattedCharSequence> lines = withBack
                ? List.of(
                primary.getVisualOrderText(),
                Component.translatable(TOOLTIP_RIGHT_CLICK_BACK).getVisualOrderText())
                : List.of(primary.getVisualOrderText());
        graphics.setTooltipForNextFrame(
                font,
                lines,
                DefaultTooltipPositioner.INSTANCE,
                mouseX,
                mouseY,
                true);
    }

    private boolean isMouseOverSlot(Slot slot, int mouseX, int mouseY) {
        int x = leftPos + slot.x;
        int y = topPos + slot.y;
        return mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16;
    }

    private ItemStack extinguishIcon() {
        return menu.isExtinguishing()
                ? new ItemStack(Items.BARRIER)
                : new ItemStack(ModItems.BURNING_FLAME.get());
    }

    private Component extinguishStateTooltip() {
        return menu.isExtinguishing()
                ? Component.translatable("gui.iska_utils.blazing_altar.extinguish.cancel")
                : Component.translatable("gui.iska_utils.blazing_altar.extinguish");
    }

    private ItemStack spawnModeIcon() {
        return switch (BlazingAltarSpawnMode.fromId(menu.getSpawnModeId())) {
            case HOSTILE -> new ItemStack(Items.ZOMBIE_HEAD);
            case BOTH -> new ItemStack(Items.IRON_SWORD);
            case PASSIVE -> new ItemStack(Items.WHEAT);
        };
    }

    private MutableComponent spawnModeStateTooltip() {
        return Component.translatable(
                "gui.iska_utils.blazing_altar.spawn_mode."
                        + BlazingAltarSpawnMode.fromId(menu.getSpawnModeId()).name().toLowerCase());
    }

    private ItemStack groundIcon() {
        return menu.isGroundOnly() ? new ItemStack(Items.GRASS_BLOCK) : new ItemStack(Items.FEATHER);
    }

    private MutableComponent groundStateTooltip() {
        return Component.translatable(
                menu.isGroundOnly()
                        ? "gui.iska_utils.blazing_altar.ground_only.on"
                        : "gui.iska_utils.blazing_altar.ground_only.off");
    }

    private ItemStack visionIcon() {
        return FlameVisibilityClient.isGlobalFlameVisionEnabled()
                ? new ItemStack(Items.ENDER_EYE)
                : new ItemStack(Items.ENDER_PEARL);
    }

    private MutableComponent flameVisionStateTooltip() {
        return Component.translatable(
                FlameVisibilityClient.isGlobalFlameVisionEnabled()
                        ? "gui.iska_utils.blazing_altar.tooltip.flame_vision.on"
                        : "gui.iska_utils.blazing_altar.tooltip.flame_vision.off");
    }

    private ItemStack redstoneIcon() {
        return switch (menu.getRedstoneMode()) {
            case 0 -> new ItemStack(Items.GUNPOWDER);
            case 1 -> new ItemStack(Items.REDSTONE);
            case 2 -> ItemStack.EMPTY;
            case 4 -> new ItemStack(Items.BARRIER);
            default -> new ItemStack(Items.REDSTONE);
        };
    }

    private MutableComponent redstoneStateLine() {
        int redstoneMode = menu.getRedstoneMode();
        if (redstoneMode == 3) {
            redstoneMode = 4;
        }
        return Component.translatable(switch (redstoneMode) {
            case 0 -> "gui.iska_utils.generic.redstone_mode.none";
            case 1 -> "gui.iska_utils.generic.redstone_mode.low";
            case 2 -> "gui.iska_utils.generic.redstone_mode.high";
            case 4 -> "gui.iska_utils.generic.redstone_mode.disabled";
            default -> "gui.iska_utils.generic.redstone_mode.none";
        });
    }
}

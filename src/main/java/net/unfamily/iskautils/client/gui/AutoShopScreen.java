package net.unfamily.iskautils.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.entity.AutoShopBlockEntity;
import net.unfamily.iskautils.integration.jei.ghost.IIskaUtilsGhostTarget;
import net.unfamily.iskautils.shop.ShopCurrency;
import net.unfamily.iskautils.shop.ShopLoader;

public class AutoShopScreen extends AbstractContainerScreen<AutoShopMenu>
        implements IIskaUtilsGhostTarget {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/backgrounds/auto_shop.png");
    private static final int GUI_WIDTH = 200;
    private static final int GUI_HEIGHT = 160;
    private static final int BUTTON_SIZE = 16;

    private static final int CLOSE_BUTTON_Y = 5;
    private static final int CLOSE_BUTTON_SIZE = 12;
    private static final int CLOSE_BUTTON_X = GUI_WIDTH - CLOSE_BUTTON_SIZE - 5;

    private static final int REDSTONE_BUTTON_X = 20;
    private static final int REDSTONE_BUTTON_Y = 23;
    private static final int CURRENCY_BUTTON_X = 38;
    private static final int CURRENCY_BUTTON_Y = 23;
    private static final int MODE_BUTTON_X = 38;
    private static final int MODE_BUTTON_Y = 48;

    private Button closeButton;
    private ItemIconButton redstoneModeButton;
    private SymbolIconButton currencyButton;
    private SymbolIconButton modeButton;

    public AutoShopScreen(AutoShopMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, GUI_WIDTH, GUI_HEIGHT);
    }

    @Override
    protected void init() {
        super.init();
        closeButton = Button.builder(Component.literal("✕"),
                        button -> {
                            playButtonSound();
                            this.onClose();
                        })
                .bounds(this.leftPos + CLOSE_BUTTON_X, this.topPos + CLOSE_BUTTON_Y,
                        CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE)
                .build();
        addRenderableWidget(closeButton);

        redstoneModeButton = addRenderableWidget(MachineGuiButtons.redstoneIconButton(
                this.leftPos + REDSTONE_BUTTON_X,
                this.topPos + REDSTONE_BUTTON_Y,
                b -> onRedstoneModePressed(false),
                menu::getRedstoneMode,
                true));

        currencyButton = addRenderableWidget(new SymbolIconButton(
                this.leftPos + CURRENCY_BUTTON_X,
                this.topPos + CURRENCY_BUTTON_Y,
                BUTTON_SIZE,
                b -> onCurrencyPressed(false),
                this::getCurrencySymbol,
                getCurrencyTooltip()));

        modeButton = addRenderableWidget(new SymbolIconButton(
                this.leftPos + MODE_BUTTON_X,
                this.topPos + MODE_BUTTON_Y,
                BUTTON_SIZE,
                b -> onModePressed(false),
                this::getModeLetter,
                Component.empty()));

        updateModeButtonTooltip();
    }

    private void updateModeButtonTooltip() {
        if (modeButton != null) {
            modeButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(getModeTooltip()));
        }
    }

    private String getModeLetter() {
        return menu.isAutoBuyMode()
                ? Component.translatable("gui.iska_utils.auto_shop.mode.letter.buy").getString()
                : Component.translatable("gui.iska_utils.auto_shop.mode.letter.sell").getString();
    }

    private Component getModeTooltip() {
        return menu.isAutoBuyMode()
                ? Component.translatable("gui.iska_utils.auto_shop.tooltip.buy")
                : Component.translatable("gui.iska_utils.auto_shop.tooltip.sell");
    }

    private String getCurrencySymbol() {
        ShopCurrency currency = ShopLoader.getCurrencies().get(menu.getSelectedCurrencyId());
        if (currency != null && currency.charSymbol != null && !currency.charSymbol.isEmpty()) {
            return currency.charSymbol;
        }
        return menu.getSelectedCurrencyId();
    }

    private Component getCurrencyTooltip() {
        ShopCurrency currency = ShopLoader.getCurrencies().get(menu.getSelectedCurrencyId());
        if (currency != null && currency.name != null) {
            return Component.translatable(currency.name);
        }
        return Component.literal(menu.getSelectedCurrencyId());
    }

    private void playButtonSound() {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(
                    net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                            net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0F, 0.0F, this.imageWidth, this.imageHeight, GUI_WIDTH, GUI_HEIGHT);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        Component title = Component.translatable("block.iska_utils.auto_shop");
        int titleWidth = this.font.width(title);
        guiGraphics.text(this.font, title, (this.imageWidth - titleWidth) / 2, 8, GuiTextColors.TITLE, false);

        Component selectText = Component.translatable("gui.iska_utils.auto_shop.select_item");
        guiGraphics.text(this.font, selectText, 75, 27, GuiTextColors.TITLE, false);

        Component encapsulatedText = Component.translatable("gui.iska_utils.auto_shop.encapsulated_item");
        guiGraphics.text(this.font, encapsulatedText, 75, 52, GuiTextColors.TITLE, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        updateModeButtonTooltip();
        renderFilterGhost(guiGraphics);
        renderFilterGhostTooltip(guiGraphics, mouseX, mouseY);
        if (redstoneModeButton != null && redstoneModeButton.isMouseOver(mouseX, mouseY)) {
            MachineGuiButtons.renderTooltipLine(
                    guiGraphics, font, mouseX, mouseY,
                    MachineGuiButtons.redstoneTooltip(menu.getRedstoneMode(), true));
        }
    }

    private void renderFilterGhost(GuiGraphicsExtractor guiGraphics) {
        GuiGhostItem.render(guiGraphics, leftPos, topPos, menu.getSlot(AutoShopMenu.FILTER_SLOT_INDEX), getFilterGhostItem());
    }

    private ItemStack getFilterGhostItem() {
        BlockPos pos = menu.getSyncedBlockPos();
        if (minecraft == null || minecraft.level == null || pos.equals(BlockPos.ZERO)) {
            return ItemStack.EMPTY;
        }
        if (minecraft.level.getBlockEntity(pos) instanceof AutoShopBlockEntity autoShop) {
            return autoShop.getSelectedItem();
        }
        return ItemStack.EMPTY;
    }

    private void renderFilterGhostTooltip(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        Slot filterSlot = menu.getSlot(AutoShopMenu.FILTER_SLOT_INDEX);
        ItemStack ghost = getFilterGhostItem();
        if (filterSlot == null || !filterSlot.getItem().isEmpty() || ghost.isEmpty()) {
            return;
        }
        if (!isMouseOverSlot(filterSlot, mouseX, mouseY)) {
            return;
        }
        guiGraphics.setTooltipForNextFrame(
                font,
                java.util.List.of(ghost.getHoverName().getVisualOrderText()),
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

    @Override
    public IGhostIngredientConsumer getGhostHandler() {
        return new IGhostItemConsumer() {
            @Override
            public void accept(Object ingredient) {
                if (ingredient instanceof ItemStack stack) {
                    acceptJeiFilterItem(stack);
                }
            }
        };
    }

    @Override
    public Rect2i getGhostTargetArea() {
        Slot filterSlot = menu.getSlot(AutoShopMenu.FILTER_SLOT_INDEX);
        if (filterSlot == null) {
            return null;
        }
        return new Rect2i(leftPos + filterSlot.x, topPos + filterSlot.y, 18, 18);
    }

    private void acceptJeiFilterItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        BlockPos machinePos = resolveMachinePos();
        if (!machinePos.equals(BlockPos.ZERO)) {
            ItemStack copy = stack.copy();
            copy.setCount(1);
            net.unfamily.iskautils.network.ModMessages.sendAutoShopSelectedItemPacket(machinePos, copy);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 1) {
            if (redstoneModeButton != null && redstoneModeButton.isMouseOver(event.x(), event.y())) {
                onRedstoneModePressed(true);
                return true;
            }
            if (currencyButton != null && currencyButton.isMouseOver(event.x(), event.y())) {
                onCurrencyPressed(true);
                return true;
            }
            if (modeButton != null && modeButton.isMouseOver(event.x(), event.y())) {
                onModePressed(true);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private BlockPos resolveMachinePos() {
        BlockPos machinePos = menu.getSyncedBlockPos();
        if (!machinePos.equals(BlockPos.ZERO)) {
            return machinePos;
        }
        if (minecraft != null && minecraft.level != null) {
            BlockPos playerPos = minecraft.player != null ? minecraft.player.blockPosition() : BlockPos.ZERO;
            if (!playerPos.equals(BlockPos.ZERO)) {
                for (int dx = -8; dx <= 8; dx++) {
                    for (int dy = -8; dy <= 8; dy++) {
                        for (int dz = -8; dz <= 8; dz++) {
                            BlockPos candidate = playerPos.offset(dx, dy, dz);
                            if (minecraft.level.getBlockEntity(candidate) instanceof AutoShopBlockEntity) {
                                return candidate;
                            }
                        }
                    }
                }
            }
        }
        return BlockPos.ZERO;
    }

    private void onRedstoneModePressed(boolean backward) {
        playButtonSound();
        BlockPos machinePos = resolveMachinePos();
        if (!machinePos.equals(BlockPos.ZERO)) {
            net.unfamily.iskautils.network.ModMessages.sendAutoShopRedstoneModePacket(machinePos, backward);
        }
    }

    private void onCurrencyPressed(boolean backward) {
        playButtonSound();
        BlockPos machinePos = resolveMachinePos();
        if (!machinePos.equals(BlockPos.ZERO)) {
            net.unfamily.iskautils.network.ModMessages.sendAutoShopCycleCurrencyPacket(machinePos, backward);
        }
    }

    private void onModePressed(boolean backward) {
        playButtonSound();
        BlockPos machinePos = resolveMachinePos();
        if (!machinePos.equals(BlockPos.ZERO)) {
            net.unfamily.iskautils.network.ModMessages.sendAutoShopSetModePacket(
                    machinePos, !menu.isAutoBuyMode(), backward);
        }
    }
}

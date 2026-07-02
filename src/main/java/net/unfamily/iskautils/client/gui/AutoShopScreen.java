package net.unfamily.iskautils.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
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
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/backgrounds/auto_shop.png");
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
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
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
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight, GUI_WIDTH, GUI_HEIGHT);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component title = Component.translatable("block.iska_utils.auto_shop");
        int titleWidth = this.font.width(title);
        guiGraphics.drawString(this.font, title, (this.imageWidth - titleWidth) / 2, 8, 0x404040, false);

        Component selectText = Component.translatable("gui.iska_utils.auto_shop.select_item");
        guiGraphics.drawString(this.font, selectText, 75, 27, 0x404040, false);

        Component encapsulatedText = Component.translatable("gui.iska_utils.auto_shop.encapsulated_item");
        guiGraphics.drawString(this.font, encapsulatedText, 75, 52, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        updateModeButtonTooltip();
        renderFilterGhost(guiGraphics);
        renderFilterGhostTooltip(guiGraphics, mouseX, mouseY);
        if (redstoneModeButton.isHovered()) {
            guiGraphics.renderTooltip(font,
                    MachineGuiButtons.redstoneTooltip(menu.getRedstoneMode(), true), mouseX, mouseY);
        }
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private void renderFilterGhost(GuiGraphics guiGraphics) {
        GuiGhostItem.render(guiGraphics, leftPos, topPos, menu.getSlot(AutoShopMenu.FILTER_SLOT_INDEX), getFilterGhostItem());
    }

    private void renderFilterGhostTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Slot filterSlot = menu.getSlot(AutoShopMenu.FILTER_SLOT_INDEX);
        ItemStack ghost = getFilterGhostItem();
        if (filterSlot == null || !filterSlot.getItem().isEmpty() || ghost.isEmpty()) {
            return;
        }
        if (!isMouseOverSlot(filterSlot, mouseX, mouseY)) {
            return;
        }
        guiGraphics.renderTooltip(
                font,
                java.util.List.of(ghost.getHoverName().getVisualOrderText()),
                mouseX,
                mouseY);
    }

    private boolean isMouseOverSlot(Slot slot, int mouseX, int mouseY) {
        int x = leftPos + slot.x;
        int y = topPos + slot.y;
        return mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16;
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
        BlockPos machinePos = menu.getSyncedBlockPos();
        if (!machinePos.equals(BlockPos.ZERO)) {
            ItemStack copy = stack.copy();
            copy.setCount(1);
            net.unfamily.iskautils.network.ModMessages.sendAutoShopSelectedItemPacket(machinePos, copy);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1) {
            if (redstoneModeButton.isHovered()) {
                onRedstoneModePressed(true);
                return true;
            }
            if (currencyButton.isHovered()) {
                onCurrencyPressed(true);
                return true;
            }
            if (modeButton.isHovered()) {
                onModePressed(true);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void onRedstoneModePressed(boolean backward) {
        playButtonSound();
        BlockPos machinePos = menu.getSyncedBlockPos();
        if (!machinePos.equals(BlockPos.ZERO)) {
            net.unfamily.iskautils.network.ModMessages.sendAutoShopRedstoneModePacket(machinePos, backward);
        }
    }

    private void onCurrencyPressed(boolean backward) {
        playButtonSound();
        BlockPos machinePos = menu.getSyncedBlockPos();
        if (!machinePos.equals(BlockPos.ZERO)) {
            net.unfamily.iskautils.network.ModMessages.sendAutoShopCycleCurrencyPacket(machinePos, backward);
        }
    }

    private void onModePressed(boolean backward) {
        playButtonSound();
        BlockPos machinePos = menu.getSyncedBlockPos();
        if (!machinePos.equals(BlockPos.ZERO)) {
            net.unfamily.iskautils.network.ModMessages.sendAutoShopSetModePacket(
                    machinePos, !menu.isAutoBuyMode(), backward);
        }
    }
}

package net.unfamily.iskautils.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.entity.AutoShopBlockEntity;
import net.unfamily.iskautils.integration.jei.ghost.IIskaUtilsGhostTarget;
import net.unfamily.iskautils.integration.mekanism.MekChemicalHelper;
import net.unfamily.iskautils.shop.ShopCurrency;
import net.unfamily.iskautils.shop.ShopEntry;
import net.unfamily.iskautils.shop.ShopEntryHelper;
import net.unfamily.iskautils.shop.ShopLoader;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class AutoShopScreen extends AbstractContainerScreen<AutoShopMenu>
        implements IIskaUtilsGhostTarget {

    private enum SubView { MAIN, ITEM_PICKER }

    private static final Identifier MAIN_TEXTURE =
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/backgrounds/auto_shop.png");
    private static final int GUI_WIDTH = AutoShopGuiLayout.GUI_WIDTH;
    private static final int GUI_HEIGHT = AutoShopGuiLayout.GUI_HEIGHT;
    private static final int PICKER_WIDTH = ShopBrowsePanel.GUI_WIDTH;
    private static final int PICKER_HEIGHT = 240;
    private static final int BUTTON_SIZE = 16;

    private static final int CLOSE_BUTTON_Y = 5;
    private static final int CLOSE_BUTTON_SIZE = 12;
    private static final int CLOSE_BUTTON_X = GUI_WIDTH - CLOSE_BUTTON_SIZE - 5;

    private static final int CURRENCY_BUTTON_X = 2;
    private static final int CURRENCY_BUTTON_Y = 23;
    private static final int BUCKET_BUTTON_X = 20;
    private static final int BUCKET_BUTTON_Y = 23;
    private static final int SELECT_BUTTON_X = 38;
    private static final int SELECT_BUTTON_Y = 23;
    private static final int REDSTONE_BUTTON_X = 20;
    private static final int REDSTONE_BUTTON_Y = 48;
    private static final int MODE_BUTTON_X = 38;
    private static final int MODE_BUTTON_Y = 48;

    private SubView subView = SubView.MAIN;
    private final AutoShopItemPickerOverlay itemPicker;

    private Button closeButton;
    private ItemIconButton redstoneModeButton;
    private SymbolIconButton currencyButton;
    private SymbolIconButton convertBucketButton;
    private SymbolIconButton selectCatalogButton;
    private SymbolIconButton modeButton;
    private Button liquidDumpButton;
    private Button gasDumpButton;

    public AutoShopScreen(AutoShopMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, PICKER_WIDTH, PICKER_HEIGHT);
        this.inventoryLabelY = 73;
        itemPicker = new AutoShopItemPickerOverlay(
                this::onClose,
                this::leaveItemPicker,
                this::resolveMachinePos,
                () -> this.leftPos,
                () -> this.topPos,
                this::playButtonSound,
                this::rebuildPickerSelectButtons,
                () -> this.font);
    }

    private void rebuildPickerSelectButtons() {
        if (subView == SubView.ITEM_PICKER) {
            itemPicker.rebuildSelectButtons(this);
        }
    }

    <T extends net.minecraft.client.gui.components.events.GuiEventListener & net.minecraft.client.gui.components.Renderable & net.minecraft.client.gui.narration.NarratableEntry> T addPickerWidget(T widget) {
        return addRenderableWidget(widget);
    }

    void removePickerWidget(net.minecraft.client.gui.components.events.GuiEventListener widget) {
        removeWidget(widget);
    }

    private int layoutWidth() {
        return subView == SubView.MAIN ? GUI_WIDTH : PICKER_WIDTH;
    }

    private int layoutHeight() {
        return subView == SubView.MAIN ? GUI_HEIGHT : PICKER_HEIGHT;
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        repositionLayout();
        super.extractContents(graphics, mouseX, mouseY, partialTick);
    }

    private void repositionLayout() {
        int layoutWidth = layoutWidth();
        int layoutHeight = layoutHeight();
        this.leftPos = (this.width - layoutWidth) / 2;
        this.topPos = (this.height - layoutHeight) / 2;
    }

    private void enterItemPicker() {
        subView = SubView.ITEM_PICKER;
        this.inventoryLabelY = 10000;
        repositionLayout();
        itemPicker.loadData();
        this.clearWidgets();
        itemPicker.initWidgets(this);
    }

    private void leaveItemPicker() {
        subView = SubView.MAIN;
        this.inventoryLabelY = 73;
        repositionLayout();
        this.clearWidgets();
        init();
    }

    @Override
    protected void init() {
        super.init();
        repositionLayout();
        if (subView == SubView.ITEM_PICKER) {
            itemPicker.initWidgets(this);
            return;
        }

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

        convertBucketButton = addRenderableWidget(new SymbolIconButton(
                this.leftPos + BUCKET_BUTTON_X,
                this.topPos + BUCKET_BUTTON_Y,
                BUTTON_SIZE,
                b -> onConvertBucketPressed(),
                () -> "🪣",
                Component.translatable("gui.iska_utils.auto_shop.convert_bucket.tooltip")));

        selectCatalogButton = addRenderableWidget(new SymbolIconButton(
                this.leftPos + SELECT_BUTTON_X,
                this.topPos + SELECT_BUTTON_Y,
                BUTTON_SIZE,
                b -> {
                    playButtonSound();
                    enterItemPicker();
                },
                () -> "≡",
                Component.translatable("gui.iska_utils.auto_shop.select_item_catalog")));

        modeButton = addRenderableWidget(new SymbolIconButton(
                this.leftPos + MODE_BUTTON_X,
                this.topPos + MODE_BUTTON_Y,
                BUTTON_SIZE,
                b -> onModePressed(false),
                this::getModeLetter,
                Component.empty()));

        liquidDumpButton = addRenderableWidget(Button.builder(Component.literal("D"),
                        b -> dumpTank(false))
                .bounds(leftPos + AutoShopGuiLayout.LIQUID_BAR_X - 1,
                        topPos + AutoShopGuiLayout.DUMP_Y,
                        AutoShopGuiLayout.DUMP_W, AutoShopGuiLayout.DUMP_H)
                .build());
        gasDumpButton = addRenderableWidget(Button.builder(Component.literal("D"),
                        b -> dumpTank(true))
                .bounds(leftPos + AutoShopGuiLayout.GAS_BAR_X - 1,
                        topPos + AutoShopGuiLayout.DUMP_Y,
                        AutoShopGuiLayout.DUMP_W, AutoShopGuiLayout.DUMP_H)
                .build());
        gasDumpButton.visible = MekChemicalHelper.isLoaded();

        updateModeButtonTooltip();
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (subView == SubView.ITEM_PICKER) {
            itemPicker.tick();
        }
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
        if (subView == SubView.ITEM_PICKER) {
            itemPicker.renderBackground(guiGraphics, mouseX, mouseY);
            renderGuiLabels(guiGraphics);
            return;
        }
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, MAIN_TEXTURE,
                leftPos, topPos, 0.0F, 0.0F,
                GUI_WIDTH, GUI_HEIGHT, GUI_WIDTH, GUI_HEIGHT);
        renderTanks(guiGraphics);
        renderGuiLabels(guiGraphics);
    }

    private void renderTanks(GuiGraphicsExtractor graphics) {
        FluidRenderHelper.renderTank(
                graphics,
                leftPos + AutoShopGuiLayout.LIQUID_BAR_X,
                topPos + AutoShopGuiLayout.BAR_Y,
                AutoShopGuiLayout.BAR_W,
                AutoShopGuiLayout.BAR_H,
                menu.getFluidRegistryId(),
                menu.getFluidAmount(),
                menu.getFluidCapacity());

        int gasX = leftPos + AutoShopGuiLayout.GAS_BAR_X;
        int gasY = topPos + AutoShopGuiLayout.BAR_Y;
        if (!MekChemicalHelper.isLoaded()) {
            graphics.fill(gasX, gasY,
                    gasX + AutoShopGuiLayout.BAR_W,
                    gasY + AutoShopGuiLayout.BAR_H,
                    AutoShopGuiLayout.MASK_COLOR);
        } else {
            FluidRenderHelper.renderChemicalTank(
                    graphics,
                    gasX,
                    gasY,
                    AutoShopGuiLayout.BAR_W,
                    AutoShopGuiLayout.BAR_H,
                    menu.getGasId(),
                    menu.getGasAmount(),
                    menu.getGasCapacity());
        }
    }

    /** Absolute screen coordinates; extractLabels runs under an extra GUI translate. */
    private void renderGuiLabels(GuiGraphicsExtractor guiGraphics) {
        if (subView == SubView.ITEM_PICKER) {
            Component title = Component.translatable("gui.iska_utils.auto_shop.picker.title");
            int titleWidth = this.font.width(title);
            guiGraphics.text(this.font, title, leftPos + (PICKER_WIDTH - titleWidth) / 2, topPos + 8,
                    GuiTextColors.TITLE, false);
            return;
        }

        Component title = Component.translatable("block.iska_utils.auto_shop");
        int titleWidth = this.font.width(title);
        guiGraphics.text(this.font, title, leftPos + (GUI_WIDTH - titleWidth) / 2, topPos + 8,
                GuiTextColors.TITLE, false);

        Component selectText = Component.translatable("gui.iska_utils.auto_shop.select_item");
        guiGraphics.text(this.font, selectText, leftPos + 75, topPos + 27, GuiTextColors.TITLE, false);

        Component encapsulatedText = Component.translatable("gui.iska_utils.auto_shop.encapsulated_item");
        guiGraphics.text(this.font, encapsulatedText, leftPos + 75, topPos + 52, GuiTextColors.TITLE, false);
    }

    @Override
    protected void extractSlots(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        if (subView == SubView.ITEM_PICKER) {
            return;
        }
        super.extractSlots(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        // Static labels are drawn in extractBackground (absolute coords; dual layout sizes).
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        if (subView == SubView.ITEM_PICKER && itemPicker.extractTooltips(guiGraphics, mouseX, mouseY, this::getTooltipFromContainerItem)) {
            return;
        }
        super.extractTooltip(guiGraphics, mouseX, mouseY);
        if (subView == SubView.MAIN) {
            renderTankTooltip(guiGraphics, mouseX, mouseY);
        }
    }

    private void renderTankTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int barY = topPos + AutoShopGuiLayout.BAR_Y;
        if (isInside(mouseX, mouseY, leftPos + AutoShopGuiLayout.LIQUID_BAR_X, barY,
                AutoShopGuiLayout.BAR_W, AutoShopGuiLayout.BAR_H)) {
            if (menu.getFluidAmount() <= 0 || menu.getFluidRegistryId() < 0) {
                graphics.setTooltipForNextFrame(font, List.of(
                        Component.translatable("gui.iska_utils.auto_shop.tank.empty").getVisualOrderText()
                ), mouseX, mouseY);
                return;
            }
            var fluid = BuiltInRegistries.FLUID.byId(menu.getFluidRegistryId());
            if (fluid == null || fluid == Fluids.EMPTY) {
                graphics.setTooltipForNextFrame(font, List.of(
                        Component.translatable("gui.iska_utils.auto_shop.tank.empty").getVisualOrderText()
                ), mouseX, mouseY);
                return;
            }
            FluidStack stack = new FluidStack(fluid, (int) Math.min(Integer.MAX_VALUE, menu.getFluidAmount()));
            graphics.setTooltipForNextFrame(font, List.of(
                    stack.getHoverName().getVisualOrderText(),
                    Component.literal(menu.getFluidAmount() + " mB").getVisualOrderText()
            ), mouseX, mouseY);
        } else if (MekChemicalHelper.isLoaded()
                && isInside(mouseX, mouseY, leftPos + AutoShopGuiLayout.GAS_BAR_X, barY,
                AutoShopGuiLayout.BAR_W, AutoShopGuiLayout.BAR_H)) {
            if (menu.getGasAmount() <= 0 || menu.getGasId().isEmpty()) {
                graphics.setTooltipForNextFrame(font, List.of(
                        Component.translatable("gui.iska_utils.auto_shop.tank.empty").getVisualOrderText()
                ), mouseX, mouseY);
                return;
            }
            Object chemical = MekChemicalHelper.createStackFromId(menu.getGasId(), Math.max(1L, menu.getGasAmount()));
            Component name = MekChemicalHelper.getDisplayName(chemical);
            if (name.getString().isEmpty()) {
                name = Component.literal(menu.getGasId());
            }
            graphics.setTooltipForNextFrame(font, List.of(
                    name.getVisualOrderText(),
                    Component.literal(menu.getGasAmount() + " mB").getVisualOrderText()
            ), mouseX, mouseY);
        }
    }

    private static boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (subView == SubView.ITEM_PICKER) {
            return;
        }

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
        Slot filterSlot = menu.getSlot(AutoShopMenu.FILTER_SLOT_INDEX);
        if (filterSlot == null || !filterSlot.getItem().isEmpty()) {
            return;
        }
        ShopEntry bound = getBoundShopEntry();
        if (bound != null && bound.type == ShopEntry.EntryType.FLUID) {
            var fluid = ShopEntryHelper.displayFluidForEntry(bound);
            if (!fluid.isEmpty()) {
                GuiFluidStillBlit.blit16(guiGraphics, fluid, leftPos + filterSlot.x, topPos + filterSlot.y);
            }
            return;
        }
        if (bound != null && bound.type == ShopEntry.EntryType.GAS) {
            Object gas = ShopEntryHelper.displayGasForEntry(bound);
            if (gas != null) {
                GuiChemicalStillBlit.blit16(guiGraphics, gas, leftPos + filterSlot.x, topPos + filterSlot.y);
            }
            return;
        }
        GuiGhostItem.render(guiGraphics, leftPos, topPos, filterSlot, getFilterGhostItem());
    }

    @Nullable
    private ShopEntry getBoundShopEntry() {
        BlockPos pos = menu.getSyncedBlockPos();
        if (minecraft == null || minecraft.level == null || pos.equals(BlockPos.ZERO)) {
            return null;
        }
        if (minecraft.level.getBlockEntity(pos) instanceof AutoShopBlockEntity autoShop) {
            return autoShop.getBoundEntry();
        }
        return null;
    }

    private ItemStack getFilterGhostItem() {
        BlockPos pos = menu.getSyncedBlockPos();
        if (minecraft == null || minecraft.level == null || pos.equals(BlockPos.ZERO)) {
            return ItemStack.EMPTY;
        }
        if (minecraft.level.getBlockEntity(pos) instanceof AutoShopBlockEntity autoShop) {
            ShopEntry bound = autoShop.getBoundEntry();
            if (bound != null && bound.type == ShopEntry.EntryType.ITEM) {
                ItemStack display = ShopEntryHelper.displayStackForEntry(bound);
                if (!display.isEmpty()) {
                    return display;
                }
            }
            return autoShop.getSelectedItem();
        }
        return ItemStack.EMPTY;
    }

    private void renderFilterGhostTooltip(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        Slot filterSlot = menu.getSlot(AutoShopMenu.FILTER_SLOT_INDEX);
        if (filterSlot == null || !filterSlot.getItem().isEmpty()) {
            return;
        }
        if (!isMouseOverSlot(filterSlot, mouseX, mouseY)) {
            return;
        }
        ShopEntry bound = getBoundShopEntry();
        if (bound != null) {
            guiGraphics.setTooltipForNextFrame(
                    font,
                    List.of(ShopEntryHelper.displayTooltipForEntry(bound).getVisualOrderText()),
                    DefaultTooltipPositioner.INSTANCE,
                    mouseX,
                    mouseY,
                    true);
            return;
        }
        ItemStack ghost = getFilterGhostItem();
        if (ghost.isEmpty()) {
            return;
        }
        guiGraphics.setTooltipForNextFrame(
                font,
                List.of(ghost.getHoverName().getVisualOrderText()),
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
        if (subView != SubView.MAIN) {
            return null;
        }
        return new IGhostIngredientConsumer() {
            @Override
            public Object supportedTarget(Object ingredient) {
                if (ingredient instanceof ItemStack stack && !stack.isEmpty()) {
                    return stack;
                }
                if (ingredient instanceof FluidStack fluid && !fluid.isEmpty()) {
                    return fluid;
                }
                if (MekChemicalHelper.isLoaded()
                        && MekChemicalHelper.isChemicalStackObject(ingredient)
                        && !MekChemicalHelper.isEmpty(ingredient)) {
                    return ingredient;
                }
                return null;
            }

            @Override
            public void accept(Object ingredient) {
                if (ingredient instanceof ItemStack stack) {
                    acceptJeiFilterItem(stack);
                } else if (ingredient instanceof FluidStack fluid) {
                    acceptJeiFilterFluid(fluid);
                } else if (MekChemicalHelper.isLoaded() && MekChemicalHelper.isChemicalStackObject(ingredient)) {
                    acceptJeiFilterGas(ingredient);
                }
            }
        };
    }

    @Override
    public Rect2i getGhostTargetArea() {
        if (subView != SubView.MAIN) {
            return null;
        }
        Slot filterSlot = menu.getSlot(AutoShopMenu.FILTER_SLOT_INDEX);
        if (filterSlot == null) {
            return null;
        }
        return new Rect2i(leftPos + filterSlot.x - 1, topPos + filterSlot.y - 1, 18, 18);
    }

    private void onConvertBucketPressed() {
        playButtonSound();
        BlockPos machinePos = resolveMachinePos();
        if (!machinePos.equals(BlockPos.ZERO)) {
            net.unfamily.iskautils.network.ModMessages.sendAutoShopConvertSelectedPacket(machinePos);
        }
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

    private void acceptJeiFilterFluid(FluidStack fluid) {
        if (fluid == null || fluid.isEmpty()) {
            return;
        }
        ShopEntry entry = ShopEntryHelper.findMatchingFluidEntry(fluid, menu.isAutoBuyMode());
        tryApplyTypedShopEntry(entry);
    }

    private void acceptJeiFilterGas(Object chemicalStack) {
        if (!MekChemicalHelper.isLoaded() || MekChemicalHelper.isEmpty(chemicalStack)) {
            return;
        }
        ShopEntry entry = ShopEntryHelper.findMatchingGasEntry(chemicalStack, menu.isAutoBuyMode());
        tryApplyTypedShopEntry(entry);
    }

    private void tryApplyTypedShopEntry(@Nullable ShopEntry entry) {
        if (entry == null) {
            return;
        }
        BlockPos machinePos = resolveMachinePos();
        if (machinePos.equals(BlockPos.ZERO)) {
            return;
        }
        boolean buyMode = ShopEntryHelper.resolveBuyModeForEntry(entry, menu.isAutoBuyMode());
        if (!ShopBrowsePanel.isSelectableAutoShopEntry(entry, buyMode)) {
            return;
        }
        net.unfamily.iskautils.network.ModMessages.sendAutoShopApplyPickerSelectionPacket(
                machinePos, entry.id, buyMode);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (subView == SubView.ITEM_PICKER) {
            if (itemPicker.mouseClicked(event, doubleClick)) {
                return true;
            }
            return super.mouseClicked(event, doubleClick);
        }

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

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (subView == SubView.ITEM_PICKER && itemPicker.mouseReleased(event)) {
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (subView == SubView.ITEM_PICKER && itemPicker.mouseDragged(event, dx, dy)) {
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (subView == SubView.ITEM_PICKER && itemPicker.mouseScrolled(mouseX, mouseY, deltaY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (subView == SubView.ITEM_PICKER) {
            if (event.key() == GLFW.GLFW_KEY_ESCAPE
                    || (minecraft != null && minecraft.options.keyInventory.matches(event))) {
                itemPicker.handleEscape();
                return true;
            }
            if (itemPicker.keyPressed(this, event)) {
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (subView == SubView.ITEM_PICKER && itemPicker.charTyped(event)) {
            return true;
        }
        return super.charTyped(event);
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

    private void dumpTank(boolean gas) {
        BlockPos machinePos = resolveMachinePos();
        if (!machinePos.equals(BlockPos.ZERO)) {
            playButtonSound();
            net.unfamily.iskautils.network.ModMessages.sendAutoShopDumpTankPacket(machinePos, gas);
        }
    }
}

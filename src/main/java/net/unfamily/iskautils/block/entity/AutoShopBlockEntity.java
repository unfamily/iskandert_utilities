package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.unfamily.iskalib.transfer.LegacyIFluidHandlerResourceHandler;
import net.unfamily.iskalib.transfer.LegacyItemHandlerResourceHandler;
import net.unfamily.iskautils.integration.mekanism.MekChemicalHelper;
import net.unfamily.iskautils.shop.ShopEntry;
import net.unfamily.iskautils.shop.ShopEntryHelper;
import org.jetbrains.annotations.NotNull;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;

/**
 * Block Entity for Auto Shop Block
 * Manages automatic item extraction via hopper and similar devices
 */
public class AutoShopBlockEntity extends BlockEntity {
    /** Tank capacity equals one shop unit ({@link ShopEntry#amount}); buy only when empty. */
    public static final int DEFAULT_TANK_MB = 1000;
    
    // Custom slot for encapsulated function (1 slot) - exposed for automatic extraction
    private final ItemStackHandler encapsulatedSlot = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return true;
        }
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            // Always allow extraction
            return super.extractItem(slot, amount, simulate);
        }
    };

    private final ResourceHandler<ItemResource> itemTransferHandler = LegacyItemHandlerResourceHandler.wrap(encapsulatedSlot);
    private final FluidTank fluidTank = new FluidTank(DEFAULT_TANK_MB, this::isFluidValidForSelection) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final ResourceHandler<FluidResource> fluidTransferHandler =
            LegacyIFluidHandlerResourceHandler.wrap(fluidTank);
    private Object gasTank;
    private String pendingGasId = "";
    private long pendingGasAmount;
    private int tankCapacity = DEFAULT_TANK_MB;

    /** Read-only handler for the filter slot display (ghost slot: no insert/extract). */
    private final IItemHandler filterDisplayHandler = new IItemHandler() {
        @Override
        public int getSlots() {
            return 1;
        }
        @Override
        @NotNull
        public ItemStack getStackInSlot(int slot) {
            return slot == 0 ? selectedItem.copy() : ItemStack.EMPTY;
        }
        @Override
        @NotNull
        public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return stack; // No-op: ghost slot does not accept items
        }
        @Override
        @NotNull
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY; // No-op: ghost slot does not give items
        }
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return false;
        }
    };

    // Shop state (simplified)
    private boolean isActive = false;
    private String currentCategory = "000_default";
    private String selectedValute = resolveDefaultCurrencyId();
    private UUID ownerTeamId = null; // Team ID of the player who placed the AutoShop
    private UUID placedByPlayer = null; // UUID of the player who placed the Auto Shop
    private ItemStack selectedItem = ItemStack.EMPTY; // Selected item for encapsulated slot
    private String selectedShopEntryId = ""; // Bound shop entry from picker (optional)
    private ShopEntry.EntryType selectedEntryType = ShopEntry.EntryType.ITEM;
    private boolean autoBuyMode = true; // true = Auto Buy, false = Auto Sell
    
    // Redstone mode: when to run auto buy/sell (same logic as Structure Placer Machine)
    private int redstoneMode = 0;
    private boolean previousRedstoneState = false;
    
    /**
     * Redstone modes: when the auto shop is allowed to run
     */
    public enum RedstoneMode {
        NONE(0),    // Always active
        LOW(1),     // Only when redstone signal is OFF
        HIGH(2),    // Only when redstone signal is ON
        PULSE(3),   // Only on redstone rising edge (low to high)
        DISABLED(4); // Never active
        private final int value;
        RedstoneMode(int value) { this.value = value; }
        public int getValue() { return value; }
        public static RedstoneMode fromValue(int value) {
            for (RedstoneMode m : values()) if (m.value == value) return m;
            return NONE;
        }
        public RedstoneMode next() {
            return switch (this) {
                case NONE -> LOW;
                case LOW -> HIGH;
                case HIGH -> PULSE;
                case PULSE -> DISABLED;
                case DISABLED -> NONE;
            };
        }

        public RedstoneMode previous() {
            return switch (this) {
                case NONE -> DISABLED;
                case LOW -> NONE;
                case HIGH -> LOW;
                case PULSE -> HIGH;
                case DISABLED -> PULSE;
            };
        }
    }
    
    public AutoShopBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.AUTO_SHOP_BE.get(), pos, blockState);
    }
    
    private static final String ENCAPSULATED_SLOT_TAG = "encapsulatedSlot";
    private static final String SELECTED_ITEM_TAG = "selectedItem";
    private static final String SELECTED_SHOP_ENTRY_ID_TAG = "selectedShopEntryId";
    private static final String SELECTED_ENTRY_TYPE_TAG = "selectedEntryType";
    private static final String FLUID_TANK_TAG = "fluidTank";
    private static final String GAS_ID_TAG = "gasId";
    private static final String GAS_AMOUNT_TAG = "gasAmount";
    private static final String TANK_CAPACITY_TAG = "tankCapacity";

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);

        output.putBoolean("isActive", isActive);
        output.putString("currentCategory", currentCategory);
        output.putString("selectedValute", selectedValute);
        output.putBoolean("autoBuyMode", autoBuyMode);
        output.putString("ownerTeamId", ownerTeamId != null ? ownerTeamId.toString() : "");
        output.putString("placedByPlayer", placedByPlayer != null ? placedByPlayer.toString() : "");
        output.putInt("redstoneMode", redstoneMode);
        output.putBoolean("previousRedstoneState", previousRedstoneState);

        ItemStack slotStack = encapsulatedSlot.getStackInSlot(0);
        if (!slotStack.isEmpty()) {
            output.store(ENCAPSULATED_SLOT_TAG, ItemStack.CODEC, slotStack);
        }
        if (!selectedItem.isEmpty()) {
            output.store(SELECTED_ITEM_TAG, ItemStack.CODEC, selectedItem);
        }
        if (selectedShopEntryId != null && !selectedShopEntryId.isEmpty()) {
            output.putString(SELECTED_SHOP_ENTRY_ID_TAG, selectedShopEntryId);
        }
        output.putString(SELECTED_ENTRY_TYPE_TAG, selectedEntryType.name());
        output.putInt(TANK_CAPACITY_TAG, tankCapacity);
        if (!fluidTank.isEmpty()) {
            output.store(FLUID_TANK_TAG, FluidStack.CODEC, fluidTank.getFluid());
        }
        String gasId = getGasId();
        long gasAmount = getGasAmount();
        if (gasId != null && !gasId.isEmpty() && gasAmount > 0) {
            output.putString(GAS_ID_TAG, gasId);
            output.putLong(GAS_AMOUNT_TAG, gasAmount);
        }
    }
    
    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        this.isActive = input.getBooleanOr("isActive", false);
        this.currentCategory = input.getStringOr("currentCategory", "000_default");

        this.selectedValute = normalizeCurrencyId(input.getStringOr("selectedValute", resolveDefaultCurrencyId()));

        this.autoBuyMode = input.getBooleanOr("autoBuyMode", true);

        String ownerTeamStr = input.getStringOr("ownerTeamId", "");
        this.ownerTeamId = ownerTeamStr.isEmpty() ? null : UUID.fromString(ownerTeamStr);

        String placedByStr = input.getStringOr("placedByPlayer", "");
        this.placedByPlayer = placedByStr.isEmpty() ? null : UUID.fromString(placedByStr);

        this.redstoneMode = input.getIntOr("redstoneMode", 0);
        this.previousRedstoneState = input.getBooleanOr("previousRedstoneState", false);

        ItemStack loadedSlot = input.read(ENCAPSULATED_SLOT_TAG, ItemStack.CODEC).orElse(ItemStack.EMPTY);
        encapsulatedSlot.setStackInSlot(0, loadedSlot);

        this.selectedItem = input.read(SELECTED_ITEM_TAG, ItemStack.CODEC).orElse(ItemStack.EMPTY);
        if (!this.selectedItem.isEmpty()) {
            this.selectedItem = this.selectedItem.copy();
            this.selectedItem.setCount(1);
        }
        this.selectedShopEntryId = input.getStringOr(SELECTED_SHOP_ENTRY_ID_TAG, "");
        try {
            this.selectedEntryType = ShopEntry.EntryType.valueOf(
                    input.getStringOr(SELECTED_ENTRY_TYPE_TAG, ShopEntry.EntryType.ITEM.name()));
        } catch (IllegalArgumentException ignored) {
            this.selectedEntryType = ShopEntry.EntryType.ITEM;
        }
        this.tankCapacity = Math.max(1, input.getIntOr(TANK_CAPACITY_TAG, DEFAULT_TANK_MB));
        this.fluidTank.setCapacity(tankCapacity);
        this.fluidTank.setFluid(input.read(FLUID_TANK_TAG, FluidStack.CODEC).orElse(FluidStack.EMPTY));
        this.pendingGasId = input.getStringOr(GAS_ID_TAG, "");
        this.pendingGasAmount = Math.max(0L, input.getLongOr(GAS_AMOUNT_TAG, 0L));
        ensureGasTank();
    }

    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.merge(this.saveCustomOnly(registries));
        return tag;
    }
    
    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }
    
    // Methods for data access
    
    public boolean isActive() {
        return this.isActive;
    }
    
    public void setActive(boolean active) {
        this.isActive = active;
        setChanged();
    }
    
    public String getCurrentCategory() {
        return this.currentCategory;
    }
    
    public void setCurrentCategory(String category) {
        this.currentCategory = category;
        setChanged();
    }
    
    public ItemStackHandler getEncapsulatedSlot() {
        return encapsulatedSlot;
    }

    public ResourceHandler<ItemResource> getItemTransferHandler() {
        return itemTransferHandler;
    }

    public ResourceHandler<FluidResource> getFluidTransferHandler() {
        return fluidTransferHandler;
    }

    public int getFluidAmount() {
        return fluidTank.getFluidAmount();
    }

    public int getFluidCapacity() {
        return fluidTank.getCapacity();
    }

    public int getFluidRegistryId() {
        return fluidTank.isEmpty() ? -1 : BuiltInRegistries.FLUID.getId(fluidTank.getFluid().getFluid());
    }

    public long getGasAmount() {
        ensureGasTank();
        return gasTank != null ? MekChemicalHelper.getTankAmountLong(gasTank) : pendingGasAmount;
    }

    public long getGasCapacity() {
        ensureGasTank();
        return gasTank != null ? MekChemicalHelper.getTankCapacityLong(gasTank) : tankCapacity;
    }

    public String getGasId() {
        ensureGasTank();
        if (gasTank != null) {
            String id = MekChemicalHelper.getRegistryName(MekChemicalHelper.getChemicalInTank(gasTank, 0));
            return id != null ? id : "";
        }
        return pendingGasId;
    }

    public ShopEntry.EntryType getSelectedEntryType() {
        return selectedEntryType;
    }

    public void dumpFluidTankContents() {
        if (!fluidTank.isEmpty()) {
            fluidTank.setFluid(FluidStack.EMPTY);
            setChanged();
        }
    }

    public void dumpGasTankContents() {
        boolean changed = MekChemicalHelper.dumpTank(gasTank);
        if (pendingGasAmount > 0) {
            pendingGasAmount = 0;
            pendingGasId = "";
            changed = true;
        }
        if (changed) {
            setChanged();
        }
    }

    private boolean isFluidValidForSelection(FluidStack stack) {
        if (stack.isEmpty() || selectedEntryType != ShopEntry.EntryType.FLUID) {
            return true;
        }
        ShopEntry entry = getBoundEntry();
        return entry == null || ShopEntryHelper.matchesFluid(stack, entry.fluid);
    }

    public ShopEntry getBoundEntry() {
        return selectedShopEntryId == null || selectedShopEntryId.isEmpty()
                ? null
                : net.unfamily.iskautils.shop.ShopLoader.getEntries().get(selectedShopEntryId);
    }

    private void ensureGasTank() {
        if (gasTank != null || !MekChemicalHelper.isLoaded()) {
            return;
        }
        gasTank = MekChemicalHelper.createAllValidTank(tankCapacity);
        if (gasTank != null && pendingGasAmount > 0 && !pendingGasId.isEmpty()) {
            Object stack = MekChemicalHelper.createStackFromId(pendingGasId, pendingGasAmount);
            MekChemicalHelper.fill(gasTank, stack, false);
            pendingGasAmount = 0;
            pendingGasId = "";
        }
    }

    private void resizeTanks(int entryAmount) {
        // One shop unit fills the "slot"; no oversized buffer capacity.
        tankCapacity = Math.max(1, entryAmount);
        fluidTank.setCapacity(tankCapacity);
        if (fluidTank.getFluidAmount() > tankCapacity) {
            fluidTank.setFluid(fluidTank.getFluid().copyWithAmount(tankCapacity));
        }
        if (gasTank != null) {
            MekChemicalHelper.setTankCapacity(gasTank, tankCapacity);
        }
    }

    /** Returns the read-only filter display handler (ghost slot: display only, no put/take). */
    public IItemHandler getFilterDisplayHandler() {
        return filterDisplayHandler;
    }
    
    public String getSelectedValute() {
        return this.selectedValute;
    }
    
    public void setSelectedValute(String valute) {
        this.selectedValute = normalizeCurrencyId(valute);
        setChanged();
    }

    public static String resolveDefaultCurrencyId() {
        List<String> ids = getSortedCurrencyIds();
        return ids.isEmpty() ? "null_coin" : ids.getFirst();
    }

    public static List<String> getSortedCurrencyIds() {
        return new ArrayList<>(net.unfamily.iskautils.shop.ShopLoader.getCurrencies().keySet().stream().sorted().toList());
    }

    public static int getCurrencyIndex(String currencyId) {
        List<String> ids = getSortedCurrencyIds();
        if (ids.isEmpty()) {
            return 0;
        }
        int index = ids.indexOf(normalizeCurrencyId(currencyId));
        return index >= 0 ? index : 0;
    }

    public static String getCurrencyIdFromIndex(int index) {
        List<String> ids = getSortedCurrencyIds();
        if (ids.isEmpty()) {
            return "null_coin";
        }
        if (index < 0 || index >= ids.size()) {
            return ids.getFirst();
        }
        return ids.get(index);
    }

    public static String normalizeCurrencyId(String currencyId) {
        if (currencyId == null || currencyId.isEmpty() || "unset".equals(currencyId)) {
            return resolveDefaultCurrencyId();
        }
        if (!net.unfamily.iskautils.shop.ShopLoader.getCurrencies().containsKey(currencyId)) {
            return resolveDefaultCurrencyId();
        }
        return currencyId;
    }

    public void ensureDefaultCurrency() {
        this.selectedValute = normalizeCurrencyId(this.selectedValute);
        setChanged();
    }

    public void cycleCurrency(boolean backward) {
        List<String> ids = getSortedCurrencyIds();
        if (ids.isEmpty()) {
            this.selectedValute = "null_coin";
            setChanged();
            return;
        }
        int currentIndex = ids.indexOf(this.selectedValute);
        if (currentIndex < 0) {
            currentIndex = 0;
        }
        int nextIndex = backward
                ? (currentIndex - 1 + ids.size()) % ids.size()
                : (currentIndex + 1) % ids.size();
        this.selectedValute = ids.get(nextIndex);
        setChanged();
    }

    public int getCurrencyIndex() {
        return getCurrencyIndex(this.selectedValute);
    }
    
    public UUID getOwnerTeamId() {
        return this.ownerTeamId;
    }
    
    public void setOwnerTeamId(UUID teamId) {
        this.ownerTeamId = teamId;
        setChanged();
    }
    
    public String getOwnerTeamName() {
        if (this.ownerTeamId != null && this.level != null && !this.level.isClientSide()) {
            if (this.level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                return net.unfamily.iskalib.team.ShopTeamManager.getInstance(serverLevel)
                        .getTeamNameById(this.ownerTeamId);
            }
        }
        return null;
    }
    
    public ItemStack getSelectedItem() {
        return this.selectedItem;
    }
    
    public void setSelectedItem(ItemStack item) {

        
        if (item.isEmpty()) {
            this.selectedItem = ItemStack.EMPTY;
        } else {
            // Create a copy of the item with count 1, preserving NBT
            this.selectedItem = item.copy();
            this.selectedItem.setCount(1);
            

        }
        this.selectedShopEntryId = "";
        this.selectedEntryType = ShopEntry.EntryType.ITEM;
        resizeTanks(1);

        
        setChanged();
    }

    public String getSelectedShopEntryId() {
        return selectedShopEntryId != null ? selectedShopEntryId : "";
    }

    public void applyPickerSelection(ItemStack item, String currencyId, boolean buyMode, String entryId) {
        ShopEntry entry = entryId != null ? net.unfamily.iskautils.shop.ShopLoader.getEntries().get(entryId) : null;
        this.selectedEntryType = entry != null ? entry.type : ShopEntry.EntryType.ITEM;
        this.selectedItem = item == null ? ItemStack.EMPTY : item.copy();
        if (!this.selectedItem.isEmpty()) {
            this.selectedItem.setCount(1);
        }
        this.selectedShopEntryId = entryId != null ? entryId : "";
        this.selectedValute = normalizeCurrencyId(currencyId);
        this.autoBuyMode = buyMode;
        resizeTanks(entry != null ? entry.amount : 1);
        setChanged();
    }
    
    public boolean hasSelectedItem() {
        return !this.selectedItem.isEmpty();
    }
    
    public void clearSelectedItem() {
        this.selectedItem = ItemStack.EMPTY;
        this.selectedShopEntryId = "";
        this.selectedEntryType = ShopEntry.EntryType.ITEM;
        resizeTanks(1);
        setChanged();
    }
    
    public boolean isAutoBuyMode() {
        return this.autoBuyMode;
    }
    
    public void setAutoBuyMode(boolean autoBuyMode) {
        this.autoBuyMode = autoBuyMode;
        setChanged();
    }
    
    public void toggleAutoMode() {
        this.autoBuyMode = !this.autoBuyMode;
        setChanged();
    }
    
    public UUID getPlacedByPlayer() {
        return placedByPlayer;
    }
    
    public void setPlacedByPlayer(UUID placedByPlayer) {
        this.placedByPlayer = placedByPlayer;
        setChanged();
    }
    
    public int getRedstoneMode() {
        return redstoneMode;
    }
    
    public void setRedstoneMode(int redstoneMode) {
        this.redstoneMode = redstoneMode % 5;
        setChanged();
    }
    
    /**
     * Checks if a player can use this AutoShop
     * Verifies that the player still belongs to the saved team
     * And that the player who placed the AutoShop is still in the team
     */
    public boolean canPlayerUse(ServerPlayer player) {
        // If there's no saved team, only the player who placed it can use it
        if (ownerTeamId == null) {
            return player.getUUID().equals(placedByPlayer);
        }
        
        // If there's a saved team, check that the player still belongs to that team
        if (level != null && !level.isClientSide()) {
            net.unfamily.iskalib.team.ShopTeamManager teamManager =
                net.unfamily.iskalib.team.ShopTeamManager.getInstance((net.minecraft.server.level.ServerLevel) player.level());
            
            // Get player's team
            String playerTeamName = teamManager.getPlayerTeam(player);
            if (playerTeamName == null) {
                return false; // Player is not in a team
            }
            
            // Get player's team ID
            UUID playerTeamId = teamManager.getTeamIdByName(playerTeamName);
            if (playerTeamId == null) {
                return false; // Player's team not found
            }
            
            // Check that it's the same team
            if (!playerTeamId.equals(ownerTeamId)) {
                return false; // Player is not in the saved team
            }
            
            // Check that the player who placed the AutoShop is still in the team
            if (placedByPlayer != null) {
                String placerTeamName = teamManager.getPlayerTeam(placedByPlayer);
                if (placerTeamName == null || !placerTeamName.equals(playerTeamName)) {
                    return false; // The placer is no longer in the team
                }
            }
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Searches for a ShopEntry with an exact ItemStack match (same item and components).
     */
    private static net.unfamily.iskautils.shop.ShopEntry findEntryForItemExact(ItemStack templateItem, String boundEntryId) {
        if (boundEntryId != null && !boundEntryId.isEmpty()) {
            net.unfamily.iskautils.shop.ShopEntry bound = net.unfamily.iskautils.shop.ShopLoader.getEntries().get(boundEntryId);
            if (bound != null && bound.type == ShopEntry.EntryType.ITEM
                    && ShopEntryHelper.matchesItem(templateItem, bound.item)) {
                return bound;
            }
        }
        Map<String, net.unfamily.iskautils.shop.ShopEntry> allEntries = net.unfamily.iskautils.shop.ShopLoader.getEntries();
        for (Map.Entry<String, net.unfamily.iskautils.shop.ShopEntry> entryMap : allEntries.entrySet()) {
            net.unfamily.iskautils.shop.ShopEntry entry = entryMap.getValue();
            if (entry.type == ShopEntry.EntryType.ITEM && ShopEntryHelper.matchesItem(templateItem, entry.item)) {
                return entry;
            }
        }
        return null;
    }

    /** Resolves the internal shop team key from a saved team UUID (not display name). */
    private static String resolveTeamKey(net.unfamily.iskalib.team.ShopTeamManager teamManager, java.util.UUID ownerTeamId) {
        if (ownerTeamId == null) {
            return null;
        }
        for (String key : teamManager.getAllTeams()) {
            java.util.UUID id = teamManager.getTeamIdByName(key);
            if (ownerTeamId.equals(id)) {
                return key;
            }
        }
        return null;
    }

    private static boolean stagesMet(ShopEntry entry, net.unfamily.iskalib.stage.StageRegistry registry,
                                     ServerPlayer placerPlayer, String teamKey) {
        if (entry.stages == null || entry.stages.length == 0 || registry == null) {
            return true;
        }
        for (var stage : entry.stages) {
            String type = stage.stageType != null ? stage.stageType.toLowerCase() : "world";
            boolean actual = switch (type) {
                case "player" -> placerPlayer != null && registry.hasPlayerStage(placerPlayer, stage.stage);
                case "team" -> registry.hasTeamStage(teamKey, stage.stage);
                default -> registry.hasWorldStage(stage.stage);
            };
            if (actual != stage.is) {
                return false;
            }
        }
        return true;
    }

    private static void processTypedEntry(AutoShopBlockEntity entity, ShopEntry entry,
                                          net.unfamily.iskalib.team.ShopTeamManager teamManager,
                                          String teamKey, String currencyId,
                                          net.unfamily.iskalib.stage.StageRegistry registry,
                                          ServerPlayer placerPlayer) {
        if (entry == null || entry.type == ShopEntry.EntryType.ITEM || entry.amount <= 0) {
            return;
        }
        String entryCurrency = entry.valute != null ? entry.valute : "null_coin";
        if (!entryCurrency.equals(currencyId) || !stagesMet(entry, registry, placerPlayer, teamKey)) {
            return;
        }

        if (!entity.isAutoBuyMode()) {
            if (!ShopEntryHelper.isSellAllowed(entry) || !entity.hasTypedAmount(entry)) {
                return;
            }
            if (entity.extractTyped(entry)) {
                teamManager.addTeamValutes(teamKey, currencyId, entry.sell);
                entity.setChanged();
            }
            return;
        }

        if (!ShopEntryHelper.isBuyAllowed(entry) || !entity.canInsertTyped(entry)) {
            return;
        }
        double cost = entry.free ? 0 : entry.buy;
        if (teamManager.getTeamValuteBalance(teamKey, currencyId) < cost
                || !teamManager.removeTeamValutes(teamKey, currencyId, cost)) {
            return;
        }
        if (!entity.insertTyped(entry) && cost > 0) {
            teamManager.addTeamValutes(teamKey, currencyId, cost);
        }
    }

    private boolean hasTypedAmount(ShopEntry entry) {
        return switch (entry.type) {
            case FLUID -> fluidTank.getFluidAmount() >= entry.amount
                    && ShopEntryHelper.matchesFluid(fluidTank.getFluid(), entry.fluid);
            case GAS -> getGasAmount() >= entry.amount
                    && ShopEntryHelper.matchesGas(MekChemicalHelper.getChemicalInTank(gasTank, 0), entry.gas);
            case ITEM -> false;
        };
    }

    private boolean extractTyped(ShopEntry entry) {
        return switch (entry.type) {
            case FLUID -> fluidTank.drain(entry.amount, IFluidHandler.FluidAction.EXECUTE).getAmount() == entry.amount;
            case GAS -> MekChemicalHelper.extractFromTank(gasTank, entry.amount) == entry.amount;
            case ITEM -> false;
        };
    }

    private boolean canInsertTyped(ShopEntry entry) {
        // Like the item encapsulated slot: buy only when the tank is empty.
        return switch (entry.type) {
            case FLUID -> {
                if (!fluidTank.isEmpty()) {
                    yield false;
                }
                var fluid = ShopEntryHelper.resolveFluid(entry.fluid);
                yield fluid != null && fluidTank.fill(new FluidStack(fluid, entry.amount),
                        IFluidHandler.FluidAction.SIMULATE) == entry.amount;
            }
            case GAS -> {
                ensureGasTank();
                if (getGasAmount() > 0) {
                    yield false;
                }
                Object stack = MekChemicalHelper.createStackFromId(entry.gas, entry.amount);
                yield stack != null && MekChemicalHelper.fill(gasTank, stack, true) == entry.amount;
            }
            case ITEM -> false;
        };
    }

    private boolean insertTyped(ShopEntry entry) {
        return switch (entry.type) {
            case FLUID -> {
                var fluid = ShopEntryHelper.resolveFluid(entry.fluid);
                yield fluid != null && fluidTank.fill(new FluidStack(fluid, entry.amount),
                        IFluidHandler.FluidAction.EXECUTE) == entry.amount;
            }
            case GAS -> {
                Object stack = MekChemicalHelper.createStackFromId(entry.gas, entry.amount);
                yield stack != null && MekChemicalHelper.fill(gasTank, stack, false) == entry.amount;
            }
            case ITEM -> false;
        };
    }
    
    /**
     * Block tick (called by server)
     */
    public static void tick(Level level, BlockPos pos, BlockState state, AutoShopBlockEntity entity) {
        if (level.isClientSide()) {
            return;
        }

        // Redstone gate: decide if auto shop is allowed to run this tick
        int redstonePower = level.getBestNeighborSignal(pos);
        boolean hasRedstoneSignal = redstonePower > 0;
        RedstoneMode mode = RedstoneMode.fromValue(entity.getRedstoneMode());
        boolean shouldRun = false;
        switch (mode) {
            case DISABLED -> shouldRun = false;
            case NONE -> shouldRun = true;
            case LOW -> shouldRun = !hasRedstoneSignal;
            case HIGH -> shouldRun = hasRedstoneSignal;
            case PULSE -> {
                if (hasRedstoneSignal && !entity.previousRedstoneState) {
                    shouldRun = true;
                }
                entity.previousRedstoneState = hasRedstoneSignal;
            }
        }
        if (!shouldRun) {
            return;
        }

        net.minecraft.server.level.ServerLevel serverLevel = (net.minecraft.server.level.ServerLevel) level;
        net.unfamily.iskalib.team.ShopTeamManager teamManager = net.unfamily.iskalib.team.ShopTeamManager.getInstance(serverLevel);

        // Lazy-bind team if the placer joined a team after placing the block
        if (entity.getOwnerTeamId() == null && entity.getPlacedByPlayer() != null) {
            String placerTeamName = teamManager.getPlayerTeam(entity.getPlacedByPlayer());
            if (placerTeamName != null) {
                java.util.UUID teamId = teamManager.getTeamIdByName(placerTeamName);
                if (teamId != null) {
                    entity.setOwnerTeamId(teamId);
                }
            }
        }

        // Get the owner's team (needed for both modes)
        if (entity.getOwnerTeamId() == null) {
            return;
        }
        
        String teamKey = resolveTeamKey(teamManager, entity.getOwnerTeamId());
        if (teamKey == null) {
            return; // Team no longer exists
        }

        // Check that the player who placed the AutoShop is still in the team
        if (entity.getPlacedByPlayer() != null) {
            String placerTeamKey = teamManager.getPlayerTeam(entity.getPlacedByPlayer());
            if (placerTeamKey == null || !placerTeamKey.equals(teamKey)) {
                return; // The placer is no longer in the team, block the AutoShop
            }
        }

        // Retrieve the placer's ServerPlayer (if online) - needed for player stage
        net.minecraft.server.level.ServerPlayer placerPlayer = serverLevel.getServer().getPlayerList().getPlayer(entity.getPlacedByPlayer());

        String currencyId = normalizeCurrencyId(entity.getSelectedValute());
        if (!currencyId.equals(entity.getSelectedValute())) {
            entity.setSelectedValute(currencyId);
        }

        ShopEntry boundEntry = entity.getBoundEntry();
        if (boundEntry != null && boundEntry.type != ShopEntry.EntryType.ITEM) {
            net.unfamily.iskalib.stage.StageRegistry registry =
                    net.unfamily.iskalib.stage.StageRegistry.getInstance(serverLevel.getServer());
            processTypedEntry(entity, boundEntry, teamManager, teamKey, currencyId, registry, placerPlayer);
            return;
        }

        // SELL mode
        if (!entity.isAutoBuyMode()) {
            ItemStackHandler slot = entity.getEncapsulatedSlot();
            ItemStack stack = slot.getStackInSlot(0);
            if (stack.isEmpty()) {
                return;
            }

            ItemStack filterItem = entity.getSelectedItem();
            ShopEntry selectedEntry = entity.getBoundEntry();
            String itemSelector = selectedEntry != null ? selectedEntry.item : null;
            if (itemSelector != null
                    ? !ShopEntryHelper.matchesItem(stack, itemSelector)
                    : (!filterItem.isEmpty() && !ItemStack.isSameItemSameComponents(stack, filterItem))) {
                return;
            }

            net.unfamily.iskautils.shop.ShopEntry entry = findEntryForItemExact(
                    filterItem.isEmpty() ? stack : filterItem, entity.getSelectedShopEntryId());
            if (entry == null || entry.sell <= 0) {
                return;
            }

            // Check that currency matches
            String entryCurrency = entry.valute != null ? entry.valute : "null_coin";
            if (!entryCurrency.equals(currencyId)) {
                return;
            }

            // Check required stages
            net.unfamily.iskalib.stage.StageRegistry registry = net.unfamily.iskalib.stage.StageRegistry.getInstance(serverLevel.getServer());
            if (entry.stages != null && entry.stages.length > 0 && registry != null) {
                boolean hasAllStages = true;
                for (var stage : entry.stages) {
                    boolean stageMet = false;
                    String type = stage.stageType != null ? stage.stageType.toLowerCase() : "world";
                    if ("player".equals(type)) {
                        if (placerPlayer == null) {
                            hasAllStages = false;
                            break;
                        }
                        boolean hasPlayerStage = registry.hasPlayerStage(placerPlayer, stage.stage);
                        stageMet = (hasPlayerStage == stage.is);
                    } else if ("team".equals(type)) {
                        boolean hasTeamStage = registry.hasTeamStage(teamKey, stage.stage);
                        stageMet = (hasTeamStage == stage.is);
                    } else if ("world".equals(type)) {
                        boolean hasWorldStage = registry.hasWorldStage(stage.stage);
                        stageMet = (hasWorldStage == stage.is);
                    }
                    if (!stageMet) {
                        hasAllStages = false;
                        break;
                    }
                }
                if (!hasAllStages) {
                    return;
                }
            }

            // Check that there are enough items in the slot
            if (stack.getCount() < entry.amount) {
                return;
            }

            // Remove correct count from slot
            ItemStack removed = slot.extractItem(0, entry.amount, false);
            if (removed.isEmpty()) {
                return;
            }

            // Credit money to team (only single value)
            teamManager.addTeamValutes(teamKey, currencyId, entry.sell);
            entity.setChanged();
        }
        // BUY mode
        else {
            // Check if the slot is empty (can only buy if empty)
            ItemStackHandler slot = entity.getEncapsulatedSlot();
            ItemStack stack = slot.getStackInSlot(0);
            if (!stack.isEmpty()) {
                return;
            }

            // Check if there's a filter item set (ghost slot)
            ItemStack selectedStack = entity.getSelectedItem();
            if (selectedStack.isEmpty()) {
                return;
            }

            net.unfamily.iskautils.shop.ShopEntry entry = findEntryForItemExact(selectedStack, entity.getSelectedShopEntryId());
            if (entry == null || (entry.buy <= 0 && !entry.free)) {
                return;
            }

            // Check that currency matches
            String entryCurrency = entry.valute != null ? entry.valute : "null_coin";
            if (!entryCurrency.equals(currencyId)) {
                return;
            }

            // Check required stages
            net.unfamily.iskalib.stage.StageRegistry registry = net.unfamily.iskalib.stage.StageRegistry.getInstance(serverLevel.getServer());
            if (entry.stages != null && entry.stages.length > 0 && registry != null) {
                boolean hasAllStages = true;
                for (var stage : entry.stages) {
                    boolean stageMet = false;
                    String type = stage.stageType != null ? stage.stageType.toLowerCase() : "world";
                    if ("player".equals(type)) {
                        if (placerPlayer == null) {
                            hasAllStages = false;
                            break;
                        }
                        boolean hasPlayerStage = registry.hasPlayerStage(placerPlayer, stage.stage);
                        stageMet = (hasPlayerStage == stage.is);
                    } else if ("team".equals(type)) {
                        boolean hasTeamStage = registry.hasTeamStage(teamKey, stage.stage);
                        stageMet = (hasTeamStage == stage.is);
                    } else if ("world".equals(type)) {
                        boolean hasWorldStage = registry.hasWorldStage(stage.stage);
                        stageMet = (hasWorldStage == stage.is);
                    }
                    if (!stageMet) {
                        hasAllStages = false;
                        break;
                    }
                }
                if (!hasAllStages) {
                    return;
                }
            }

            // Check team funds (free entries cost 0)
            double cost = entry.free ? 0 : entry.buy;
            double teamBalance = teamManager.getTeamValuteBalance(teamKey, currencyId);
            if (teamBalance < cost) {
                return; // Insufficient funds
            }

            // Deduct money from team
            if (!teamManager.removeTeamValutes(teamKey, currencyId, cost)) {
                return; // Removal failed
            }

            // Create item from found entry (not from template)
            // This prevents duplication of NBT that don't exist in the shop
            ItemStack itemToCreate = net.unfamily.iskautils.shop.ItemConverter.parseItemString(entry.item, entry.amount);
            slot.setStackInSlot(0, itemToCreate);
            entity.setChanged();
        }
    }
} 
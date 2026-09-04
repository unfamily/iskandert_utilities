package net.unfamily.iskautils.block.entity;

import net.unfamily.iskautils.util.ModLogger;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.unfamily.iskautils.integration.mekanism.MekChemicalHelper;
import net.unfamily.iskautils.shop.ShopCurrency;
import net.unfamily.iskautils.shop.ShopEntry;
import net.unfamily.iskautils.shop.ShopEntryHelper;
import net.unfamily.iskautils.shop.ShopOtherRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.world.entity.player.Player;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;

/**
 * Block Entity for Auto Shop Block
 * Manages automatic item extraction via hopper and similar devices
 */
public class AutoShopBlockEntity extends BlockEntity {
    /** Tank capacity equals one shop unit ({@link ShopEntry#amount}); buy only when empty. */
    public static final int DEFAULT_TANK_MB = 1000;
    
    private static final ModLogger LOGGER = ModLogger.of(AutoShopBlockEntity.class);
    
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

    private final FluidTank fluidTank = new FluidTank(DEFAULT_TANK_MB, this::isFluidValidForSelection) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private Object gasTank;
    private String pendingGasId = "";
    private long pendingGasAmount;
    /** Fluid buffer capacity (independent from gas). */
    private int fluidCapacity = DEFAULT_TANK_MB;
    /** Gas buffer capacity (independent from fluid). */
    private int gasCapacity = DEFAULT_TANK_MB;
    private final ResizableEnergyStorage energyStorage = new ResizableEnergyStorage();
    
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
    
    @Override
    public void onDataPacket(net.minecraft.network.Connection net, net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider lookupProvider) {
        super.onDataPacket(net, pkt, lookupProvider);
        if (pkt.getTag() != null) {
            loadAdditional(pkt.getTag(), lookupProvider);
        }
    }
    
    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
    
    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
    
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        
        // Save encapsulated slot
        tag.put("encapsulatedSlot", encapsulatedSlot.serializeNBT(registries));
        
        // Save shop state (filter/selected item is in shopData.selectedItem)
        CompoundTag shopData = new CompoundTag();
        shopData.putBoolean("isActive", isActive);
        shopData.putString("currentCategory", currentCategory);
        
        // Always save the currency (even if it's "unset")
        shopData.putString("selectedValute", selectedValute);
        
        // Always save the mode (buy/sell)
        shopData.putBoolean("autoBuyMode", autoBuyMode);
        
        // Save owner team ID if present
        if (ownerTeamId != null) {
            shopData.putUUID("ownerTeamId", ownerTeamId);
        }
        
        // Save placedByPlayer only if not empty
        if (placedByPlayer != null) {
            shopData.putUUID("placedByPlayer", placedByPlayer);
        }
        
        // Save selectedItem only if not empty and valid
        if (!selectedItem.isEmpty() && selectedItem.getItem() != null) {
            CompoundTag selectedTag = new CompoundTag();
            selectedItem.save(registries, selectedTag);
            // Always save the item if valid, even if tag is empty (can happen for simple items)
            shopData.put("selectedItem", selectedTag);
        }
        if (selectedShopEntryId != null && !selectedShopEntryId.isEmpty()) {
            shopData.putString("selectedShopEntryId", selectedShopEntryId);
        }
        shopData.putString("selectedEntryType", selectedEntryType.name());
        shopData.putInt("redstoneMode", redstoneMode);
        shopData.putBoolean("previousRedstoneState", previousRedstoneState);
        
        tag.put("shopData", shopData);
        tag.putInt("fluidCapacity", fluidCapacity);
        tag.putInt("gasCapacity", gasCapacity);
        tag.putInt("energyStored", energyStorage.getEnergyStored());
        tag.putInt("energyCapacity", energyStorage.getMaxEnergyStored());
        tag.put("fluidTank", fluidTank.writeToNBT(registries, new CompoundTag()));
        String gasId = getGasId();
        long gasAmount = getGasAmount();
        if (!gasId.isEmpty() && gasAmount > 0) {
            tag.putString("gasId", gasId);
            tag.putLong("gasAmount", gasAmount);
        }
    }
    
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        
        // Load encapsulated slot
        if (tag.contains("encapsulatedSlot")) {
            encapsulatedSlot.deserializeNBT(registries, tag.getCompound("encapsulatedSlot"));
        }
        
        // Load shop state (filter slot is logical only, stored in shopData.selectedItem)
        if (tag.contains("shopData")) {
            CompoundTag shopData = tag.getCompound("shopData");
            this.isActive = shopData.getBoolean("isActive");
            this.currentCategory = shopData.getString("currentCategory");
            
            if (shopData.contains("selectedValute")) {
                this.selectedValute = normalizeCurrencyId(shopData.getString("selectedValute"));
            } else {
                this.selectedValute = resolveDefaultCurrencyId();
            }
            
            // Load mode if present
            if (shopData.contains("autoBuyMode")) {
                this.autoBuyMode = shopData.getBoolean("autoBuyMode");
            } else {
                this.autoBuyMode = true; // Default if not present
            }
            
            // Load owner team ID if present
            if (shopData.contains("ownerTeamId")) {
                this.ownerTeamId = shopData.getUUID("ownerTeamId");
            } else {
                this.ownerTeamId = null; // Default if not present
            }
            
            // Load placedByPlayer if present
            if (shopData.contains("placedByPlayer")) {
                this.placedByPlayer = shopData.getUUID("placedByPlayer");
            } else {
                this.placedByPlayer = null; // Default if not present
            }
            
            // Load selectedItem if present
            if (shopData.contains("selectedItem")) {
                try {
                    CompoundTag selectedTag = shopData.getCompound("selectedItem");
                    // Try to load the item even if tag is empty (can happen for simple items)
                    this.selectedItem = ItemStack.parse(registries, selectedTag).orElse(ItemStack.EMPTY);
                    // Verify that loaded item is valid
                    if (this.selectedItem.isEmpty() || this.selectedItem.getItem() == null) {
                        this.selectedItem = ItemStack.EMPTY;
                        LOGGER.warn("AutoShopBlockEntity: Invalid selectedItem loaded, resetting to EMPTY");
                    }
                } catch (Exception e) {
                    LOGGER.error("AutoShopBlockEntity: Error loading selectedItem", e);
                    this.selectedItem = ItemStack.EMPTY;
                }
            } else {
                this.selectedItem = ItemStack.EMPTY; // Default if not present
            }
            if (shopData.contains("selectedShopEntryId")) {
                this.selectedShopEntryId = shopData.getString("selectedShopEntryId");
            } else {
                this.selectedShopEntryId = "";
            }
            try {
                this.selectedEntryType = ShopEntry.EntryType.valueOf(
                        shopData.getString("selectedEntryType").isEmpty()
                                ? ShopEntry.EntryType.ITEM.name()
                                : shopData.getString("selectedEntryType"));
            } catch (IllegalArgumentException ignored) {
                this.selectedEntryType = ShopEntry.EntryType.ITEM;
            }
            if (shopData.contains("redstoneMode")) {
                this.redstoneMode = shopData.getInt("redstoneMode");
            }
            if (shopData.contains("previousRedstoneState")) {
                this.previousRedstoneState = shopData.getBoolean("previousRedstoneState");
            }
        }
        // Migration: if old save had physical selectedSlot NBT, copy first slot to logical selectedItem
        if (tag.contains("selectedSlot")) {
            try {
                var itemsList = tag.getCompound("selectedSlot").getList("Items", 10);
                if (itemsList.size() > 0) {
                    var oldSelected = ItemStack.parse(registries, itemsList.getCompound(0)).orElse(ItemStack.EMPTY);
                    if (!oldSelected.isEmpty() && this.selectedItem.isEmpty()) {
                        this.selectedItem = oldSelected.copy();
                        this.selectedItem.setCount(1);
                    }
                }
            } catch (Exception ignored) {}
        }
        int legacyCap = tag.contains("tankCapacity") ? Math.max(1, tag.getInt("tankCapacity")) : DEFAULT_TANK_MB;
        fluidCapacity = Math.max(1, tag.contains("fluidCapacity") ? tag.getInt("fluidCapacity") : legacyCap);
        gasCapacity = Math.max(1, tag.contains("gasCapacity") ? tag.getInt("gasCapacity") : legacyCap);
        fluidTank.setCapacity(fluidCapacity);
        if (tag.contains("fluidTank")) {
            fluidTank.readFromNBT(registries, tag.getCompound("fluidTank"));
        }
        pendingGasId = tag.getString("gasId");
        pendingGasAmount = Math.max(0L, tag.getLong("gasAmount"));
        energyStorage.resize(Math.max(0, tag.getInt("energyCapacity")));
        energyStorage.setEnergy(Math.max(0, tag.getInt("energyStored")));
        ensureGasTank();
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

    public IFluidHandler getFluidTransferHandler() {
        return fluidTank;
    }

    /** Mekanism {@code IChemicalHandler} for pipes / tubes (null if Mek absent). */
    @Nullable
    public Object getChemicalTransferHandler() {
        if (!MekChemicalHelper.isLoaded()) {
            return null;
        }
        ensureGasTank();
        return gasTank;
    }

    public int getFluidAmount() {
        return fluidTank.getFluidAmount();
    }

    public int getFluidCapacity() {
        return fluidTank.getCapacity();
    }

    public IEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    public int getEnergyStored() {
        return energyStorage.getEnergyStored();
    }

    public int getEnergyCapacity() {
        return energyStorage.getMaxEnergyStored();
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
        return gasTank != null ? MekChemicalHelper.getTankCapacityLong(gasTank) : gasCapacity;
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
        if (MekChemicalHelper.isRadioactiveInTank(gasTank)
                || MekChemicalHelper.isRadioactiveGasId(pendingGasId)) {
            return;
        }
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
        gasTank = MekChemicalHelper.createAllValidTank(gasCapacity);
        if (gasTank != null && pendingGasAmount > 0 && !pendingGasId.isEmpty()) {
            Object stack = MekChemicalHelper.createStackFromId(pendingGasId, pendingGasAmount);
            MekChemicalHelper.fill(gasTank, stack, false);
            pendingGasAmount = 0;
            pendingGasId = "";
        }
    }

    /** Resize only the buffer matching the selected shop entry type. Fluid and gas stay independent. */
    private void resizeForEntry(@Nullable ShopEntry entry) {
        if (entry == null) {
            resizeEnergy(0);
            return;
        }
        int amount = Math.max(1, entry.amount);
        switch (entry.type) {
            case FLUID -> {
                resizeFluidTank(amount);
                resizeEnergy(0);
            }
            case GAS -> {
                resizeGasTank(amount);
                resizeEnergy(0);
            }
            case ITEM -> resizeEnergy(0);
            case OTHER -> resizeEnergy(ShopOtherRegistry.isRf(entry.other) ? amount : 0);
        }
    }

    private void resizeEnergy(int capacity) {
        energyStorage.resize(Math.max(0, capacity));
    }

    private void resizeFluidTank(int entryAmount) {
        fluidCapacity = Math.max(1, entryAmount);
        fluidTank.setCapacity(fluidCapacity);
        if (fluidTank.getFluidAmount() > fluidCapacity) {
            fluidTank.setFluid(fluidTank.getFluid().copyWithAmount(fluidCapacity));
        }
    }

    private void resizeGasTank(int entryAmount) {
        gasCapacity = Math.max(1, entryAmount);
        if (!MekChemicalHelper.isLoaded()) {
            return;
        }
        // BasicChemicalTank capacity is fixed at creation — recreate while preserving contents.
        String keepId = "";
        long keepAmt = 0L;
        if (gasTank != null) {
            Object inTank = MekChemicalHelper.getChemicalInTank(gasTank, 0);
            if (!MekChemicalHelper.isEmpty(inTank)) {
                keepId = MekChemicalHelper.getRegistryName(inTank);
                keepAmt = MekChemicalHelper.getAmount(inTank);
            }
        }
        gasTank = MekChemicalHelper.createAllValidTank(gasCapacity);
        if (gasTank != null && keepId != null && !keepId.isEmpty() && keepAmt > 0) {
            Object restack = MekChemicalHelper.createStackFromId(keepId, Math.min(keepAmt, gasCapacity));
            MekChemicalHelper.fill(gasTank, restack, false);
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
        return new ArrayList<>(ShopCurrency.sortedIds(net.unfamily.iskautils.shop.ShopLoader.getCurrencies().values()));
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
        // Do not resize fluid/gas buffers when selecting an item — buffers are independent.
        resizeEnergy(0);
        
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
        resizeForEntry(entry);
        setChanged();
    }
    
    public boolean hasSelectedItem() {
        return !this.selectedItem.isEmpty();
    }
    
    public void clearSelectedItem() {
        this.selectedItem = ItemStack.EMPTY;
        this.selectedShopEntryId = "";
        this.selectedEntryType = ShopEntry.EntryType.ITEM;
        // Leave buffer contents/capacities intact; only clear the shop selection.
        resizeEnergy(0);
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
                net.unfamily.iskalib.team.ShopTeamManager.getInstance(player.serverLevel());
            
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
            case OTHER -> ShopOtherRegistry.isRf(entry.other)
                    && energyStorage.getEnergyStored() >= entry.amount;
            case ITEM -> false;
        };
    }

    private boolean extractTyped(ShopEntry entry) {
        return switch (entry.type) {
            case FLUID -> fluidTank.drain(entry.amount, IFluidHandler.FluidAction.EXECUTE).getAmount() == entry.amount;
            case GAS -> MekChemicalHelper.extractFromTank(gasTank, entry.amount) == entry.amount;
            case OTHER -> ShopOtherRegistry.isRf(entry.other)
                    && energyStorage.extractEnergy(entry.amount, false) == entry.amount;
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
            case OTHER -> ShopOtherRegistry.isRf(entry.other)
                    && energyStorage.getEnergyStored() == 0
                    && energyStorage.receiveEnergy(entry.amount, true) == entry.amount;
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
            case OTHER -> ShopOtherRegistry.isRf(entry.other)
                    && energyStorage.receiveEnergy(entry.amount, false) == entry.amount;
            case ITEM -> false;
        };
    }
    
    /**
     * Bucket / fluid container: fill AutoShop tank from item, or fill empty container from tank.
     * Colossal Resource Port pattern.
     */
    public boolean interactWithItemFluidHandler(IFluidHandlerItem itemHandler, Player player) {
        if (itemHandler == null || itemHandler.getTanks() == 0) {
            return false;
        }
        FluidStack inItem = itemHandler.getFluidInTank(0);
        if (!inItem.isEmpty()) {
            if (fluidTank.fill(inItem.copy(), IFluidHandler.FluidAction.SIMULATE) > 0) {
                int filled = fluidTank.fill(inItem.copy(), IFluidHandler.FluidAction.EXECUTE);
                if (filled > 0) {
                    itemHandler.drain(filled, IFluidHandler.FluidAction.EXECUTE);
                    inItem.getFluid().getPickupSound().ifPresent(player::playSound);
                    setChanged();
                    return true;
                }
            }
            return false;
        }
        FluidStack inBlock = fluidTank.getFluid();
        if (!inBlock.isEmpty() && itemHandler.isFluidValid(0, inBlock)) {
            int capacity = itemHandler.getTankCapacity(0);
            FluidStack toFill = inBlock.copy();
            toFill.setAmount(Math.min(inBlock.getAmount(), capacity));
            int filled = itemHandler.fill(toFill, IFluidHandler.FluidAction.EXECUTE);
            if (filled > 0) {
                fluidTank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
                var soundEvent = inBlock.getFluid().getFluidType()
                        .getSound(net.neoforged.neoforge.common.SoundActions.BUCKET_EMPTY);
                if (soundEvent != null) {
                    player.playSound(soundEvent);
                }
                setChanged();
                return true;
            }
        }
        return false;
    }

    /**
     * Chemical tanks / gas cells: same deposit/withdraw idea as fluid buckets when Mek is loaded.
     */
    public boolean interactWithItemChemicalHandler(ItemStack singleItem, Player player) {
        if (!MekChemicalHelper.isLoaded() || singleItem == null || singleItem.isEmpty()) {
            return false;
        }
        ensureGasTank();
        if (gasTank == null) {
            return false;
        }
        return MekChemicalHelper.transferBetweenItemAndTank(singleItem, gasTank, player);
    }

    /**
     * Manual emerald-button trade: ignores redstone shouldRun, only allowed while mode is DISABLED.
     * {@code quantity} uses the same multipliers as the player shop (1 / 4 / 16).
     * Items go to/from the opening player's inventory; fluids/gases use the machine tanks with qty capacity.
     */
    public boolean tryManualTrade(ServerPlayer player, int quantity) {
        if (level == null || level.isClientSide() || player == null) {
            return false;
        }
        if (RedstoneMode.fromValue(getRedstoneMode()) != RedstoneMode.DISABLED) {
            return false;
        }
        if (!canPlayerUse(player)) {
            return false;
        }
        int qty = Math.max(1, Math.min(quantity, 64));
        return runManualTrade(player, qty);
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

        entity.runTradeCycles(1);
    }

    private boolean runManualTrade(ServerPlayer player, int quantity) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return false;
        }
        net.unfamily.iskalib.team.ShopTeamManager teamManager =
                net.unfamily.iskalib.team.ShopTeamManager.getInstance(serverLevel);

        if (getOwnerTeamId() == null && getPlacedByPlayer() != null) {
            String placerTeamName = teamManager.getPlayerTeam(getPlacedByPlayer());
            if (placerTeamName != null) {
                java.util.UUID teamId = teamManager.getTeamIdByName(placerTeamName);
                if (teamId != null) {
                    setOwnerTeamId(teamId);
                }
            }
        }
        if (getOwnerTeamId() == null) {
            return false;
        }

        String teamKey = resolveTeamKey(teamManager, getOwnerTeamId());
        if (teamKey == null) {
            return false;
        }
        if (getPlacedByPlayer() != null) {
            String placerTeamKey = teamManager.getPlayerTeam(getPlacedByPlayer());
            if (placerTeamKey == null || !placerTeamKey.equals(teamKey)) {
                return false;
            }
        }

        String currencyId = normalizeCurrencyId(getSelectedValute());
        if (!currencyId.equals(getSelectedValute())) {
            setSelectedValute(currencyId);
        }

        ShopEntry entry = resolveManualEntry();
        if (entry == null) {
            return false;
        }
        String entryCurrency = entry.valute != null ? entry.valute : "null_coin";
        if (!entryCurrency.equals(currencyId)) {
            return false;
        }

        net.unfamily.iskalib.stage.StageRegistry registry =
                net.unfamily.iskalib.stage.StageRegistry.getInstance(serverLevel.getServer());
        if (!stagesMet(entry, registry, player, teamKey)) {
            return false;
        }

        if (entry.type == ShopEntry.EntryType.ITEM) {
            String entryId = selectedShopEntryId != null && !selectedShopEntryId.isEmpty()
                    ? selectedShopEntryId
                    : entry.id;
            if (entryId == null || entryId.isEmpty()) {
                return false;
            }
            if (isAutoBuyMode()) {
                if (ShopEntryHelper.isTagEntry(entry) || !ShopEntryHelper.isBuyAllowed(entry)) {
                    return false;
                }
                return net.unfamily.iskautils.shop.ShopTransactionManager.buyItem(player, entryId, quantity);
            }
            if (!ShopEntryHelper.isSellAllowed(entry)) {
                return false;
            }
            return net.unfamily.iskautils.shop.ShopTransactionManager.sellItem(player, entryId, quantity);
        }

        if (isAutoBuyMode()) {
            return processManualTypedBuy(entry, teamManager, teamKey, currencyId, quantity);
        }
        return processManualTypedSell(entry, teamManager, teamKey, currencyId, quantity);
    }

    @Nullable
    private ShopEntry resolveManualEntry() {
        ShopEntry bound = getBoundEntry();
        if (bound != null) {
            return bound;
        }
        ItemStack selected = getSelectedItem();
        if (!selected.isEmpty()) {
            return findEntryForItemExact(selected, "");
        }
        return null;
    }

    private boolean processManualTypedBuy(ShopEntry entry,
                                          net.unfamily.iskalib.team.ShopTeamManager teamManager,
                                          String teamKey, String currencyId, int quantity) {
        if (!ShopEntryHelper.isBuyAllowed(entry) || entry.amount <= 0 || quantity <= 0) {
            return false;
        }
        int unit = entry.amount;
        int totalAmount = unit * quantity;
        double cost = entry.free ? 0 : entry.buy * quantity;

        if (entry.type == ShopEntry.EntryType.FLUID) {
            if (!fluidTank.isEmpty()) {
                return false;
            }
            var fluid = ShopEntryHelper.resolveFluid(entry.fluid);
            if (fluid == null) {
                return false;
            }
            if (cost > 0 && (teamManager.getTeamValuteBalance(teamKey, currencyId) < cost
                    || !teamManager.removeTeamValutes(teamKey, currencyId, cost))) {
                return false;
            }
            resizeFluidTank(totalAmount);
            if (fluidTank.fill(new FluidStack(fluid, totalAmount), IFluidHandler.FluidAction.EXECUTE) != totalAmount) {
                if (cost > 0) {
                    teamManager.addTeamValutes(teamKey, currencyId, cost);
                }
                resizeFluidTank(unit);
                return false;
            }
            setChanged();
            return true;
        }

        if (entry.type == ShopEntry.EntryType.GAS) {
            ensureGasTank();
            if (gasTank == null || getGasAmount() > 0) {
                return false;
            }
            Object stack = MekChemicalHelper.createStackFromId(entry.gas, totalAmount);
            if (stack == null) {
                return false;
            }
            if (cost > 0 && (teamManager.getTeamValuteBalance(teamKey, currencyId) < cost
                    || !teamManager.removeTeamValutes(teamKey, currencyId, cost))) {
                return false;
            }
            resizeGasTank(totalAmount);
            if (MekChemicalHelper.fill(gasTank, stack, false) != totalAmount) {
                if (cost > 0) {
                    teamManager.addTeamValutes(teamKey, currencyId, cost);
                }
                resizeGasTank(unit);
                return false;
            }
            setChanged();
            return true;
        }
        if (entry.type == ShopEntry.EntryType.OTHER && ShopOtherRegistry.isRf(entry.other)) {
            if (energyStorage.getEnergyStored() > 0) {
                return false;
            }
            if (cost > 0 && (teamManager.getTeamValuteBalance(teamKey, currencyId) < cost
                    || !teamManager.removeTeamValutes(teamKey, currencyId, cost))) {
                return false;
            }
            resizeEnergy(totalAmount);
            if (energyStorage.receiveEnergy(totalAmount, false) != totalAmount) {
                if (cost > 0) {
                    teamManager.addTeamValutes(teamKey, currencyId, cost);
                }
                resizeEnergy(unit);
                return false;
            }
            setChanged();
            return true;
        }
        return false;
    }

    private boolean processManualTypedSell(ShopEntry entry,
                                           net.unfamily.iskalib.team.ShopTeamManager teamManager,
                                           String teamKey, String currencyId, int quantity) {
        if (!ShopEntryHelper.isSellAllowed(entry) || entry.amount <= 0 || quantity <= 0) {
            return false;
        }
        int unit = entry.amount;

        if (entry.type == ShopEntry.EntryType.FLUID) {
            if (!ShopEntryHelper.matchesFluid(fluidTank.getFluid(), entry.fluid)) {
                return false;
            }
            int availableUnits = fluidTank.getFluidAmount() / unit;
            int units = Math.min(quantity, availableUnits);
            if (units <= 0) {
                return false;
            }
            int extract = units * unit;
            if (fluidTank.drain(extract, IFluidHandler.FluidAction.EXECUTE).getAmount() != extract) {
                return false;
            }
            teamManager.addTeamValutes(teamKey, currencyId, entry.sell * units);
            setChanged();
            return true;
        }

        if (entry.type == ShopEntry.EntryType.GAS) {
            ensureGasTank();
            Object inTank = MekChemicalHelper.getChemicalInTank(gasTank, 0);
            if (!ShopEntryHelper.matchesGas(inTank, entry.gas)) {
                return false;
            }
            long availableUnits = getGasAmount() / unit;
            int units = (int) Math.min(quantity, availableUnits);
            if (units <= 0) {
                return false;
            }
            int extract = units * unit;
            if (MekChemicalHelper.extractFromTank(gasTank, extract) != extract) {
                return false;
            }
            teamManager.addTeamValutes(teamKey, currencyId, entry.sell * units);
            setChanged();
            return true;
        }
        if (entry.type == ShopEntry.EntryType.OTHER && ShopOtherRegistry.isRf(entry.other)) {
            int availableUnits = energyStorage.getEnergyStored() / unit;
            int units = Math.min(quantity, availableUnits);
            if (units <= 0) {
                return false;
            }
            int extract = units * unit;
            if (energyStorage.extractEnergy(extract, false) != extract) {
                return false;
            }
            teamManager.addTeamValutes(teamKey, currencyId, entry.sell * units);
            setChanged();
            return true;
        }
        return false;
    }

    private boolean runTradeCycles(int quantity) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return false;
        }
        net.unfamily.iskalib.team.ShopTeamManager teamManager =
                net.unfamily.iskalib.team.ShopTeamManager.getInstance(serverLevel);

        if (getOwnerTeamId() == null && getPlacedByPlayer() != null) {
            String placerTeamName = teamManager.getPlayerTeam(getPlacedByPlayer());
            if (placerTeamName != null) {
                java.util.UUID teamId = teamManager.getTeamIdByName(placerTeamName);
                if (teamId != null) {
                    setOwnerTeamId(teamId);
                }
            }
        }
        if (getOwnerTeamId() == null) {
            return false;
        }

        String teamKey = resolveTeamKey(teamManager, getOwnerTeamId());
        if (teamKey == null) {
            return false;
        }
        if (getPlacedByPlayer() != null) {
            String placerTeamKey = teamManager.getPlayerTeam(getPlacedByPlayer());
            if (placerTeamKey == null || !placerTeamKey.equals(teamKey)) {
                return false;
            }
        }

        ServerPlayer placerPlayer = serverLevel.getServer().getPlayerList().getPlayer(getPlacedByPlayer());
        String currencyId = normalizeCurrencyId(getSelectedValute());
        if (!currencyId.equals(getSelectedValute())) {
            setSelectedValute(currencyId);
        }

        boolean any = false;
        for (int i = 0; i < quantity; i++) {
            if (!processOneTrade(teamManager, teamKey, currencyId, placerPlayer)) {
                break;
            }
            any = true;
        }
        return any;
    }

    private boolean processOneTrade(net.unfamily.iskalib.team.ShopTeamManager teamManager, String teamKey,
                                    String currencyId, @Nullable ServerPlayer placerPlayer) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return false;
        }
        ShopEntry boundEntry = getBoundEntry();
        if (boundEntry != null && boundEntry.type != ShopEntry.EntryType.ITEM) {
            net.unfamily.iskalib.stage.StageRegistry registry =
                    net.unfamily.iskalib.stage.StageRegistry.getInstance(serverLevel.getServer());
            int beforeFluid = fluidTank.getFluidAmount();
            long beforeGas = getGasAmount();
            int beforeEnergy = energyStorage.getEnergyStored();
            processTypedEntry(this, boundEntry, teamManager, teamKey, currencyId, registry, placerPlayer);
            return fluidTank.getFluidAmount() != beforeFluid || getGasAmount() != beforeGas
                    || energyStorage.getEnergyStored() != beforeEnergy;
        }

        if (!isAutoBuyMode()) {
            ItemStackHandler slot = getEncapsulatedSlot();
            ItemStack stack = slot.getStackInSlot(0);
            if (stack.isEmpty()) {
                return false;
            }

            ItemStack filterItem = getSelectedItem();
            ShopEntry selectedEntry = getBoundEntry();
            String itemSelector = selectedEntry != null ? selectedEntry.item : null;
            if (itemSelector != null
                    ? !ShopEntryHelper.matchesItem(stack, itemSelector)
                    : (!filterItem.isEmpty() && !ItemStack.isSameItemSameComponents(stack, filterItem))) {
                return false;
            }

            ShopEntry entry = findEntryForItemExact(
                    filterItem.isEmpty() ? stack : filterItem, getSelectedShopEntryId());
            if (entry == null || entry.sell <= 0) {
                return false;
            }
            String entryCurrency = entry.valute != null ? entry.valute : "null_coin";
            if (!entryCurrency.equals(currencyId)) {
                return false;
            }
            net.unfamily.iskalib.stage.StageRegistry registry =
                    net.unfamily.iskalib.stage.StageRegistry.getInstance(serverLevel.getServer());
            if (!stagesMet(entry, registry, placerPlayer, teamKey)) {
                return false;
            }
            if (stack.getCount() < entry.amount) {
                return false;
            }
            ItemStack removed = slot.extractItem(0, entry.amount, false);
            if (removed.isEmpty()) {
                return false;
            }
            teamManager.addTeamValutes(teamKey, currencyId, entry.sell);
            setChanged();
            return true;
        }

        ItemStackHandler slot = getEncapsulatedSlot();
        ItemStack stack = slot.getStackInSlot(0);
        if (!stack.isEmpty()) {
            return false;
        }
        ItemStack selectedStack = getSelectedItem();
        if (selectedStack.isEmpty()) {
            return false;
        }
        ShopEntry entry = findEntryForItemExact(selectedStack, getSelectedShopEntryId());
        if (entry == null || (entry.buy <= 0 && !entry.free)) {
            return false;
        }
        String entryCurrency = entry.valute != null ? entry.valute : "null_coin";
        if (!entryCurrency.equals(currencyId)) {
            return false;
        }
        net.unfamily.iskalib.stage.StageRegistry registry =
                net.unfamily.iskalib.stage.StageRegistry.getInstance(serverLevel.getServer());
        if (!stagesMet(entry, registry, placerPlayer, teamKey)) {
            return false;
        }
        double cost = entry.free ? 0 : entry.buy;
        if (teamManager.getTeamValuteBalance(teamKey, currencyId) < cost
                || !teamManager.removeTeamValutes(teamKey, currencyId, cost)) {
            return false;
        }
        ItemStack itemToCreate = net.unfamily.iskalib.item.ItemConverter.parseItemString(entry.item, entry.amount);
        if (itemToCreate.isEmpty()) {
            if (cost > 0) {
                teamManager.addTeamValutes(teamKey, currencyId, cost);
            }
            return false;
        }
        slot.setStackInSlot(0, itemToCreate);
        setChanged();
        return true;
    }

    private final class ResizableEnergyStorage extends EnergyStorage {
        private ResizableEnergyStorage() {
            super(0, Integer.MAX_VALUE, Integer.MAX_VALUE);
        }

        private void resize(int newCapacity) {
            this.capacity = Math.max(0, newCapacity);
            this.energy = Math.min(this.energy, this.capacity);
        }

        private void setEnergy(int amount) {
            this.energy = Math.max(0, Math.min(this.capacity, amount));
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = super.receiveEnergy(maxReceive, simulate);
            if (!simulate && received > 0) {
                AutoShopBlockEntity.this.setChanged();
            }
            return received;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int extracted = super.extractEnergy(maxExtract, simulate);
            if (!simulate && extracted > 0) {
                AutoShopBlockEntity.this.setChanged();
            }
            return extracted;
        }
    }
} 
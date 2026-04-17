package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.unfamily.iskalib.transfer.LegacyItemHandlerResourceHandler;
import org.jetbrains.annotations.NotNull;
import net.minecraft.world.item.ItemStack;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.world.entity.player.Player;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;

/**
 * Block Entity for Auto Shop Block
 * Manages automatic item extraction via hopper and similar devices
 */
public class AutoShopBlockEntity extends BlockEntity {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AutoShopBlockEntity.class);
    
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
    private String selectedValute = "unset"; // Selected currency, default to "unset"
    private UUID ownerTeamId = null; // Team ID of the player who placed the AutoShop
    private UUID placedByPlayer = null; // UUID of the player who placed the Auto Shop
    private ItemStack selectedItem = ItemStack.EMPTY; // Selected item for encapsulated slot
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
    }
    
    public AutoShopBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.AUTO_SHOP_BE.get(), pos, blockState);
    }
    
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
    }
    
    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        this.isActive = input.getBooleanOr("isActive", false);
        this.currentCategory = input.getStringOr("currentCategory", "000_default");

        this.selectedValute = input.getStringOr("selectedValute", "unset");
        if (this.selectedValute.isEmpty()) this.selectedValute = "unset";

        this.autoBuyMode = input.getBooleanOr("autoBuyMode", true);

        String ownerTeamStr = input.getStringOr("ownerTeamId", "");
        this.ownerTeamId = ownerTeamStr.isEmpty() ? null : UUID.fromString(ownerTeamStr);

        String placedByStr = input.getStringOr("placedByPlayer", "");
        this.placedByPlayer = placedByStr.isEmpty() ? null : UUID.fromString(placedByStr);

        this.redstoneMode = input.getIntOr("redstoneMode", 0);
        this.previousRedstoneState = input.getBooleanOr("previousRedstoneState", false);
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

    /** Returns the read-only filter display handler (ghost slot: display only, no put/take). */
    public IItemHandler getFilterDisplayHandler() {
        return filterDisplayHandler;
    }
    
    public String getSelectedValute() {
        return this.selectedValute;
    }
    
    public void setSelectedValute(String valute) {
        this.selectedValute = valute != null ? valute : "unset";
        setChanged();
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
        

        
        setChanged();
    }
    
    public boolean hasSelectedItem() {
        return !this.selectedItem.isEmpty();
    }
    
    public void clearSelectedItem() {
        this.selectedItem = ItemStack.EMPTY;
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
     * Searches for a ShopEntry for a specific item by directly comparing ItemStacks
     */
    private static net.unfamily.iskautils.shop.ShopEntry findEntryForItem(ItemStack templateItem) {
        Map<String, net.unfamily.iskautils.shop.ShopEntry> allEntries = net.unfamily.iskautils.shop.ShopLoader.getEntries();
        
        // First look for exact match (same item with same NBT)
        for (Map.Entry<String, net.unfamily.iskautils.shop.ShopEntry> entryMap : allEntries.entrySet()) {
            net.unfamily.iskautils.shop.ShopEntry entry = entryMap.getValue();
            
            // Convert entry to ItemStack for comparison
            ItemStack entryItem = net.unfamily.iskautils.shop.ItemConverter.parseItemString(entry.item, 1);
            if (!entryItem.isEmpty() && ItemStack.isSameItemSameComponents(templateItem, entryItem)) {
                return entry; // Exact match found
            }
        }
        
        // If no exact match found, search by item type (without NBT)
        for (Map.Entry<String, net.unfamily.iskautils.shop.ShopEntry> entryMap : allEntries.entrySet()) {
            net.unfamily.iskautils.shop.ShopEntry entry = entryMap.getValue();
            
            // Convert entry to ItemStack for comparison
            ItemStack entryItem = net.unfamily.iskautils.shop.ItemConverter.parseItemString(entry.item, 1);
            if (!entryItem.isEmpty() && templateItem.is(entryItem.getItem())) {
                return entry; // Type match found
            }
        }
        
        return null; // Entry not found
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

        // Get the owner's team (needed for both modes)
        if (entity.getOwnerTeamId() == null) {
            return;
        }
        net.minecraft.server.level.ServerLevel serverLevel = (net.minecraft.server.level.ServerLevel) level;
        net.unfamily.iskalib.team.ShopTeamManager teamManager = net.unfamily.iskalib.team.ShopTeamManager.getInstance(serverLevel);
        
        // Get team using saved ID
        net.unfamily.iskalib.team.ShopTeamManager.Team team = teamManager.getTeamById(entity.getOwnerTeamId());
        if (team == null) {
            return; // Team no longer exists
        }
        
        String teamName = team.getName();

        // Check that the player who placed the AutoShop is still in the team
        if (entity.getPlacedByPlayer() != null) {
            String placerTeamName = teamManager.getPlayerTeam(entity.getPlacedByPlayer());
            if (placerTeamName == null || !placerTeamName.equals(teamName)) {
                return; // The placer is no longer in the team, block the AutoShop
            }
        }

        // Retrieve the placer's ServerPlayer (if online) - needed for player stage
        net.minecraft.server.level.ServerPlayer placerPlayer = serverLevel.getServer().getPlayerList().getPlayer(entity.getPlacedByPlayer());

        // Check that there's a valid selected currency
        String currencyId = entity.getSelectedValute();
        if (currencyId == null || currencyId.equals("unset")) {
            return;
        }

        // SELL mode
        if (!entity.isAutoBuyMode()) {
            // Check if there's an item in the encapsulated slot
            ItemStackHandler slot = entity.getEncapsulatedSlot();
            ItemStack stack = slot.getStackInSlot(0);
            if (stack.isEmpty()) {
                return;
            }

            // Determine which template to use for ShopEntry search
            ItemStack filterItem = entity.getSelectedItem();
            ItemStack templateItem = !filterItem.isEmpty() ? filterItem : stack;

            // Find ShopEntry using the most appropriate template
            net.unfamily.iskautils.shop.ShopEntry entry = findEntryForItem(templateItem);
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
                        boolean hasTeamStage = registry.hasTeamStage(teamName, stage.stage);
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
            if (stack.getCount() < entry.itemCount) {
                return;
            }

            // Remove correct count from slot
            ItemStack removed = slot.extractItem(0, entry.itemCount, false);
            if (removed.isEmpty()) {
                return;
            }

            // Credit money to team (only single value)
            teamManager.addTeamValutes(teamName, currencyId, entry.sell);
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

            // Find ShopEntry for selected filter item
            ItemStack itemId = selectedStack;
            net.unfamily.iskautils.shop.ShopEntry entry = findEntryForItem(itemId);
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
                        boolean hasTeamStage = registry.hasTeamStage(teamName, stage.stage);
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
            double teamBalance = teamManager.getTeamValuteBalance(teamName, currencyId);
            if (teamBalance < cost) {
                return; // Insufficient funds
            }

            // Deduct money from team
            if (!teamManager.removeTeamValutes(teamName, currencyId, cost)) {
                return; // Removal failed
            }

            // Create item from found entry (not from template)
            // This prevents duplication of NBT that don't exist in the shop
            ItemStack itemToCreate = net.unfamily.iskautils.shop.ItemConverter.parseItemString(entry.item, entry.itemCount);
            slot.setStackInSlot(0, itemToCreate);
            entity.setChanged();
        }
    }
} 
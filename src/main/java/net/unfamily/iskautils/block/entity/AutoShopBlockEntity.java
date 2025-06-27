package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
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
    
    // Slot for selected item (for auto buy/sell)
    private final ItemStackHandler selectedSlot = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return true;
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
        
        // Save selected slot
        tag.put("selectedSlot", selectedSlot.serializeNBT(registries));
        
        // Save shop state
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
        
        tag.put("shopData", shopData);
    }
    
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        
        // Load encapsulated slot
        if (tag.contains("encapsulatedSlot")) {
            encapsulatedSlot.deserializeNBT(registries, tag.getCompound("encapsulatedSlot"));
        }
        
        // Load selected slot
        if (tag.contains("selectedSlot")) {
            selectedSlot.deserializeNBT(registries, tag.getCompound("selectedSlot"));
        }
        
        // Load shop state
        if (tag.contains("shopData")) {
            CompoundTag shopData = tag.getCompound("shopData");
            this.isActive = shopData.getBoolean("isActive");
            this.currentCategory = shopData.getString("currentCategory");
            
            // Load currency if present, otherwise use "unset"
            if (shopData.contains("selectedValute")) {
                this.selectedValute = shopData.getString("selectedValute");
                if (this.selectedValute.isEmpty()) {
                    this.selectedValute = "unset"; // Fallback if empty
                }
            } else {
                this.selectedValute = "unset"; // Default if not present
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
        }
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
    
    public ItemStackHandler getSelectedSlot() {
        return selectedSlot;
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
                return net.unfamily.iskautils.shop.ShopTeamManager.getInstance(serverLevel)
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
            net.unfamily.iskautils.shop.ShopTeamManager teamManager = 
                net.unfamily.iskautils.shop.ShopTeamManager.getInstance(player.serverLevel());
            
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

        // Get the owner's team (needed for both modes)
        if (entity.getOwnerTeamId() == null) {
            return;
        }
        net.minecraft.server.level.ServerLevel serverLevel = (net.minecraft.server.level.ServerLevel) level;
        net.unfamily.iskautils.shop.ShopTeamManager teamManager = net.unfamily.iskautils.shop.ShopTeamManager.getInstance(serverLevel);
        
        // Get team using saved ID
        net.unfamily.iskautils.shop.ShopTeamManager.Team team = teamManager.getTeamById(entity.getOwnerTeamId());
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
            // If there's a template in the selected slot, use it, otherwise use the item itself
            ItemStackHandler selectedSlot = entity.getSelectedSlot();
            ItemStack templateStack = selectedSlot.getStackInSlot(0);
            ItemStack templateItem;
            
            if (!templateStack.isEmpty()) {
                // Use template from selected slot
                templateItem = templateStack;
            } else {
                // Use item from encapsulated slot as template
                templateItem = stack;
            }

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
            net.unfamily.iskautils.stage.StageRegistry registry = net.unfamily.iskautils.stage.StageRegistry.getInstance(serverLevel.getServer());
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

            // Check if there's an item in the selected slot
            ItemStackHandler selectedSlot = entity.getSelectedSlot();
            ItemStack selectedStack = selectedSlot.getStackInSlot(0);
            if (selectedStack.isEmpty()) {
                return;
            }

            // Find ShopEntry for selected item - use complete item representation
            ItemStack itemId = selectedStack; // Include NBT/data components
            net.unfamily.iskautils.shop.ShopEntry entry = findEntryForItem(itemId);
            if (entry == null || entry.buy <= 0) {
                return;
            }

            // Check that currency matches
            String entryCurrency = entry.valute != null ? entry.valute : "null_coin";
            if (!entryCurrency.equals(currencyId)) {
                return;
            }

            // Check required stages
            net.unfamily.iskautils.stage.StageRegistry registry = net.unfamily.iskautils.stage.StageRegistry.getInstance(serverLevel.getServer());
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

            // Check team funds
            double teamBalance = teamManager.getTeamValuteBalance(teamName, currencyId);
            if (teamBalance < entry.buy) {
                return; // Insufficient funds
            }

            // Deduct money from team
            if (!teamManager.removeTeamValutes(teamName, currencyId, entry.buy)) {
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
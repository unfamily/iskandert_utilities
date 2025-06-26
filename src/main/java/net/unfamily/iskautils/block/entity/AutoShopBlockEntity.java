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

/**
 * Block Entity per l'Auto Shop Block
 * Gestisce l'estrazione automatica di item tramite hopper e dispositivi simili
 */
public class AutoShopBlockEntity extends BlockEntity {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AutoShopBlockEntity.class);
    
    // Slot custom per la funzione encapsulated (1 slot) - esposto per l'estrazione automatica
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
            // Permetti sempre l'estrazione
            return super.extractItem(slot, amount, simulate);
        }
    };
    
    // Slot per l'item selezionato (per auto compra/vendi)
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
    
    // Stato del negozio (semplificato)
    private boolean isActive = false;
    private String currentCategory = "000_default";
    private String selectedValute = "unset"; // Valuta selezionata, default a "unset"
    private UUID ownerTeamId = null; // ID del team del player che ha piazzato l'AutoShop
    private UUID placedByPlayer = null; // UUID del giocatore che ha piazzato l'Auto Shop
    private ItemStack selectedItem = ItemStack.EMPTY; // Item selezionato per la slot encapsulata
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
        
        // Salva encapsulated slot
        tag.put("encapsulatedSlot", encapsulatedSlot.serializeNBT(registries));
        
        // Salva selected slot
        tag.put("selectedSlot", selectedSlot.serializeNBT(registries));
        
        // Salva stato del negozio
        CompoundTag shopData = new CompoundTag();
        shopData.putBoolean("isActive", isActive);
        shopData.putString("currentCategory", currentCategory);
        
        // Always save the currency (even if it's "unset")
        shopData.putString("selectedValute", selectedValute);
        
        // Always save the mode (buy/sell)
        shopData.putBoolean("autoBuyMode", autoBuyMode);
        
        // Salva l'ID del team del proprietario se presente
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
        
        // Carica encapsulated slot
        if (tag.contains("encapsulatedSlot")) {
            encapsulatedSlot.deserializeNBT(registries, tag.getCompound("encapsulatedSlot"));
        }
        
        // Carica selected slot
        if (tag.contains("selectedSlot")) {
            selectedSlot.deserializeNBT(registries, tag.getCompound("selectedSlot"));
        }
        
        // Carica stato del negozio
        if (tag.contains("shopData")) {
            CompoundTag shopData = tag.getCompound("shopData");
            this.isActive = shopData.getBoolean("isActive");
            this.currentCategory = shopData.getString("currentCategory");
            
            // Carica la valuta se presente, altrimenti usa "unset"
            if (shopData.contains("selectedValute")) {
                this.selectedValute = shopData.getString("selectedValute");
                if (this.selectedValute.isEmpty()) {
                    this.selectedValute = "unset"; // Fallback se vuoto
                }
            } else {
                this.selectedValute = "unset"; // Default se non presente
            }
            
            // Load mode if present
            if (shopData.contains("autoBuyMode")) {
                this.autoBuyMode = shopData.getBoolean("autoBuyMode");
            } else {
                this.autoBuyMode = true; // Default se non presente
            }
            
            // Carica l'ID del team del proprietario se presente
            if (shopData.contains("ownerTeamId")) {
                this.ownerTeamId = shopData.getUUID("ownerTeamId");
            } else {
                this.ownerTeamId = null; // Default se non presente
            }
            
            // Carica il placedByPlayer se presente
            if (shopData.contains("placedByPlayer")) {
                this.placedByPlayer = shopData.getUUID("placedByPlayer");
            } else {
                this.placedByPlayer = null; // Default se non presente
            }
            
            // Carica il selectedItem se presente
            if (shopData.contains("selectedItem")) {
                try {
                    CompoundTag selectedTag = shopData.getCompound("selectedItem");
                    // Try to load the item even if tag is empty (can happen for simple items)
                    this.selectedItem = ItemStack.parse(registries, selectedTag).orElse(ItemStack.EMPTY);
                    // Verifica che l'item caricato sia valido
                    if (this.selectedItem.isEmpty() || this.selectedItem.getItem() == null) {
                        this.selectedItem = ItemStack.EMPTY;
                        LOGGER.warn("AutoShopBlockEntity: Invalid selectedItem loaded, resetting to EMPTY");
                    }
                } catch (Exception e) {
                    LOGGER.error("AutoShopBlockEntity: Error loading selectedItem", e);
                    this.selectedItem = ItemStack.EMPTY;
                }
            } else {

                this.selectedItem = ItemStack.EMPTY; // Default se non presente
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
    
    // Metodi per accesso ai dati
    
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
            // Crea una copia dell'item con count 1, preservando i NBT
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
     * Cerca una ShopEntry per un item specifico confrontando direttamente gli ItemStack
     */
    private static net.unfamily.iskautils.shop.ShopEntry findEntryForItem(ItemStack templateItem) {
        Map<String, net.unfamily.iskautils.shop.ShopEntry> allEntries = net.unfamily.iskautils.shop.ShopLoader.getEntries();
        
        // Prima cerca un match esatto (stesso item con stessi NBT)
        for (Map.Entry<String, net.unfamily.iskautils.shop.ShopEntry> entryMap : allEntries.entrySet()) {
            net.unfamily.iskautils.shop.ShopEntry entry = entryMap.getValue();
            
            // Converti l'entry in ItemStack per il confronto
            ItemStack entryItem = net.unfamily.iskautils.shop.ItemConverter.parseItemString(entry.item, 1);
            if (!entryItem.isEmpty() && ItemStack.isSameItemSameComponents(templateItem, entryItem)) {
                return entry; // Match esatto trovato
            }
        }
        
        // Se non trova match esatto, cerca per tipo di item (senza NBT)
        for (Map.Entry<String, net.unfamily.iskautils.shop.ShopEntry> entryMap : allEntries.entrySet()) {
            net.unfamily.iskautils.shop.ShopEntry entry = entryMap.getValue();
            
            // Converti l'entry in ItemStack per il confronto
            ItemStack entryItem = net.unfamily.iskautils.shop.ItemConverter.parseItemString(entry.item, 1);
            if (!entryItem.isEmpty() && templateItem.is(entryItem.getItem())) {
                return entry; // Match per tipo trovato
            }
        }
        
        return null; // Entry non trovata
    }
    
    /**
     * Tick del blocco (chiamato dal server)
     */
    public static void tick(Level level, BlockPos pos, BlockState state, AutoShopBlockEntity entity) {
        if (level.isClientSide()) {
            return;
        }

        // Get the owner's team (needed for both modes)
        if (entity.getPlacedByPlayer() == null) {
            return;
        }
        net.minecraft.server.level.ServerLevel serverLevel = (net.minecraft.server.level.ServerLevel) level;
        net.unfamily.iskautils.shop.ShopTeamManager teamManager = net.unfamily.iskautils.shop.ShopTeamManager.getInstance(serverLevel);
        String teamName = teamManager.getPlayerTeam(entity.getPlacedByPlayer());
        if (teamName == null) {
            return;
        }

        // Recupera il ServerPlayer del piazzatore (se online) - necessario per stage player
        net.minecraft.server.level.ServerPlayer placerPlayer = serverLevel.getServer().getPlayerList().getPlayer(entity.getPlacedByPlayer());

        // Controlla che ci sia una valuta selezionata valida
        String valuteId = entity.getSelectedValute();
        if (valuteId == null || valuteId.equals("unset")) {
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

            // Determina quale template usare per la ricerca della ShopEntry
            // If there's a template in the selected slot, use it, otherwise use the item itself
            ItemStackHandler selectedSlot = entity.getSelectedSlot();
            ItemStack templateStack = selectedSlot.getStackInSlot(0);
            ItemStack templateItem;
            
            if (!templateStack.isEmpty()) {
                // Usa il template dalla selected slot
                templateItem = templateStack;
            } else {
                // Usa l'item dalla encapsulated slot come template
                templateItem = stack;
            }

            // Find ShopEntry using the most appropriate template
            net.unfamily.iskautils.shop.ShopEntry entry = findEntryForItem(templateItem);
            if (entry == null || entry.sell <= 0) {
                return;
            }

            // Controlla che la valuta corrisponda
            String entryValute = entry.valute != null ? entry.valute : "null_coin";
            if (!entryValute.equals(valuteId)) {
                return;
            }

            // Controlla gli stage richiesti
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

            // Controlla che ci siano abbastanza item nella slot
            if (stack.getCount() < entry.itemCount) {
                return;
            }

            // Rimuovi il count corretto dalla slot
            ItemStack removed = slot.extractItem(0, entry.itemCount, false);
            if (removed.isEmpty()) {
                return;
            }

            // Accredita i soldi al team (solo il valore singolo)
            teamManager.addTeamValutes(teamName, valuteId, entry.sell);
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

            // Trova la ShopEntry per l'item selezionato - usa la rappresentazione completa dell'item
            ItemStack itemId = selectedStack; // Include NBT/data components
            net.unfamily.iskautils.shop.ShopEntry entry = findEntryForItem(itemId);
            if (entry == null || entry.buy <= 0) {
                return;
            }

            // Controlla che la valuta corrisponda
            String entryValute = entry.valute != null ? entry.valute : "null_coin";
            if (!entryValute.equals(valuteId)) {
                return;
            }

            // Controlla gli stage richiesti
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

            // Controlla i fondi del team
            double teamBalance = teamManager.getTeamValuteBalance(teamName, valuteId);
            if (teamBalance < entry.buy) {
                return; // Fondi insufficienti
            }

            // Scala i soldi dal team
            if (!teamManager.removeTeamValutes(teamName, valuteId, entry.buy)) {
                return; // Fallimento nella rimozione
            }

            // Crea l'item dalla entry trovata (non dal template)
            // Questo previene la duplicazione di NBT che non esistono nello shop
            ItemStack itemToCreate = net.unfamily.iskautils.shop.ItemConverter.parseItemString(entry.item, entry.itemCount);
            slot.setStackInSlot(0, itemToCreate);
            entity.setChanged();
        }
    }
} 
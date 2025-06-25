package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.world.item.ItemStack;

/**
 * Block Entity per l'Auto Shop Block
 * Gestisce l'interfaccia del negozio automatico e le transazioni
 */
public class AutoShopBlockEntity extends BlockEntity implements MenuProvider {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AutoShopBlockEntity.class);
    
    // Item storage per gli slot del negozio (visualizzazione e transazioni)
    private final ItemStackHandler itemHandler = new ItemStackHandler(36) { // 4 righe x 9 slot
        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            setChanged();
        }
        
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return true;
        }
    };
    
    // Slot custom per la funzione encapsulated (1 slot)
    private final ItemStackHandler encapsulatedSlot = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    
    // Stato del negozio (semplificato)
    private boolean isActive = false;
    private String currentCategory = "000_default";
    private String selectedValute = "unset"; // Valuta selezionata, default a "unset"
    
    public AutoShopBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.AUTO_SHOP_BE.get(), pos, blockState);
    }
    
    @Override
    public Component getDisplayName() {
        return Component.translatable("block.iska_utils.auto_shop");
    }
    
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new net.unfamily.iskautils.client.gui.AutoShopMenu(containerId, playerInventory, this);
    }
    
    @Override
    public void onDataPacket(net.minecraft.network.Connection net, net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider lookupProvider) {
        super.onDataPacket(net, pkt, lookupProvider);
        if (pkt.getTag() != null) {
            loadAdditional(pkt.getTag(), lookupProvider);
        }
    }
    
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        
        // Salva item handler
        tag.put("inventory", itemHandler.serializeNBT(registries));
        
        // Salva encapsulated slot
        tag.put("encapsulatedSlot", encapsulatedSlot.serializeNBT(registries));
        
        // Salva stato del negozio
        CompoundTag shopData = new CompoundTag();
        shopData.putBoolean("isActive", isActive);
        shopData.putString("currentCategory", currentCategory);
        shopData.putString("selectedValute", selectedValute);
        tag.put("shopData", shopData);
    }
    
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        
        // Carica item handler
        if (tag.contains("inventory")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        }
        
        // Carica encapsulated slot
        if (tag.contains("encapsulatedSlot")) {
            encapsulatedSlot.deserializeNBT(registries, tag.getCompound("encapsulatedSlot"));
        }
        
        // Carica stato del negozio
        if (tag.contains("shopData")) {
            CompoundTag shopData = tag.getCompound("shopData");
            this.isActive = shopData.getBoolean("isActive");
            this.currentCategory = shopData.getString("currentCategory");
            this.selectedValute = shopData.getString("selectedValute");
            if (this.selectedValute.isEmpty()) {
                this.selectedValute = "unset"; // Fallback se vuoto
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
    
    public IItemHandler getItemHandler() {
        return this.itemHandler;
    }
    
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
    
    public String getSelectedValute() {
        return this.selectedValute;
    }
    
    public void setSelectedValute(String valute) {
        this.selectedValute = valute != null ? valute : "unset";
        setChanged();
    }
    
    /**
     * Tick del blocco (chiamato dal server)
     */
    public static void tick(Level level, BlockPos pos, BlockState state, AutoShopBlockEntity entity) {
        if (level.isClientSide()) {
            return;
        }
        
        // TODO: Implementare logica di tick se necessario
    }
} 
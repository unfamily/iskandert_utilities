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
            // TODO: Implementare validazione basata su configurazione shop
            return true;
        }
    };
    
    // Stato del negozio (semplificato)
    private boolean isActive = false;
    private String currentCategory = "000_default";
    
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
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider provider) {
        super.saveAdditional(nbt, provider);
        
        // Salva solo i dati essenziali
        nbt.putBoolean("isActive", this.isActive);
        nbt.putString("currentCategory", this.currentCategory);
    }
    
    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider provider) {
        super.loadAdditional(nbt, provider);
        
        // Carica solo i dati essenziali
        this.isActive = nbt.getBoolean("isActive");
        this.currentCategory = nbt.getString("currentCategory");
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
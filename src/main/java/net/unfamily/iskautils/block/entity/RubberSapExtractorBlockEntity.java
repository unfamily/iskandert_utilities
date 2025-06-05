package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.block.RubberLogEmptyBlock;
import net.unfamily.iskautils.block.RubberLogFilledBlock;
import net.unfamily.iskautils.block.RubberSapExtractorBlock;
import net.unfamily.iskautils.item.ModItems;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RubberSapExtractorBlockEntity extends BlockEntity implements WorldlyContainer {
    private static final Logger LOGGER = LoggerFactory.getLogger(RubberSapExtractorBlockEntity.class);
    
    // Inventario: slot 0 per output (sap)
    private final NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    
    // Energia
    private final EnergyStorageImpl energyStorage;
    
    // Contatori e stato
    private int extractionProgress = 0;
    private int extractionTime = Config.rubberSapExtractorSpeed;
    private boolean isPowered = false;
    
    public RubberSapExtractorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RUBBER_SAP_EXTRACTOR.get(), pos, state);
        this.energyStorage = new EnergyStorageImpl(Config.rubberSapExtractorEnergyBuffer);
    }
    
    /**
     * Metodo principale chiamato ad ogni tick del server
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, RubberSapExtractorBlockEntity blockEntity) {
        // Se il blocco è spento (redstone disabilitata), non fare nulla
        if (state.getValue(RubberSapExtractorBlock.POWERED)) {
            return;
        }
        
        // Controllo se c'è energia sufficiente
        if (blockEntity.energyStorage.getEnergyStored() < Config.rubberSapExtractorEnergyConsume) {
            return;
        }
        
        // Solo procedi se c'è spazio nell'inventario di output
        if (blockEntity.isFull()) {
            return;
        }
        
        // Se non stiamo già estraendo, cerca un blocco riempito
        if (blockEntity.extractionProgress == 0) {
            // Ottieni la direzione in cui è rivolto l'estrattore
            Direction facing = state.getValue(RubberSapExtractorBlock.FACING);
            
            // Controlla i blocchi dietro l'estrattore
            Direction opposite = facing.getOpposite();
            BlockPos checkPos = pos.relative(opposite);
            
            // Funzione per trovare un blocco di rubber log pieno
            BlockPos filledLogPos = blockEntity.findFilledRubberLog(level, checkPos, opposite);
            
            // Se è stato trovato un blocco pieno, inizia l'estrazione
            if (filledLogPos != null) {
                blockEntity.extractionProgress = 1;
                blockEntity.setChanged();
            }
        } 
        // Se stiamo già estraendo, continua il processo
        else {
            blockEntity.extractionProgress++;
            
            // Quando il processo è completato
            if (blockEntity.extractionProgress >= blockEntity.extractionTime) {
                // Estrai un sap
                blockEntity.extractSap();
                
                // Consuma energia
                blockEntity.energyStorage.extractEnergy(Config.rubberSapExtractorEnergyConsume, false);
                
                // Resetta il progresso
                blockEntity.extractionProgress = 0;
                blockEntity.setChanged();
            }
        }
    }
    
    /**
     * Cerca un blocco di rubber log pieno, verificando prima dietro al blocco e poi salendo
     * @param level Il mondo
     * @param startPos Posizione di partenza
     * @param searchDirection Direzione in cui continuare a cercare
     * @return La posizione del blocco pieno trovato, o null se non trovato
     */
    private BlockPos findFilledRubberLog(Level level, BlockPos startPos, Direction searchDirection) {
        BlockPos currentPos = startPos;
        
        // Prima controlla se il blocco dietro è un log di rubber pieno
        BlockState state = level.getBlockState(currentPos);
        if (state.is(ModBlocks.RUBBER_LOG_FILLED.get())) {
            return currentPos;
        }
        
        // Se è un log di rubber vuoto o un log normale, continua a salire
        if (state.is(ModBlocks.RUBBER_LOG_EMPTY.get()) || state.is(ModBlocks.RUBBER_LOG.get())) {
            // Continua a salire finché non troviamo un blocco pieno o usciamo dalla colonna di rubber log
            BlockPos abovePos = currentPos;
            int maxChecks = 10; // Limite per evitare loop infiniti
            
            for (int i = 0; i < maxChecks; i++) {
                abovePos = abovePos.above();
                BlockState aboveState = level.getBlockState(abovePos);
                
                // Se troviamo un blocco pieno, lo restituiamo
                if (aboveState.is(ModBlocks.RUBBER_LOG_FILLED.get())) {
                    return abovePos;
                }
                
                // Se non è un blocco di rubber log, usciamo dal ciclo
                if (!aboveState.is(ModBlocks.RUBBER_LOG_EMPTY.get()) && !aboveState.is(ModBlocks.RUBBER_LOG.get())) {
                    break;
                }
            }
        }
        
        // Nessun blocco pieno trovato
        return null;
    }
    
    /**
     * Estrae un sap dal blocco pieno e lo aggiunge all'inventario
     */
    private void extractSap() {
        if (this.level == null) return;
        
        // Ottieni la direzione in cui è rivolto l'estrattore
        BlockState blockState = this.level.getBlockState(this.getBlockPos());
        if (!blockState.hasProperty(RubberSapExtractorBlock.FACING)) return;
        
        Direction facing = blockState.getValue(RubberSapExtractorBlock.FACING);
        
        // Controlla i blocchi dietro l'estrattore
        Direction opposite = facing.getOpposite();
        BlockPos checkPos = this.getBlockPos().relative(opposite);
        
        // Cerca un blocco di rubber log pieno
        BlockPos filledLogPos = findFilledRubberLog(this.level, checkPos, opposite);
        
        if (filledLogPos != null) {
            // Aggiungi un sap all'inventario
            ItemStack sapStack = new ItemStack(ModItems.SAP.get());
            if (items.get(0).isEmpty()) {
                items.set(0, sapStack);
            } else {
                items.get(0).grow(1);
            }
            
            // Converti il blocco pieno in vuoto preservando tutte le proprietà
            BlockState filledLogState = this.level.getBlockState(filledLogPos);
            BlockState emptyLogState = ModBlocks.RUBBER_LOG_EMPTY.get().defaultBlockState();
            
            // Preserva solo la proprietà AXIS che è quella che determina l'orientamento
            if (filledLogState.hasProperty(BlockStateProperties.AXIS) && 
                emptyLogState.hasProperty(BlockStateProperties.AXIS)) {
                emptyLogState = emptyLogState.setValue(
                    BlockStateProperties.AXIS, 
                    filledLogState.getValue(BlockStateProperties.AXIS)
                );
            }
            
            // Imposta il blocco come vuoto
            this.level.setBlock(filledLogPos, emptyLogState, Block.UPDATE_ALL);
            
            // Notifico il cambiamento
            LOGGER.debug("Converted filled rubber log to empty at {}", filledLogPos);
        }
    }
    
    /**
     * Verifica se l'inventario di output è pieno
     * @return true se l'inventario è pieno
     */
    private boolean isFull() {
        ItemStack stack = items.get(0);
        return !stack.isEmpty() && stack.getCount() >= stack.getMaxStackSize();
    }
    
    /**
     * Chiamato quando lo stato powered del blocco cambia
     */
    public void onPoweredStateChanged(boolean powered) {
        this.isPowered = powered;
        this.setChanged();
    }
    
    /**
     * Restituisce la lista degli item contenuti
     * @return lista degli item nel contenitore
     */
    public NonNullList<ItemStack> getItems() {
        return this.items;
    }
    
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        
        // Salva l'inventario
        ContainerHelper.saveAllItems(tag, this.items, provider);
        
        // Salva lo stato di estrazione
        tag.putInt("ExtractionProgress", this.extractionProgress);
        
        // Salva energia
        tag.putInt("Energy", this.energyStorage.getEnergyStored());
    }
    
    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        
        // Carica l'inventario 
        ContainerHelper.loadAllItems(tag, this.items, provider);
        
        // Carica lo stato di estrazione
        this.extractionProgress = tag.getInt("ExtractionProgress");
        
        // Carica energia
        this.energyStorage.setEnergy(tag.getInt("Energy"));
    }
    
    /**
     * Custom Energy Storage implementation
     */
    public class EnergyStorageImpl extends EnergyStorage {
        public EnergyStorageImpl(int capacity) {
            super(capacity);
        }
        
        public void setEnergy(int energy) {
            this.energy = Math.max(0, Math.min(energy, capacity));
        }
    }
    
    /**
     * Ottiene lo storage di energia
     */
    public IEnergyStorage getEnergyStorage() {
        return this.energyStorage;
    }
    
    // --- WorldlyContainer Implementation ---
    
    private static final int[] SLOTS_FOR_UP = new int[]{};
    private static final int[] SLOTS_FOR_DOWN = new int[]{0};
    private static final int[] SLOTS_FOR_SIDES = new int[]{};
    
    @Override
    public int getContainerSize() {
        return items.size();
    }
    
    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }
    
    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) {
            setChanged();
        }
        return result;
    }
    
    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }
    
    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }
    
    @Override
    public boolean stillValid(Player player) {
        if (this.level == null || this.level.getBlockEntity(this.worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr(this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.5, this.worldPosition.getZ() + 0.5) <= 64.0 &&
               level.getBlockState(worldPosition).getBlock() instanceof RubberSapExtractorBlock;
    }
    
    @Override
    public void clearContent() {
        items.clear();
    }
    
    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) {
            return SLOTS_FOR_DOWN;
        }
        return side == Direction.UP ? SLOTS_FOR_UP : SLOTS_FOR_SIDES;
    }
    
    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        return false; // Non accettiamo input
    }
    
    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return direction == Direction.DOWN && slot == 0; // Solo output dal basso
    }
} 
package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.unfamily.iskalib.transfer.LegacyItemHandlerResourceHandler;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.block.EntropicSpawnerBlock;
import net.unfamily.iskautils.client.gui.EntropicSpawnerMenu;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.util.AncientTableFuel;
import net.unfamily.iskautils.particle.ModParticles;
import net.unfamily.iskautils.util.EntropicSpawnerSpawnUtil;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Block entity for the Entropic Spawner machine.
 */
public class EntropicSpawnerBlockEntity extends BlockEntity implements MenuProvider {
    private static final double CLIENT_PARTICLE_PLAYER_RANGE = 16.0D;
    private static final int SPAWN_BURST_PARTICLES_PER_TICK = 8;
    public static final int CLOCK_SLOT_INDEX = 0;
    public static final int PRODUCTION_SLOT_INDEX = 1;
    public static final int PLACEHOLDER_SLOT_INDEX = 2;
    public static final int FUEL_SLOT_INDEX = 3;
    public static final int MACHINE_SLOT_COUNT = 4;
    public static final int SPAWN_TOP_TEXTURE_TICKS = 30;

    private final ItemStackHandler machineItems = new ItemStackHandler(MACHINE_SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide()) {
                syncBlockState();
            }
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case CLOCK_SLOT_INDEX -> stack.is(ModItems.ENTROPIC_CLOCK.get());
                case PRODUCTION_SLOT_INDEX -> stack.is(ModItems.PRODUCTION_MODULE.get());
                case FUEL_SLOT_INDEX -> AncientTableFuel.isEntropyFuel(stack);
                default -> false;
            };
        }

        @Override
        public int getSlotLimit(int slot) {
            return switch (slot) {
                case CLOCK_SLOT_INDEX -> Math.min(64, Config.entropicSpawnerMaxEntropicClocks);
                case PRODUCTION_SLOT_INDEX -> Math.min(64, Config.entropicSpawnerMaxProductionModules);
                case FUEL_SLOT_INDEX -> 64;
                default -> 0;
            };
        }
    };

    private final ResourceHandler<ItemResource> itemTransferHandler =
            LegacyItemHandlerResourceHandler.wrap(machineItems);

    private int storedEntropy;
    private int lifetimeSpawnCount;
    private int spawnDelayTicks = -1;
    private int spawnTopTextureTicks = 0;
    private int redstoneMode = 0;
    private boolean pulsePreviousRedstone = false;
    private boolean pulseEdgeAllowsSpawn = false;
    @Nullable
    private Identifier spawnEntityId = null;
    @Nullable
    private Entity displayEntity;
    private double spin;
    private double oSpin;

    public EntropicSpawnerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENTROPIC_SPAWNER_BE.get(), pos, state);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, EntropicSpawnerBlockEntity be) {
        be.clientTick(level, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, EntropicSpawnerBlockEntity be) {
        if (!Config.entropicSpawnerEnabled) {
            return;
        }
        be.serverTick((ServerLevel) level, state);
    }

    private void clientTick(Level level, BlockState state) {
        if (!state.getValue(EntropicSpawnerBlock.ACTIVE) || spawnEntityId == null) {
            return;
        }
        if (!level.hasNearbyAlivePlayer(
                worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D,
                CLIENT_PARTICLE_PLAYER_RANGE)) {
            oSpin = spin;
            return;
        }

        EntityType<?> type = getSpawnEntityType();
        if (type != null) {
            getOrCreateDisplayEntity(level, type);
        }

        oSpin = spin;
        float spinDelay = state.getValue(EntropicSpawnerBlock.SPAWNING)
                ? 20.0F
                : Math.max(spawnDelayTicks, 0);
        spin = (spin + 1000.0F / (spinDelay + 200.0F)) % 360.0D;

        RandomSource random = level.getRandom();
        double innerX = worldPosition.getX() + 0.15D + random.nextDouble() * 0.7D;
        double innerY = worldPosition.getY() + 0.15D + random.nextDouble() * 0.55D;
        double innerZ = worldPosition.getZ() + 0.15D + random.nextDouble() * 0.7D;
        level.addParticle(ModParticles.ENTROPIC_FLAME.get(), innerX, innerY, innerZ, 0.0D, 0.0D, 0.0D);

        if (state.getValue(EntropicSpawnerBlock.SPAWNING)) {
            for (int i = 0; i < SPAWN_BURST_PARTICLES_PER_TICK; i++) {
                double topX = worldPosition.getX() + 0.1D + random.nextDouble() * 0.8D;
                double topY = worldPosition.getY() + 0.95D + random.nextDouble() * 0.85D;
                double topZ = worldPosition.getZ() + 0.1D + random.nextDouble() * 0.8D;
                double vx = (random.nextDouble() - 0.5D) * 0.025D;
                double vy = 0.03D + random.nextDouble() * 0.07D;
                double vz = (random.nextDouble() - 0.5D) * 0.025D;
                level.addParticle(ModParticles.ENTROPIC_FLAME.get(), topX, topY, topZ, vx, vy, vz);
            }
        }
    }

    public double getSpin() {
        return spin;
    }

    public double getOSpin() {
        return oSpin;
    }

    private void serverTick(ServerLevel level, BlockState state) {
        tickPulseRedstone(level);
        tryAbsorbFuelSlot();
        tickSpawnTopTexture();
        syncBlockState();

        if (spawnEntityId == null || isSpawnCapReached() || !countdownAllowed(level)) {
            return;
        }

        if (EntropicSpawnerSpawnUtil.isMobCapReachedAbove(level, worldPosition)) {
            return;
        }

        if (spawnDelayTicks < 0) {
            spawnDelayTicks = rollSpawnDelay();
            setChanged();
            return;
        }

        if (spawnDelayTicks > 0) {
            spawnDelayTicks--;
            setChanged();
            return;
        }

        if (!spawnAllowed(level)) {
            return;
        }

        attemptSpawnCycle(level);
        spawnDelayTicks = rollSpawnDelay();
        setChanged();
    }

    private void attemptSpawnCycle(ServerLevel level) {
        EntityType<?> type = getSpawnEntityType();
        if (type == null || EntropicSpawnerSpawnUtil.isMobCapReachedAbove(level, worldPosition)) {
            return;
        }
        BlockPos spawnPos = worldPosition.above();
        int count = 1 + getInstalledProductionModuleCount();
        int spawned = 0;
        for (int i = 0; i < count; i++) {
            if (isSpawnCapReached()) {
                break;
            }
            if (EntropicSpawnerSpawnUtil.isMobCapReachedAbove(level, worldPosition)) {
                break;
            }
            if (EntropicSpawnerSpawnUtil.trySpawnOneRelaxed(level, spawnPos, type)) {
                spawned++;
                lifetimeSpawnCount++;
            }
        }
        if (spawned > 0) {
            triggerSpawnTopTexture();
            setChanged();
        }
    }

    private void tryAbsorbFuelSlot() {
        boolean changed = false;
        while (true) {
            ItemStack fuel = machineItems.getStackInSlot(FUEL_SLOT_INDEX);
            if (!AncientTableFuel.isEntropyFuel(fuel)) {
                break;
            }
            if (!AncientTableFuel.canAbsorbOneMore(storedEntropy)) {
                break;
            }
            fuel.shrink(1);
            storedEntropy += AncientTableFuel.fuelPerDrop();
            machineItems.setStackInSlot(FUEL_SLOT_INDEX, fuel.isEmpty() ? ItemStack.EMPTY : fuel);
            changed = true;
        }
        if (changed) {
            setChangedAndSync();
        }
    }

    private void tickSpawnTopTexture() {
        if (spawnTopTextureTicks > 0) {
            spawnTopTextureTicks--;
        }
    }

    private void triggerSpawnTopTexture() {
        spawnTopTextureTicks = SPAWN_TOP_TEXTURE_TICKS;
        syncBlockState();
    }

    private int rollSpawnDelay() {
        int min = Config.entropicSpawnerBaseDelayMin;
        int max = Config.entropicSpawnerBaseDelayMax;
        if (max < min) {
            max = min;
        }
        int base = min + (max > min && level != null ? level.getRandom().nextInt(max - min + 1) : 0);
        double factor = 1.0D;
        if (hasEntropyFuel()) {
            factor *= Config.entropicSpawnerFuelSpeedMultiplier;
        }
        int clocks = getInstalledClockCount();
        if (clocks > 0 && Config.entropicSpawnerClockDelayFactorPerClock > 0.0D) {
            factor *= Math.pow(Config.entropicSpawnerClockDelayFactorPerClock, clocks);
        }
        return Math.max(20, (int) Math.round(base * factor));
    }

    public boolean setSpawnEntityFromEgg(ItemStack eggStack) {
        if (!(eggStack.getItem() instanceof SpawnEggItem spawnEgg)) {
            return false;
        }
        EntityType<?> type = SpawnEggItem.getType(eggStack);
        if (type == null) {
            return false;
        }
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (id == null) {
            return false;
        }
        spawnEntityId = id;
        clearDisplayEntity();
        spawnDelayTicks = -1;
        setChangedAndSync();
        return true;
    }

    private void clearDisplayEntity() {
        if (displayEntity != null) {
            displayEntity.discard();
            displayEntity = null;
        }
    }

    @Nullable
    public Entity getOrCreateDisplayEntity(Level level, EntityType<?> type) {
        if (displayEntity != null && displayEntity.getType() != type) {
            displayEntity.discard();
            displayEntity = null;
        }
        if (displayEntity == null) {
            displayEntity = type.create(level, EntitySpawnReason.MOB_SUMMONED);
            if (displayEntity != null) {
                displayEntity.setPos(worldPosition.getX() + 0.5D, worldPosition.getY(), worldPosition.getZ() + 0.5D);
                displayEntity.setYRot(0.0F);
                displayEntity.setYHeadRot(0.0F);
                displayEntity.setOldPosAndRot();
            }
        }
        return displayEntity;
    }

    @Override
    public void setRemoved() {
        clearDisplayEntity();
        super.setRemoved();
    }

    @Nullable
    public EntityType<?> getSpawnEntityType() {
        if (spawnEntityId == null) {
            return null;
        }
        return BuiltInRegistries.ENTITY_TYPE.getValue(spawnEntityId);
    }

    public int getSpawnDelayTicks() {
        return Math.max(0, spawnDelayTicks);
    }

    public int getSpawnEntityTypeSyncId() {
        EntityType<?> type = getSpawnEntityType();
        return type == null ? -1 : BuiltInRegistries.ENTITY_TYPE.getId(type);
    }

    public int getRedstoneMode() {
        return redstoneMode;
    }

    public void cycleRedstoneMode() {
        applyRedstoneMode((redstoneMode + 1) % 5);
        setChangedAndSync();
    }

    public void cycleRedstoneModeBackward() {
        applyRedstoneMode((redstoneMode + 4) % 5);
        setChangedAndSync();
    }

    private void applyRedstoneMode(int mode) {
        redstoneMode = mode;
        if (mode == 3 && level != null && !level.isClientSide()) {
            pulsePreviousRedstone = level.getBestNeighborSignal(worldPosition) > 0;
        } else if (mode != 3) {
            pulsePreviousRedstone = false;
        }
    }

    private void tickPulseRedstone(Level level) {
        pulseEdgeAllowsSpawn = false;
        if (redstoneMode != 3) {
            return;
        }
        boolean sig = level.getBestNeighborSignal(worldPosition) > 0;
        pulseEdgeAllowsSpawn = sig && !pulsePreviousRedstone;
        pulsePreviousRedstone = sig;
    }

    private boolean countdownAllowed(Level level) {
        if (redstoneMode == 4) {
            return false;
        }
        if (redstoneMode == 3) {
            return level.getBestNeighborSignal(worldPosition) > 0;
        }
        return operationalRedstone(level);
    }

    private boolean spawnAllowed(Level level) {
        if (redstoneMode == 3) {
            return pulseEdgeAllowsSpawn;
        }
        return operationalRedstone(level);
    }

    private boolean operationalRedstone(Level level) {
        int power = level.getBestNeighborSignal(worldPosition);
        boolean sig = power > 0;
        return switch (redstoneMode) {
            case 0 -> true;
            case 1 -> !sig;
            case 2 -> sig;
            case 4 -> false;
            default -> false;
        };
    }

    public boolean isSpawnCapReached() {
        int max = Config.entropicSpawnerMaxLifetimeSpawns;
        return max > 0 && lifetimeSpawnCount >= max;
    }

    public int getLifetimeSpawnCount() {
        return lifetimeSpawnCount;
    }

    public int getLifetimeSpawnMax() {
        return Config.entropicSpawnerMaxLifetimeSpawns;
    }

    private boolean redstoneAllowsSpawn(Level level) {
        if (spawnEntityId == null || isSpawnCapReached()) {
            return false;
        }
        if (redstoneMode == 4) {
            return false;
        }
        if (redstoneMode == 3) {
            return level.getBestNeighborSignal(worldPosition) > 0;
        }
        return operationalRedstone(level);
    }

    public int getInstalledClockCount() {
        return Math.min(machineItems.getStackInSlot(CLOCK_SLOT_INDEX).getCount(),
                Config.entropicSpawnerMaxEntropicClocks);
    }

    public int getInstalledProductionModuleCount() {
        return Math.min(machineItems.getStackInSlot(PRODUCTION_SLOT_INDEX).getCount(),
                Config.entropicSpawnerMaxProductionModules);
    }

    public boolean hasEntropyFuel() {
        return storedEntropy > 0;
    }

    public int getStoredEntropy() {
        return storedEntropy;
    }

    public int getMaxStoredEntropy() {
        return AncientTableFuel.maxStored();
    }

    public ItemStackHandler getMachineItems() {
        return machineItems;
    }

    public ResourceHandler<ItemResource> getItemTransferHandler() {
        return itemTransferHandler;
    }

    private void syncBlockState() {
        if (level == null || level.isClientSide()) {
            return;
        }
        BlockState state = getBlockState();
        boolean active = spawnEntityId != null && redstoneAllowsSpawn(level);
        boolean spawning = active && spawnTopTextureTicks > 0;
        BlockState newState = state
                .setValue(EntropicSpawnerBlock.ACTIVE, active)
                .setValue(EntropicSpawnerBlock.SPAWNING, spawning);
        if (newState != state) {
            level.setBlock(worldPosition, newState, 3);
        }
    }

    private void setChangedAndSync() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            syncBlockState();
        }
    }

    public void drops() {
        if (level == null) {
            return;
        }
        for (int i = 0; i < machineItems.getSlots(); i++) {
            ItemStack stack = machineItems.getStackInSlot(i);
            if (!stack.isEmpty()) {
                net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("StoredEntropy", storedEntropy);
        output.putInt("LifetimeSpawnCount", lifetimeSpawnCount);
        output.putInt("SpawnDelay", spawnDelayTicks);
        output.putInt("RedstoneMode", redstoneMode);
        output.putBoolean("PulsePreviousRedstone", pulsePreviousRedstone);
        if (spawnEntityId != null) {
            output.putString("SpawnEntity", spawnEntityId.toString());
        }
        ValueOutput.TypedOutputList<ItemStackWithSlot> items = output.list("MachineItems", ItemStackWithSlot.CODEC);
        for (int slot = 0; slot < machineItems.getSlots(); slot++) {
            ItemStack stack = machineItems.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                items.add(new ItemStackWithSlot(slot, stack));
            }
        }
        if (items.isEmpty()) {
            output.discard("MachineItems");
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        Identifier previousSpawnEntityId = spawnEntityId;
        super.loadAdditional(input);
        storedEntropy = Mth.clamp(input.getIntOr("StoredEntropy", 0), 0, getMaxStoredEntropy());
        lifetimeSpawnCount = Math.max(0, input.getIntOr("LifetimeSpawnCount", 0));
        spawnDelayTicks = input.getIntOr("SpawnDelay", -1);
        redstoneMode = input.getIntOr("RedstoneMode", 0);
        if (redstoneMode == 3) {
            redstoneMode = 4;
        }
        pulsePreviousRedstone = input.getBooleanOr("PulsePreviousRedstone", false);
        spawnEntityId = input.getString("SpawnEntity").map(Identifier::tryParse).orElse(null);
        if (!Objects.equals(previousSpawnEntityId, spawnEntityId)) {
            clearDisplayEntity();
        }
        var entries = input.listOrEmpty("MachineItems", ItemStackWithSlot.CODEC);
        boolean legacy = false;
        for (ItemStackWithSlot entry : entries) {
            if (entry.slot() >= MACHINE_SLOT_COUNT) {
                legacy = true;
                break;
            }
        }
        if (legacy) {
            loadLegacyMachineItems(entries);
        } else {
            for (ItemStackWithSlot entry : entries) {
                int slot = entry.slot();
                if (slot >= 0 && slot < machineItems.getSlots()) {
                    machineItems.setStackInSlot(slot, entry.stack());
                }
            }
        }
    }

    private void loadLegacyMachineItems(Iterable<ItemStackWithSlot> entries) {
        ItemStack clocks = ItemStack.EMPTY;
        ItemStack production = ItemStack.EMPTY;
        ItemStack fuel = ItemStack.EMPTY;
        int clockMax = Config.entropicSpawnerMaxEntropicClocks;
        int prodMax = Config.entropicSpawnerMaxProductionModules;
        for (ItemStackWithSlot entry : entries) {
            int slot = entry.slot();
            ItemStack stack = entry.stack();
            if (stack.isEmpty()) {
                continue;
            }
            if (slot >= 0 && slot < 5 && stack.is(ModItems.ENTROPIC_CLOCK.get())) {
                clocks = mergeStacks(clocks, stack, clockMax);
            } else if (slot == 5) {
                fuel = stack.copy();
            } else if (slot >= 6 && slot < 14 && stack.is(ModItems.PRODUCTION_MODULE.get())) {
                production = mergeStacks(production, stack, prodMax);
            }
        }
        machineItems.setStackInSlot(CLOCK_SLOT_INDEX, clocks);
        machineItems.setStackInSlot(PRODUCTION_SLOT_INDEX, production);
        machineItems.setStackInSlot(PLACEHOLDER_SLOT_INDEX, ItemStack.EMPTY);
        machineItems.setStackInSlot(FUEL_SLOT_INDEX, fuel);
    }

    private static ItemStack mergeStacks(ItemStack into, ItemStack add, int max) {
        if (add.isEmpty()) {
            return into;
        }
        if (into.isEmpty()) {
            ItemStack copy = add.copy();
            copy.setCount(Math.min(copy.getCount(), max));
            return copy;
        }
        if (!ItemStack.isSameItemSameComponents(into, add)) {
            return into;
        }
        into.grow(Math.min(add.getCount(), max - into.getCount()));
        return into;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.iska_utils.entropic_spawner");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new EntropicSpawnerMenu(containerId, playerInventory, this);
    }
}

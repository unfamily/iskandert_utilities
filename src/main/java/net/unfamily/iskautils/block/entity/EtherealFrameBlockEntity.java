package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.iskautils.block.EtherealFrameBlock;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.util.EtherealFrameFilterMatcher;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiConsumer;

/**
 * Block entity for the Ethereal Frame.
 * Stores an entity type filter (allow/deny list) and optional camouflage BlockState.
 * Adjacent frames form a local network that shares filters.
 */
public class EtherealFrameBlockEntity extends BlockEntity {

    private static final int MAX_NETWORK_SIZE = 64;
    private static final int SYNC_INTERVAL_TICKS = 40;

    /** Callback invoked on the client when camouflage changes (set during client init). */
    @Nullable
    private static BiConsumer<BlockPos, BlockState> clientCamouflageCallback = null;

    public static void setClientCamouflageCallback(BiConsumer<BlockPos, BlockState> cb) {
        clientCamouflageCallback = cb;
    }

    private final List<String> filterEntityTypes = new ArrayList<>();
    private boolean allowMode = true;
    private boolean blocksLight = false;
    private long lastFilterUpdate = 0;
    private int syncTickCounter = 0;

    @Nullable
    private BlockState camouflageState = null;

    private boolean reinforced = false;
    @Nullable
    private Block reinforcementMaterial = null;

    public EtherealFrameBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ETHEREAL_FRAME_BE.get(), pos, state);
        filterEntityTypes.add("minecraft:player");
        if (state.hasProperty(EtherealFrameBlock.BLOCKS_LIGHT)) {
            blocksLight = state.getValue(EtherealFrameBlock.BLOCKS_LIGHT);
        }
        if (state.hasProperty(EtherealFrameBlock.REINFORCED)) {
            reinforced = state.getValue(EtherealFrameBlock.REINFORCED);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, EtherealFrameBlockEntity frame) {
        if (level.isClientSide) {
            return;
        }
        frame.syncTickCounter++;
        if (frame.syncTickCounter >= SYNC_INTERVAL_TICKS) {
            frame.syncTickCounter = 0;
            frame.syncNetworkFromNewest();
        }
    }

    // ==================== Filter logic ====================

    public boolean shouldEntityPass(Entity entity) {
        return EtherealFrameFilterMatcher.shouldEntityPass(entity, filterEntityTypes, allowMode);
    }

    public List<String> getFilterEntityTypes() {
        return Collections.unmodifiableList(filterEntityTypes);
    }

    public void setFilterEntityTypes(List<String> types) {
        filterEntityTypes.clear();
        filterEntityTypes.addAll(EtherealFrameFilterMatcher.normalizeEntries(types));
        markFilterUpdated();
        setChanged();
        sendUpdateToClients();
    }

    public boolean isAllowMode() {
        return allowMode;
    }

    public void setAllowMode(boolean allowMode) {
        this.allowMode = allowMode;
        markFilterUpdated();
        setChanged();
        sendUpdateToClients();
    }

    public void toggleAllowMode() {
        setAllowMode(!allowMode);
    }

    public boolean blocksLight() {
        return blocksLight;
    }

    public void setBlocksLight(boolean blocksLight) {
        if (this.blocksLight == blocksLight) {
            return;
        }
        this.blocksLight = blocksLight;
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = level.getBlockState(worldPosition);
            if (state.getBlock() instanceof EtherealFrameBlock
                    && state.getValue(EtherealFrameBlock.BLOCKS_LIGHT) != blocksLight) {
                level.setBlock(worldPosition, state.setValue(EtherealFrameBlock.BLOCKS_LIGHT, blocksLight), Block.UPDATE_ALL);
            }
            level.getLightEngine().checkBlock(worldPosition);
            sendUpdateToClients();
        }
    }

    public void toggleBlocksLight() {
        setBlocksLight(!blocksLight);
    }

    public long getLastFilterUpdate() {
        return lastFilterUpdate;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        syncBlocksLightToBlockState();
        syncReinforcedToBlockState();
    }

    private void syncBlocksLightToBlockState() {
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState state = level.getBlockState(worldPosition);
        if (state.getBlock() instanceof EtherealFrameBlock
                && state.getValue(EtherealFrameBlock.BLOCKS_LIGHT) != blocksLight) {
            level.setBlock(worldPosition, state.setValue(EtherealFrameBlock.BLOCKS_LIGHT, blocksLight), Block.UPDATE_ALL);
            level.getLightEngine().checkBlock(worldPosition);
        }
    }

    private void syncReinforcedToBlockState() {
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState state = level.getBlockState(worldPosition);
        if (state.getBlock() instanceof EtherealFrameBlock
                && state.hasProperty(EtherealFrameBlock.REINFORCED)
                && state.getValue(EtherealFrameBlock.REINFORCED) != reinforced) {
            level.setBlock(worldPosition, state.setValue(EtherealFrameBlock.REINFORCED, reinforced), Block.UPDATE_ALL);
        }
    }

    // ==================== Reinforcement ====================

    public boolean isReinforced() {
        return reinforced;
    }

    public boolean isEffectivelyReinforced() {
        return net.unfamily.iskautils.Config.etherealFrameReinforcementEnabled && reinforced;
    }

    @Nullable
    public Block getReinforcementMaterial() {
        return reinforcementMaterial;
    }

    /**
     * Reinforces as many unreinforced frames in the network as {@code stack} allows.
     * @return number of frames newly reinforced (materials consumed)
     */
    public int tryReinforceNetwork(Player player, ItemStack stack, Block material) {
        if (level == null || level.isClientSide || !net.unfamily.iskautils.Config.etherealFrameReinforcementEnabled) {
            return 0;
        }
        if (!material.defaultBlockState().is(EtherealFrameBlock.REINFORCEMENT_MATERIALS)) {
            return 0;
        }
        List<EtherealFrameBlockEntity> network = collectNetworkFrames(level, worldPosition);
        int available = player.getAbilities().instabuild ? Integer.MAX_VALUE : stack.getCount();
        int used = 0;
        for (EtherealFrameBlockEntity frame : network) {
            if (frame.reinforced) {
                continue;
            }
            if (used >= available) {
                break;
            }
            frame.applyReinforcement(material);
            used++;
        }
        if (used > 0 && !player.getAbilities().instabuild) {
            stack.shrink(used);
        }
        return used;
    }

    /**
     * Removes reinforcement from the whole network and returns materials (unless creative).
     * @return number of frames stripped
     */
    public int tryStripReinforcementNetwork(Player player, @Nullable Direction dropFace) {
        if (level == null || level.isClientSide) {
            return 0;
        }
        List<EtherealFrameBlockEntity> network = collectNetworkFrames(level, worldPosition);
        int stripped = 0;
        for (EtherealFrameBlockEntity frame : network) {
            if (!frame.reinforced) {
                continue;
            }
            Block material = frame.reinforcementMaterial != null
                    ? frame.reinforcementMaterial
                    : ModBlocks.WITHER_PROOF_BLOCK.get();
            frame.clearReinforcement();
            stripped++;
            if (!player.getAbilities().instabuild) {
                ItemStack drop = new ItemStack(material);
                if (dropFace != null) {
                    Block.popResourceFromFace(level, frame.getBlockPos(), dropFace, drop);
                } else {
                    Block.popResource(level, frame.getBlockPos(), drop);
                }
            }
        }
        return stripped;
    }

    private void applyReinforcement(Block material) {
        reinforced = true;
        reinforcementMaterial = material;
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = level.getBlockState(worldPosition);
            if (state.getBlock() instanceof EtherealFrameBlock
                    && state.hasProperty(EtherealFrameBlock.REINFORCED)
                    && !state.getValue(EtherealFrameBlock.REINFORCED)) {
                level.setBlock(worldPosition, state.setValue(EtherealFrameBlock.REINFORCED, true), Block.UPDATE_ALL);
            }
            sendUpdateToClients();
        }
    }

    private void clearReinforcement() {
        reinforced = false;
        reinforcementMaterial = null;
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = level.getBlockState(worldPosition);
            if (state.getBlock() instanceof EtherealFrameBlock
                    && state.hasProperty(EtherealFrameBlock.REINFORCED)
                    && state.getValue(EtherealFrameBlock.REINFORCED)) {
                level.setBlock(worldPosition, state.setValue(EtherealFrameBlock.REINFORCED, false), Block.UPDATE_ALL);
            }
            sendUpdateToClients();
        }
    }

    /** Drops the reinforcement material at this frame (e.g. on break). */
    public void dropReinforcementMaterial() {
        if (!reinforced || level == null || level.isClientSide) {
            return;
        }
        Block material = reinforcementMaterial != null
                ? reinforcementMaterial
                : ModBlocks.WITHER_PROOF_BLOCK.get();
        Block.popResource(level, worldPosition, new ItemStack(material));
    }

    private void markFilterUpdated() {
        if (level != null && !level.isClientSide) {
            lastFilterUpdate = level.getGameTime();
        }
    }

    // ==================== Camouflage ====================

    public boolean hasCamouflage() {
        return camouflageState != null;
    }

    @Nullable
    public BlockState getCamouflage() {
        return camouflageState;
    }

    public void setCamouflage(BlockState newState, Level level, BlockPos pos, BlockState currentBlockState) {
        this.camouflageState = newState;
        setChanged();
        level.setBlock(pos, currentBlockState.setValue(EtherealFrameBlock.CAMOUFLAGED, true), Block.UPDATE_ALL);
        sendUpdateToClients();
        level.getLightEngine().checkBlock(pos);
    }

    public void clearCamouflage(Level level, BlockPos pos, BlockState currentBlockState) {
        this.camouflageState = null;
        setChanged();
        level.setBlock(pos, currentBlockState.setValue(EtherealFrameBlock.CAMOUFLAGED, false), Block.UPDATE_ALL);
        sendUpdateToClients();
        level.getLightEngine().checkBlock(pos);
    }

    // ==================== Network propagation ====================

    public void propagateFilterToNetwork(Level level) {
        if (!(level instanceof ServerLevel)) {
            return;
        }

        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        queue.add(worldPosition);
        visited.add(worldPosition);

        List<String> filterCopy = new ArrayList<>(filterEntityTypes);
        boolean modeCopy = allowMode;
        long updateCopy = lastFilterUpdate;

        while (!queue.isEmpty() && visited.size() < MAX_NETWORK_SIZE) {
            BlockPos current = queue.poll();
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.relative(dir);
                if (visited.contains(neighbor)) {
                    continue;
                }
                BlockEntity neighborBe = level.getBlockEntity(neighbor);
                if (!(neighborBe instanceof EtherealFrameBlockEntity neighborFrame)) {
                    continue;
                }
                visited.add(neighbor);
                queue.add(neighbor);
                neighborFrame.applyFilterFromSource(filterCopy, modeCopy, updateCopy);
            }
        }
    }

    public void pullFromNetworkIfNewer() {
        if (level == null || level.isClientSide) {
            return;
        }
        EtherealFrameBlockEntity newest = findNewestInNetwork(level, worldPosition);
        if (newest != null && newest != this && newest.lastFilterUpdate > lastFilterUpdate) {
            applyFilterFromSource(newest.filterEntityTypes, newest.allowMode, newest.lastFilterUpdate);
        }
    }

    private void syncNetworkFromNewest() {
        if (level == null || level.isClientSide) {
            return;
        }
        List<EtherealFrameBlockEntity> network = collectNetworkFrames(level, worldPosition);
        if (network.isEmpty()) {
            return;
        }
        EtherealFrameBlockEntity newest = network.stream()
                .max(Comparator.comparingLong(EtherealFrameBlockEntity::getLastFilterUpdate))
                .orElse(null);
        if (newest == null) {
            return;
        }
        for (EtherealFrameBlockEntity frame : network) {
            if (frame.lastFilterUpdate < newest.lastFilterUpdate) {
                frame.applyFilterFromSource(newest.filterEntityTypes, newest.allowMode, newest.lastFilterUpdate);
            }
        }
    }

    @Nullable
    private static EtherealFrameBlockEntity findNewestInNetwork(Level level, BlockPos start) {
        List<EtherealFrameBlockEntity> network = collectNetworkFrames(level, start);
        return network.stream()
                .max(Comparator.comparingLong(EtherealFrameBlockEntity::getLastFilterUpdate))
                .orElse(null);
    }

    private static List<EtherealFrameBlockEntity> collectNetworkFrames(Level level, BlockPos start) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        List<EtherealFrameBlockEntity> frames = new ArrayList<>();
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty() && visited.size() < MAX_NETWORK_SIZE) {
            BlockPos current = queue.poll();
            BlockEntity be = level.getBlockEntity(current);
            if (be instanceof EtherealFrameBlockEntity frame) {
                frames.add(frame);
                for (Direction dir : Direction.values()) {
                    BlockPos neighbor = current.relative(dir);
                    if (visited.add(neighbor)) {
                        BlockEntity neighborBe = level.getBlockEntity(neighbor);
                        if (neighborBe instanceof EtherealFrameBlockEntity) {
                            queue.add(neighbor);
                        }
                    }
                }
            }
        }
        return frames;
    }

    private void applyFilterFromSource(List<String> filterCopy, boolean modeCopy, long updateCopy) {
        filterEntityTypes.clear();
        filterEntityTypes.addAll(EtherealFrameFilterMatcher.normalizeEntries(filterCopy));
        allowMode = modeCopy;
        lastFilterUpdate = updateCopy;
        setChanged();
        sendUpdateToClients();
    }

    // ==================== Sync ====================

    private void sendUpdateToClients() {
        if (level instanceof ServerLevel serverLevel) {
            ClientboundBlockEntityDataPacket pkt = ClientboundBlockEntityDataPacket.create(this);
            var chunkPos = new net.minecraft.world.level.ChunkPos(worldPosition);
            serverLevel.getChunkSource().chunkMap.getPlayers(chunkPos, false)
                    .forEach(p -> p.connection.send(pkt));
        }
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
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt,
                             HolderLookup.Provider lookupProvider) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            handleUpdateTag(tag, lookupProvider);
        }
        notifyClientCamouflageChange();
    }

    private void notifyClientCamouflageChange() {
        if (clientCamouflageCallback != null) {
            clientCamouflageCallback.accept(worldPosition, camouflageState);
        }
    }

    // ==================== NBT ====================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        net.minecraft.nbt.ListTag listTag = new net.minecraft.nbt.ListTag();
        for (String id : filterEntityTypes) {
            listTag.add(net.minecraft.nbt.StringTag.valueOf(id));
        }
        tag.put("FilterEntityTypes", listTag);
        tag.putBoolean("AllowMode", allowMode);
        tag.putBoolean("BlocksLight", blocksLight);
        tag.putBoolean("Reinforced", reinforced);
        tag.putLong("LastFilterUpdate", lastFilterUpdate);

        if (camouflageState != null) {
            tag.put("Camouflage", NbtUtils.writeBlockState(camouflageState));
        }
        if (reinforced && reinforcementMaterial != null) {
            ResourceLocation matId = BuiltInRegistries.BLOCK.getKey(reinforcementMaterial);
            if (matId != null) {
                tag.putString("ReinforcementMaterial", matId.toString());
            }
        }
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        if (tag.contains("FilterEntityTypes")) {
            filterEntityTypes.clear();
            net.minecraft.nbt.ListTag listTag = tag.getList("FilterEntityTypes", 8);
            List<String> loaded = new ArrayList<>(listTag.size());
            for (int i = 0; i < listTag.size(); i++) {
                loaded.add(listTag.getString(i));
            }
            filterEntityTypes.addAll(EtherealFrameFilterMatcher.normalizeEntries(loaded));
        } else if (filterEntityTypes.isEmpty()) {
            filterEntityTypes.add("minecraft:player");
        }
        allowMode = tag.contains("AllowMode") ? tag.getBoolean("AllowMode") : true;
        blocksLight = tag.contains("BlocksLight") && tag.getBoolean("BlocksLight");
        reinforced = tag.contains("Reinforced") && tag.getBoolean("Reinforced");
        lastFilterUpdate = tag.contains("LastFilterUpdate") ? tag.getLong("LastFilterUpdate") : 0;

        reinforcementMaterial = null;
        if (reinforced && tag.contains("ReinforcementMaterial")) {
            ResourceLocation matId = ResourceLocation.tryParse(tag.getString("ReinforcementMaterial"));
            if (matId != null && BuiltInRegistries.BLOCK.containsKey(matId)) {
                reinforcementMaterial = BuiltInRegistries.BLOCK.get(matId);
            }
        }
        if (reinforced && reinforcementMaterial == null) {
            reinforcementMaterial = ModBlocks.WITHER_PROOF_BLOCK.get();
        }

        if (tag.contains("Camouflage")) {
            camouflageState = NbtUtils.readBlockState(
                    BuiltInRegistries.BLOCK.asLookup(),
                    tag.getCompound("Camouflage")
            );
        } else {
            camouflageState = null;
        }
    }
}

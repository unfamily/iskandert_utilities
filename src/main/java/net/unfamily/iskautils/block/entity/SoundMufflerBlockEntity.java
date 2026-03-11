package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.iskautils.block.ModBlocks;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Block entity for Sound Muffler. Stores per-category volume (0-100%).
 * 0 = mute, 100 = full. Default 50% when placed.
 * <ul>
 *   <li>ALL: all sounds except MUSIC (global cap; vanilla MASTER is treated as ALL).</li>
 *   <li>OTHER: sounds that are not music and not in any vanilla category (uncatalogued / mod sources).</li>
 *   <li>Then vanilla categories: RECORDS, WEATHER, BLOCKS, HOSTILE, NEUTRAL, PLAYERS, AMBIENT, VOICE. MUSIC is never applied.</li>
 * </ul>
 */
public class SoundMufflerBlockEntity extends BlockEntity {

    public static final int CATEGORY_COUNT = 10;
    /** Default volume when block is placed (50%). */
    public static final int DEFAULT_VOLUME = 50;

    /** 0=ALL (excl. music), 1=OTHER (uncatalogued), 2=RECORDS, 3=WEATHER, 4=BLOCKS, 5=HOSTILE, 6=NEUTRAL, 7=PLAYERS, 8=AMBIENT, 9=VOICE. */
    private final int[] volumes = new int[CATEGORY_COUNT];

    /** true = allow list (muffle only sounds in filterSoundIds), false = deny list (muffle all except filterSoundIds). */
    private boolean allowList = false;
    /** Effect range in blocks (min 8, max from config, default 8). */
    private int range = 8;
    /** Sound IDs (e.g. "minecraft:entity.creeper.hiss") in the filter. Empty = no filter applied. */
    private final List<String> filterSoundIds = new ArrayList<>();

    public SoundMufflerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOUND_MUFFLER_BE.get(), pos, state);
        for (int i = 0; i < CATEGORY_COUNT; i++) {
            volumes[i] = DEFAULT_VOLUME;
        }
    }

    public int getVolume(int categoryIndex) {
        if (categoryIndex < 0 || categoryIndex >= CATEGORY_COUNT) return DEFAULT_VOLUME;
        return volumes[categoryIndex];
    }

    public void setVolume(int categoryIndex, int percent) {
        if (categoryIndex < 0 || categoryIndex >= CATEGORY_COUNT) return;
        this.volumes[categoryIndex] = Math.max(0, Math.min(100, percent));
        setChanged();
        if (level != null && !level.isClientSide && level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            var pkt = net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
            var chunkPos = new net.minecraft.world.level.ChunkPos(worldPosition);
            serverLevel.getChunkSource().chunkMap.getPlayers(chunkPos, false).forEach(p -> p.connection.send(pkt));
        }
    }

    public void addVolume(int categoryIndex, int delta) {
        setVolume(categoryIndex, getVolume(categoryIndex) + delta);
    }

    public boolean isAllowList() {
        return allowList;
    }

    public void setAllowList(boolean allowList) {
        if (this.allowList != allowList) {
            this.allowList = allowList;
            setChanged();
            sendUpdateToClients();
        }
    }

    public void toggleAllowList() {
        setAllowList(!allowList);
    }

    /** Returns a copy of the filter sound IDs. */
    public List<String> getFilterSoundIds() {
        return new ArrayList<>(filterSoundIds);
    }

    public void setFilterSoundIds(List<String> ids) {
        this.filterSoundIds.clear();
        if (ids != null) this.filterSoundIds.addAll(ids);
        setChanged();
        sendUpdateToClients();
    }

    /** True if the filter list is active (non-empty). */
    public boolean hasFilter() {
        return !filterSoundIds.isEmpty();
    }

    /** True if this sound ID is allowed by the filter (filter not active, or matches allow/deny list). */
    public boolean isSoundAllowedByFilter(String soundId) {
        if (filterSoundIds.isEmpty()) return true;
        boolean inList = filterSoundIds.contains(soundId);
        return allowList ? inList : !inList;
    }

    public static final int RANGE_MIN = 8;

    public int getRange() {
        return range;
    }

    public void setRange(int value) {
        int max = net.unfamily.iskautils.Config.soundMufflerRangeMax;
        this.range = Math.max(RANGE_MIN, Math.min(max, value));
        setChanged();
        sendUpdateToClients();
    }

    private void sendUpdateToClients() {
        if (level != null && !level.isClientSide && level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            var pkt = net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
            var chunkPos = new net.minecraft.world.level.ChunkPos(worldPosition);
            serverLevel.getChunkSource().chunkMap.getPlayers(chunkPos, false).forEach(p -> p.connection.send(pkt));
        }
    }

    /**
     * Returns the effective volume (0-100) for a given SoundSource.
     * MUSIC is never muffled (caller skips). ALL acts as global multiplier: effective = specific * (all / 100).
     * E.g. All 50%, Blocks 30% -> 15%; All 75%, Blocks 50% -> 37%.
     */
    public int getEffectiveVolumeFor(SoundSource source) {
        if (source == SoundSource.MUSIC) return 100;
        int categoryIndex = soundSourceToCategoryIndex(source);
        int all = volumes[0];
        int specific = categoryIndex >= 0 ? volumes[categoryIndex] : volumes[1]; // OTHER
        return (specific * all) / 100;
    }

    /**
     * Maps SoundSource to our category index.
     * MUSIC -> -1 (never muffled). MASTER -> 0 (ALL, all non-music). Uncatalogued / unknown sources -> 1 (OTHER).
     */
    public static int soundSourceToCategoryIndex(SoundSource source) {
        if (source == SoundSource.MUSIC) return -1;
        return switch (source) {
            case MASTER -> 0; // ALL: all sounds except music
            case RECORDS -> 2;
            case WEATHER -> 3;
            case BLOCKS -> 4;
            case HOSTILE -> 5;
            case NEUTRAL -> 6;
            case PLAYERS -> 7;
            case AMBIENT -> 8;
            case VOICE -> 9;
            default -> 1; // OTHER: not music and not in vanilla categories (uncatalogued)
        };
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putIntArray("Volumes", volumes);
        tag.putBoolean("AllowList", allowList);
        tag.putInt("Range", range);
        ListTag list = new ListTag();
        for (String id : filterSoundIds) list.add(StringTag.valueOf(id));
        tag.put("FilterSounds", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Volumes", net.minecraft.nbt.Tag.TAG_INT_ARRAY)) {
            int[] loaded = tag.getIntArray("Volumes");
            if (loaded.length == 11) {
                volumes[0] = clamp(loaded[0]);
                volumes[1] = clamp(loaded[1]);
                for (int i = 2; i < CATEGORY_COUNT; i++) volumes[i] = clamp(loaded[i + 1]);
            } else {
                for (int i = 0; i < Math.min(loaded.length, CATEGORY_COUNT); i++) {
                    volumes[i] = clamp(loaded[i]);
                }
            }
        }
        if (tag.contains("AllowList", Tag.TAG_ANY_NUMERIC)) allowList = tag.getBoolean("AllowList");
        if (tag.contains("Range", Tag.TAG_ANY_NUMERIC)) range = Math.max(RANGE_MIN, Math.min(net.unfamily.iskautils.Config.soundMufflerRangeMax, tag.getInt("Range")));
        if (tag.contains("FilterSounds", Tag.TAG_LIST)) {
            filterSoundIds.clear();
            ListTag list = tag.getList("FilterSounds", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) filterSoundIds.add(list.getString(i));
        }
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(100, v));
    }

    @Override
    @Nullable
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putIntArray("Volumes", volumes);
        tag.putBoolean("AllowList", allowList);
        tag.putInt("Range", range);
        ListTag list = new ListTag();
        for (String id : filterSoundIds) list.add(StringTag.valueOf(id));
        tag.put("FilterSounds", list);
        return tag;
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection connection,
                             net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt,
                             HolderLookup.Provider registries) {
        super.onDataPacket(connection, pkt, registries);
        CompoundTag t = pkt.getTag();
        if (t == null) return;
        if (t.contains("Volumes", net.minecraft.nbt.Tag.TAG_INT_ARRAY)) {
            int[] loaded = t.getIntArray("Volumes");
            for (int i = 0; i < Math.min(loaded.length, CATEGORY_COUNT); i++) volumes[i] = clamp(loaded[i]);
        }
        if (t.contains("AllowList", Tag.TAG_ANY_NUMERIC)) allowList = t.getBoolean("AllowList");
        if (t.contains("Range", Tag.TAG_ANY_NUMERIC)) range = Math.max(RANGE_MIN, Math.min(net.unfamily.iskautils.Config.soundMufflerRangeMax, t.getInt("Range")));
        if (t.contains("FilterSounds", Tag.TAG_LIST)) {
            filterSoundIds.clear();
            ListTag list = t.getList("FilterSounds", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) filterSoundIds.add(list.getString(i));
        }
    }
}

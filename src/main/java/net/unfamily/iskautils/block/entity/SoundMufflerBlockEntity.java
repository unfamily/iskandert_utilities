package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import com.mojang.serialization.Codec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    /** true = allow list: listed sounds pass through; false = deny list: listed sounds are muffled. */
    private boolean allowList = false;
    /** Effect range in blocks (min 8, max from config, default 8). */
    private int range = 8;
    /** Sound IDs (e.g. "minecraft:entity.creeper.hiss") in the filter. Empty = muffle all sounds. */
    private final List<String> filterSoundIds = new ArrayList<>();

    private static final Pattern ENTITY_SOUND_PATH = Pattern.compile("^entity\\.([a-z0-9_]+)\\.");

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
        if (level != null && !level.isClientSide() && level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            var pkt = net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
            var chunkPos = net.minecraft.world.level.ChunkPos.containing(worldPosition);
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

    /** True if the filter list has entries (allow/deny mode applies). */
    public boolean hasFilter() {
        return !filterSoundIds.isEmpty();
    }

    /**
     * Whether this muffler's volume scaling applies to the given sound.
     * Empty list: all sounds (except music, handled by caller).
     * Allow list: only sounds not in the list.
     * Deny list: only sounds in the list.
     */
    public boolean shouldMuffleSound(String soundId) {
        if (filterSoundIds.isEmpty()) {
            return true;
        }
        boolean inList = filterSoundIds.contains(soundId);
        return allowList ? !inList : inList;
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
        if (level != null && !level.isClientSide() && level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            var pkt = net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
            var chunkPos = net.minecraft.world.level.ChunkPos.containing(worldPosition);
            serverLevel.getChunkSource().chunkMap.getPlayers(chunkPos, false).forEach(p -> p.connection.send(pkt));
        }
    }

    public int getEffectiveVolumeFor(SoundSource source) {
        return getEffectiveVolumeFor(source, null);
    }

    /**
     * Effective volume using optional sound event id to correct categories when {@link SoundSource} does not match
     * (e.g. some entity sounds using MASTER or OTHER).
     */
    public int getEffectiveVolumeFor(SoundSource source, @Nullable Identifier soundEventId) {
        if (source == SoundSource.MUSIC) return 100;
        int categoryIndex = resolveCategoryIndex(source, soundEventId);
        int all = volumes[0];
        int specific = categoryIndex >= 0 ? volumes[categoryIndex] : volumes[1]; // OTHER
        return (specific * all) / 100;
    }

    private static int resolveCategoryIndex(SoundSource source, @Nullable Identifier soundEventId) {
        int fromSource = soundSourceToCategoryIndex(source);
        int fromSound = categoryIndexFromSoundEventId(soundEventId);
        if (fromSound >= 0 && fromSource >= 0) {
            if ((fromSource == 0 || fromSource == 1) && fromSound >= 2) {
                return fromSound;
            }
            if (fromSound == 5 && fromSource != 5) {
                return 5;
            }
        }
        return fromSource;
    }

    /**
     * Maps a sound event id (path {@code entity.<name>.<event>}) to GUI category index, or -1 if unknown.
     */
    private static int categoryIndexFromSoundEventId(@Nullable Identifier soundEventId) {
        if (soundEventId == null) {
            return -1;
        }
        String path = soundEventId.getPath();
        Matcher matcher = ENTITY_SOUND_PATH.matcher(path);
        if (!matcher.find()) {
            return -1;
        }
        String mobPath = matcher.group(1);
        Identifier typeId = Identifier.fromNamespaceAndPath(soundEventId.getNamespace(), mobPath);
        var opt = BuiltInRegistries.ENTITY_TYPE.getOptional(typeId);
        if (opt.isEmpty() && "minecraft".equals(soundEventId.getNamespace())) {
            opt = BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.withDefaultNamespace(mobPath));
        }
        if (opt.isEmpty()) {
            return -1;
        }
        return mobCategoryToCategoryIndex(opt.get().getCategory());
    }

    private static int mobCategoryToCategoryIndex(MobCategory category) {
        if (category == MobCategory.MONSTER) {
            return 5;
        }
        if (category == MobCategory.CREATURE
                || category == MobCategory.AXOLOTLS
                || category == MobCategory.WATER_CREATURE
                || category == MobCategory.WATER_AMBIENT
                || category == MobCategory.UNDERGROUND_WATER_CREATURE) {
            return 6;
        }
        if (category == MobCategory.AMBIENT) {
            return 8;
        }
        if (category == MobCategory.MISC) {
            return 1;
        }
        return -1;
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
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putIntArray("Volumes", volumes);
        output.putBoolean("AllowList", allowList);
        output.putInt("Range", range);
        ValueOutput.TypedOutputList<String> list = output.list("FilterSounds", Codec.STRING);
        for (String id : filterSoundIds) list.add(id);
        if (list.isEmpty()) output.discard("FilterSounds");
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        int[] loaded = input.getIntArray("Volumes").orElse(new int[0]);
        if (loaded.length == 11) {
            volumes[0] = clamp(loaded[0]);
            volumes[1] = clamp(loaded[1]);
            for (int i = 2; i < CATEGORY_COUNT; i++) volumes[i] = clamp(loaded[i + 1]);
        } else if (loaded.length > 0) {
            for (int i = 0; i < Math.min(loaded.length, CATEGORY_COUNT); i++) volumes[i] = clamp(loaded[i]);
        }
        allowList = input.getBooleanOr("AllowList", allowList);
        range = Math.max(RANGE_MIN, Math.min(net.unfamily.iskautils.Config.soundMufflerRangeMax, input.getIntOr("Range", range)));
        filterSoundIds.clear();
        for (String id : input.listOrEmpty("FilterSounds", Codec.STRING)) {
            filterSoundIds.add(id);
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

    // onDataPacket is handled by the current BlockEntity serialization system.
}

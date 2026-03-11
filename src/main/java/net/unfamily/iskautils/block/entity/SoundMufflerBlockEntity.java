package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.iskautils.block.ModBlocks;

import javax.annotation.Nullable;

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
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Volumes", net.minecraft.nbt.Tag.TAG_INT_ARRAY)) {
            int[] loaded = tag.getIntArray("Volumes");
            if (loaded.length == 11) {
                // Migrate from 11 (All, Other, Master, Records, ...) to 10 (All=Master, Other, Records, ...)
                volumes[0] = clamp(loaded[0]);
                volumes[1] = clamp(loaded[1]);
                for (int i = 2; i < CATEGORY_COUNT; i++) volumes[i] = clamp(loaded[i + 1]);
            } else {
                for (int i = 0; i < Math.min(loaded.length, CATEGORY_COUNT); i++) {
                    volumes[i] = clamp(loaded[i]);
                }
            }
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
        return tag;
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection connection,
                             net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt,
                             HolderLookup.Provider registries) {
        super.onDataPacket(connection, pkt, registries);
        if (pkt.getTag() != null && pkt.getTag().contains("Volumes", net.minecraft.nbt.Tag.TAG_INT_ARRAY)) {
            int[] loaded = pkt.getTag().getIntArray("Volumes");
            for (int i = 0; i < Math.min(loaded.length, CATEGORY_COUNT); i++) {
                volumes[i] = clamp(loaded[i]);
            }
        }
    }
}

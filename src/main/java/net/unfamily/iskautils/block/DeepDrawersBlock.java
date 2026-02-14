package net.unfamily.iskautils.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.unfamily.iskautils.block.entity.DeepDrawersBlockEntity;
import org.jetbrains.annotations.Nullable;


/**
 * Deep Drawers Block - A massive storage block that can only contain non-stackable items
 * 
 * Features:
 * - Configurable slot count (default: 49995 slots = 9 x 5555, close to 50000)
 * - Only accepts items matching tags/IDs defined in configuration
 * - Blacklist system to forbid specific items (overrides allowed list)
 * - Tags starting with # are item tags (e.g. #c:enchantables)
 * - Items without # are item IDs (e.g. apotheosis:gem)
 * - Cannot be broken if it contains items (protection implemented)
 * - Contents are NEVER dropped when broken (prevents lag from massive item drops)
 * 
 * Configuration:
 * - deep_drawers_slot_count_v2: Number of storage slots (default 1024)
 * - deep_drawers_allowed_tags: List of allowed item tags/IDs
 * - deep_drawers_blacklist: List of forbidden item tags/IDs (default: minecraft:book)
 * 
 * Security Features:
 * - Block cannot be broken if it contains items (prevented via PlayerInteractEvent.LeftClickBlock handler)
 * - Additional protection via getDestroyProgress() returning -1 when items are present
 * - If somehow broken with items, contents are destroyed (not dropped) to prevent lag
 */
public class DeepDrawersBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final MapCodec<DeepDrawersBlock> CODEC = simpleCodec(DeepDrawersBlock::new);
    
    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public DeepDrawersBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH));
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
    
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        return this.defaultBlockState()
                .setValue(FACING, facing);
    }
    
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DeepDrawersBlockEntity(pos, state);
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> blockEntityType) {
        return createTickerHelper(
                blockEntityType,
                net.unfamily.iskautils.block.entity.ModBlockEntities.DEEP_DRAWERS_BE.get(),
                DeepDrawersBlockEntity::tick
        );
    }
    
    @Nullable
    protected static <E extends BlockEntity, A extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<A> createTickerHelper(
            net.minecraft.world.level.block.entity.BlockEntityType<A> typeCheck, net.minecraft.world.level.block.entity.BlockEntityType<E> typeExpected, net.minecraft.world.level.block.entity.BlockEntityTicker<? super E> ticker) {
        return typeExpected == typeCheck ? (net.minecraft.world.level.block.entity.BlockEntityTicker<A>) ticker : null;
    }
    
    /**
     * Called when a player starts attacking/breaking the block
     * NOTE: The primary protection is via DeepDrawerBreakEventHandler which uses
     * PlayerInteractEvent.LeftClickBlock to prevent breaking before it starts.
     * This method is kept as a secondary safety layer.
     */
    @Override
    public void attack(BlockState state, Level level, BlockPos pos, Player player) {
        // Secondary check: if somehow the event handler didn't catch it
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof DeepDrawersBlockEntity deepDrawers) {
            if (deepDrawers.hasItems() && !level.isClientSide()) {
                // Display warning message to player
                player.displayClientMessage(
                    Component.translatable("message.iska_utils.deep_drawers.cannot_break"),
                    true
                );
            }
        }
        super.attack(state, level, pos, player);
    }
    
    /**
     * Called when a player attempts to break the block
     * Returns the time it takes to break, or -1 if it cannot be broken
     * 
     * NOTE: This is a secondary protection layer. The primary protection is via
     * DeepDrawerBreakEventHandler which cancels PlayerInteractEvent.LeftClickBlock when items are present.
     * This method provides additional safety in case the event handler doesn't catch it.
     */
    @Override
    public float getDestroyProgress(BlockState state, Player player, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        // Check if the block entity has items
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof DeepDrawersBlockEntity deepDrawers) {
            if (deepDrawers.hasItems()) {
                return -1.0F; // Cannot be broken
            }
        }
        
        // If empty, allow normal breaking
        return super.getDestroyProgress(state, player, level, pos);
    }
    
    /**
     * Called when the player right-clicks the block
     * Opens the Deep Drawers GUI
     */
    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, 
                                                                     Player player, net.minecraft.world.phys.BlockHitResult hitResult) {
        if (!level.isClientSide() && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof DeepDrawersBlockEntity deepDrawers) {
                // Debug extraction: Shift+Right-click to extract one item (configurable, default disabled)
                if (net.unfamily.iskautils.Config.deepDrawersDebugExtractionEnabled && player.isCrouching() && player.isShiftKeyDown()) {
                    java.util.Map.Entry<Integer, net.minecraft.world.item.ItemStack> firstEntry = deepDrawers.getFirstStorageEntry();
                    if (firstEntry != null) {
                        net.minecraft.world.item.ItemStack extracted = deepDrawers.extractItemFromPhysicalSlot(firstEntry.getKey(), 1, false);
                        if (!extracted.isEmpty()) {
                            if (!player.getInventory().add(extracted)) {
                                // Inventory full, drop item
                                player.drop(extracted, false);
                            }
                            serverPlayer.displayClientMessage(
                                net.minecraft.network.chat.Component.translatable("message.iska_utils.deep_drawers.debug_extracted", extracted.getDisplayName()),
                                true); // true = actionbar
                        }
                    } else {
                        serverPlayer.displayClientMessage(
                            net.minecraft.network.chat.Component.translatable("message.iska_utils.deep_drawers.empty"),
                            true); // true = actionbar
                    }
                    return net.minecraft.world.InteractionResult.CONSUME;
                }
                
                // Show status message (actionbar) if:
                // - Shift+click (always), OR
                // - Normal click when GUI is disabled
                boolean showStatus = player.isShiftKeyDown() || true; // GUI always disabled
                
                if (showStatus) {
                    boolean isFull = deepDrawers.isFull();
                    if (isFull) {
                        serverPlayer.displayClientMessage(
                            net.minecraft.network.chat.Component.translatable("message.iska_utils.deep_drawers.full"),
                            true); // true = actionbar
                    } else {
                        int occupiedSlots = deepDrawers.getOccupiedSlotsCount();
                        int totalSlots = deepDrawers.getMaxSlots();
                        serverPlayer.displayClientMessage(
                            net.minecraft.network.chat.Component.translatable("message.iska_utils.deep_drawers.status", occupiedSlots, totalSlots),
                            true); // true = actionbar
                    }
                    return net.minecraft.world.InteractionResult.CONSUME;
                }
            }
        }
        return net.minecraft.world.InteractionResult.SUCCESS;
    }
    
    /**
     * Called when the block is removed
     * IMPORTANT: Contents are NOT dropped to prevent lag
     */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        // Only process if the block is actually being removed (not just state change)
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            
            if (blockEntity instanceof DeepDrawersBlockEntity deepDrawers) {
                // Close any open GUIs
                while (deepDrawers.isGuiOpen()) {
                    deepDrawers.onGuiClosed();
                }
                
                // SECURITY: Do NOT drop items - clear storage to prevent lag
                // This should never happen due to break protection, but is a failsafe
                if (deepDrawers.hasItems()) {
                    // Log warning if this happens (shouldn't be possible)
                    org.slf4j.LoggerFactory.getLogger(DeepDrawersBlock.class).warn(
                        "Deep Drawers at {} was broken with items inside! Contents destroyed to prevent lag. " +
                        "This should not happen due to break protection.",
                        pos
                    );
                    
                    // Clear storage without dropping (prevents lag)
                    deepDrawers.clearStorage();
                }
            }
        }
        
        super.onRemove(state, level, pos, newState, isMoving);
    }
}


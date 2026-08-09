package net.unfamily.iskautils.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.entity.EtherealFrameBlockEntity;
import net.unfamily.iskautils.block.entity.ModBlockEntities;
import net.unfamily.iskautils.client.gui.EtherealFrameFilterMenu;

import javax.annotation.Nullable;

/**
 * Ethereal Frame: a transparent block whose entity pass-through is governed by a configurable
 * allow/deny list stored in its Block Entity. Supports camouflage (mimics another block's appearance).
 */
public class EtherealFrameBlock extends Block implements EntityBlock {

    public static final MapCodec<EtherealFrameBlock> CODEC = simpleCodec(EtherealFrameBlock::new);
    public static final BooleanProperty CAMOUFLAGED = BooleanProperty.create("camouflaged");
    /** When true, behaves like tinted glass for light; when false, like clear glass. */
    public static final BooleanProperty BLOCKS_LIGHT = BooleanProperty.create("blocks_light");
    /** Visual + gameplay flag for wither-proof reinforcement (effective only if config enabled). */
    public static final BooleanProperty REINFORCED = BooleanProperty.create("reinforced");

    public static final TagKey<Block> REINFORCEMENT_MATERIALS = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "ethereal_frame_reinforcement"));

    public static final float BASE_DESTROY_SPEED = 2.0f;
    public static final float BASE_BLAST_RESISTANCE = 3.0f;
    public static final float REINFORCED_DESTROY_SPEED = 3.0f;
    public static final float REINFORCED_BLAST_RESISTANCE = 1200.0f;

    public EtherealFrameBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(CAMOUFLAGED, false)
                .setValue(BLOCKS_LIGHT, false)
                .setValue(REINFORCED, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CAMOUFLAGED, BLOCKS_LIGHT, REINFORCED);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return state.getValue(CAMOUFLAGED) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    // ==================== GLASS-LIKE LIGHT / RENDER ====================

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return !state.getValue(BLOCKS_LIGHT);
    }

    @Override
    protected int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(BLOCKS_LIGHT) ? level.getMaxLightLevel() : 0;
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacentState, Direction side) {
        return isGlassLikeFaceCullNeighbor(adjacentState) || super.skipRendering(state, adjacentState, side);
    }

    @Override
    public boolean hidesNeighborFace(BlockGetter level, BlockPos pos, BlockState state,
                                     BlockState neighborState, Direction dir) {
        if (!isGlassLikeFaceCullNeighbor(neighborState)) {
            return false;
        }
        if (!state.getValue(CAMOUFLAGED)) {
            return true;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof EtherealFrameBlockEntity frame && frame.getCamouflage() != null) {
            return isGlassLikeFaceCullNeighbor(frame.getCamouflage());
        }
        return true;
    }

    /** Neighbors whose touching faces should cull against ethereal frames / glass. */
    public static boolean isGlassLikeFaceCullNeighbor(BlockState state) {
        Block block = state.getBlock();
        return block instanceof EtherealFrameBlock
                || block instanceof HalfTransparentBlock
                || block instanceof IronBarsBlock;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EtherealFrameBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return createTickerHelper(
                blockEntityType,
                ModBlockEntities.ETHEREAL_FRAME_BE.get(),
                EtherealFrameBlockEntity::serverTick
        );
    }

    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> typeCheck, BlockEntityType<E> typeExpected, BlockEntityTicker<? super E> ticker) {
        return typeExpected == typeCheck ? (BlockEntityTicker<A>) ticker : null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof EtherealFrameBlockEntity frame) {
                frame.pullFromNetworkIfNewer();
            }
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof EtherealFrameBlockEntity frame) {
                frame.pullFromNetworkIfNewer();
            }
        }
    }

    // ==================== COLLISION (filter-based pass-through) ====================

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context instanceof EntityCollisionContext entityCtx) {
            Entity entity = entityCtx.getEntity();
            if (entity != null) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof EtherealFrameBlockEntity frame && frame.shouldEntityPass(entity)) {
                    return Shapes.empty();
                }
            }
        }
        return Shapes.block();
    }

    // ==================== APPEARANCE (Fusion CTM / neighbor queries) ====================

    @Override
    public BlockState getAppearance(BlockState state, BlockAndTintGetter level, BlockPos pos,
                                    Direction side, @Nullable BlockState queryState, @Nullable BlockPos queryPos) {
        if (state.getValue(CAMOUFLAGED)) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof EtherealFrameBlockEntity frame) {
                BlockState camouflage = frame.getCamouflage();
                if (camouflage != null) return camouflage;
            }
        }
        return state;
    }

    // ==================== REINFORCEMENT HARDNESS / IMMUNITY ====================

    private static boolean isEffectivelyReinforced(BlockGetter level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof EtherealFrameBlockEntity frame && frame.isEffectivelyReinforced();
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        float destroySpeed = isEffectivelyReinforced(level, pos) ? REINFORCED_DESTROY_SPEED : BASE_DESTROY_SPEED;
        if (destroySpeed == -1.0F) {
            return 0.0F;
        }
        int i = net.neoforged.neoforge.event.EventHooks.doPlayerHarvestCheck(player, state, level, pos) ? 30 : 100;
        return player.getDigSpeed(state, pos) / destroySpeed / (float) i;
    }

    @Override
    public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        return isEffectivelyReinforced(level, pos) ? REINFORCED_BLAST_RESISTANCE : BASE_BLAST_RESISTANCE;
    }

    @Override
    public boolean canEntityDestroy(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        if (isEffectivelyReinforced(level, pos)
                && (entity instanceof WitherBoss || entity instanceof EnderDragon)) {
            return false;
        }
        return super.canEntityDestroy(state, level, pos, entity);
    }

    // ==================== INTERACTIONS ====================

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                              BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        Block heldBlock = blockItem.getBlock();
        if (heldBlock instanceof EtherealFrameBlock) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof EtherealFrameBlockEntity frame)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (player.isShiftKeyDown()) {
            if (frame.hasCamouflage()) {
                if (!level.isClientSide) {
                    ItemStack drop = new ItemStack(frame.getCamouflage().getBlock());
                    frame.clearCamouflage(level, pos, state);
                    Block.popResourceFromFace(level, pos, hit.getDirection(), drop);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
            if (!level.isClientSide) {
                if (frame.tryStripReinforcementNetwork(player, hit.getDirection()) > 0) {
                    return ItemInteractionResult.sidedSuccess(false);
                }
            } else if (frame.isReinforced()) {
                return ItemInteractionResult.sidedSuccess(true);
            }
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // Apply camouflage if not yet camouflaged and held block is a valid full block
        if (!frame.hasCamouflage()) {
            if (!level.isClientSide) {
                BlockState placedState = getPlacementState(heldBlock, player, hand, hit, stack);
                if (placedState != null && placedState.isCollisionShapeFullBlock(level, pos)) {
                    boolean isAnimatedBE = heldBlock instanceof BaseEntityBlock
                            && heldBlock.defaultBlockState().getRenderShape() == RenderShape.ENTITYBLOCK_ANIMATED;
                    if (!isAnimatedBE) {
                        frame.setCamouflage(placedState, level, pos, state);
                        if (!player.isCreative()) stack.shrink(1);
                    }
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        // Already camouflaged: open filter GUI
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            openFilterGui(serverPlayer, pos, frame);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof EtherealFrameBlockEntity frame)) {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown()) {
            if (frame.hasCamouflage()) {
                if (!level.isClientSide) {
                    ItemStack drop = new ItemStack(frame.getCamouflage().getBlock());
                    frame.clearCamouflage(level, pos, state);
                    Block.popResourceFromFace(level, pos, hit.getDirection(), drop);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            if (!level.isClientSide) {
                if (frame.tryStripReinforcementNetwork(player, hit.getDirection()) > 0) {
                    return InteractionResult.sidedSuccess(false);
                }
            } else if (frame.isReinforced()) {
                return InteractionResult.sidedSuccess(true);
            }
        }
        // Empty hand: open filter GUI
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            openFilterGui(serverPlayer, pos, frame);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private void openFilterGui(ServerPlayer player, BlockPos pos, EtherealFrameBlockEntity frame) {
        player.openMenu(new net.minecraft.world.MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("block.iska_utils.ethereal_frame");
            }
            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                return new EtherealFrameFilterMenu(id, inv, pos);
            }
        }, pos);
    }

    // ==================== DROPS ====================

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof EtherealFrameBlockEntity frame) {
                if (frame.hasCamouflage()) {
                    Block.dropResources(frame.getCamouflage(), level, pos, null);
                }
                if (frame.isReinforced() && !player.getAbilities().instabuild) {
                    frame.dropReinforcementMaterial();
                }
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    // ==================== HELPERS ====================

    @Nullable
    private BlockState getPlacementState(Block block, Player player, InteractionHand hand,
                                         BlockHitResult hit, ItemStack stack) {
        try {
            BlockPlaceContext ctx = new BlockPlaceContext(new UseOnContext(player, hand, hit));
            BlockState placed = block.getStateForPlacement(ctx);
            return placed != null ? placed : block.defaultBlockState();
        } catch (Exception e) {
            return block.defaultBlockState();
        }
    }
}

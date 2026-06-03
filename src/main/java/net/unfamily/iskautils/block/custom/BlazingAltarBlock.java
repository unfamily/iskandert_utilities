package net.unfamily.iskautils.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.unfamily.iskautils.block.BlazingAltarFlameVisual;
import net.unfamily.iskautils.block.entity.BlazingAltarBlockEntity;
import net.unfamily.iskautils.block.entity.BlazingAltarBlockSync;
import net.unfamily.iskautils.block.entity.ModBlockEntities;
import net.unfamily.iskautils.client.gui.BlazingAltarMenu;

import javax.annotation.Nullable;

public class BlazingAltarBlock extends BaseEntityBlock {
    public static final MapCodec<BlazingAltarBlock> CODEC = simpleCodec(BlazingAltarBlock::new);

    public static final EnumProperty<BlazingAltarFlameVisual> FLAME_VISUAL =
            EnumProperty.create("flame_visual", BlazingAltarFlameVisual.class);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    /** Pedestal only (base_base + base + burning_area in Blockbench model), excludes flame cross. */
    private static final VoxelShape BASE_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 4.0, 16.0);

    public BlazingAltarBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FLAME_VISUAL, BlazingAltarFlameVisual.HIDDEN)
                .setValue(POWERED, Boolean.FALSE));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FLAME_VISUAL, POWERED);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return BASE_SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return BASE_SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return direction != null;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlazingAltarBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.BLAZING_ALTAR_BE.get(), BlazingAltarBlockEntity::tickServer);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof BlazingAltarBlockEntity altar) {
                altar.prepareForRemoval();
                if (level instanceof ServerLevel serverLevel) {
                    altar.enqueueFlameCleanupOnBreak(serverLevel);
                }
                if (!player.isCreative()) {
                double x = pos.getX() + 0.5;
                double y = pos.getY() + 0.5;
                double z = pos.getZ() + 0.5;
                ItemStackHandler placer = altar.getPlacerHandler();
                for (int i = 0; i < placer.getSlots(); i++) {
                    ItemStack stack = placer.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        Containers.dropItemStack(level, x, y, z, stack.copy());
                        placer.setStackInSlot(i, ItemStack.EMPTY);
                    }
                }
                ItemStackHandler module = altar.getModuleHandler();
                for (int i = 0; i < module.getSlots(); i++) {
                    ItemStack stack = module.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        Containers.dropItemStack(level, x, y, z, stack.copy());
                        module.setStackInSlot(i, ItemStack.EMPTY);
                    }
                }
                }
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && !movedByPiston && !oldState.is(this)) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof BlazingAltarBlockEntity altar) {
                altar.onPlaced();
            }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof BlazingAltarBlockEntity altar)) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return altar.getDisplayName();
                }

                @Override
                public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                    return new BlazingAltarMenu(id, inv, altar);
                }
            }, pos);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, fromPos, movedByPiston);
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof BlazingAltarBlockEntity altar) {
                BlazingAltarBlockSync.sync(altar);
            }
        }
    }
}

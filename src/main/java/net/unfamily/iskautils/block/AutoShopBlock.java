package net.unfamily.iskautils.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.unfamily.iskautils.block.entity.ModBlockEntities;
import net.unfamily.iskautils.block.entity.AutoShopBlockEntity;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;

/**
 * Block for Auto Shop
 */
public class AutoShopBlock extends BaseEntityBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public static final MapCodec<AutoShopBlock> CODEC = simpleCodec(AutoShopBlock::new);

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public AutoShopBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        net.minecraft.core.Direction facing = context.getHorizontalDirection().getOpposite();
        return this.defaultBlockState()
                .setValue(FACING, facing);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return direction != null;
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AutoShopBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        if (!level.isClientSide() && placer instanceof ServerPlayer serverPlayer) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof AutoShopBlockEntity autoShopEntity) {
                autoShopEntity.setPlacedByPlayer(serverPlayer.getUUID());
                autoShopEntity.ensureDefaultCurrency();

                net.unfamily.iskalib.team.ShopTeamManager teamManager =
                    net.unfamily.iskalib.team.ShopTeamManager.getInstance((net.minecraft.server.level.ServerLevel) serverPlayer.level());
                String teamKey = teamManager.getPlayerTeamKey(serverPlayer);
                if (teamKey != null) {
                    UUID teamId = teamManager.getTeamIdByName(teamKey);
                    if (teamId != null) {
                        autoShopEntity.setOwnerTeamId(teamId);
                    }
                }
            }
        }
    }

    /**
     * Bucket / fluid-container / chemical-tank click: fill AutoShop tanks from item or drain tanks into item.
     */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof AutoShopBlockEntity autoShop)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (player instanceof ServerPlayer serverPlayer && !autoShop.canPlayerUse(serverPlayer)) {
            player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable(
                    "block.iska_utils.auto_shop.team.error"));
            return InteractionResult.FAIL;
        }

        ItemAccess itemAccess = ItemAccess.forPlayerInteraction(player, hand).oneByOne();
        ResourceHandler<FluidResource> fluidHandler = itemAccess.getCapability(Capabilities.Fluid.ITEM);
        if (fluidHandler != null && autoShop.interactWithItemFluidHandler(fluidHandler, player)) {
            return InteractionResult.SUCCESS_SERVER;
        }
        ItemStack singleStack = stack.copyWithCount(1);
        if (autoShop.interactWithItemChemicalHandler(singleStack, player)) {
            applyHandAfterItemTransfer(player, hand, stack, singleStack);
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    private static void applyHandAfterItemTransfer(Player player, InteractionHand hand,
                                                   ItemStack held, ItemStack result) {
        held.shrink(1);
        if (held.isEmpty()) {
            player.setItemInHand(hand, result);
        } else {
            player.setItemInHand(hand, held);
            if (!player.getInventory().add(result)) {
                player.drop(result, false);
            }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity entity = level.getBlockEntity(pos);
        if (!(entity instanceof AutoShopBlockEntity autoShop)) {
            return InteractionResult.PASS;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            if (!autoShop.canPlayerUse(serverPlayer)) {
                player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("block.iska_utils.auto_shop.team.error"));
                return InteractionResult.FAIL;
            }

            serverPlayer.openMenu(new net.minecraft.world.MenuProvider() {
                @Override
                public net.minecraft.network.chat.Component getDisplayName() {
                    return net.minecraft.network.chat.Component.translatable("block.iska_utils.auto_shop");
                }

                @Override
                public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inv, Player player) {
                    return new net.unfamily.iskautils.client.gui.AutoShopMenu(id, inv, autoShop);
                }
            }, pos);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return null;
        }

        return createTickerHelper(blockEntityType, ModBlockEntities.AUTO_SHOP_BE.get(),
                AutoShopBlockEntity::tick);
    }
}

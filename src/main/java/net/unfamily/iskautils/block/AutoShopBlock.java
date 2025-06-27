package net.unfamily.iskautils.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.unfamily.iskautils.block.entity.ModBlockEntities;
import net.unfamily.iskautils.block.entity.AutoShopBlockEntity;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.items.ItemStackHandler;
import java.util.UUID;

/**
 * Block for Auto Shop
 */
public class AutoShopBlock extends BaseEntityBlock {
    
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    
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
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }
    
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
    
    @Override
    public SoundType getSoundType(BlockState state) {
        return SoundType.METAL;
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AutoShopBlockEntity(pos, state);
    }
    
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        
        // Save the player who placed this Auto Shop and their team
        if (!level.isClientSide() && placer instanceof ServerPlayer serverPlayer) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof AutoShopBlockEntity autoShopEntity) {
                autoShopEntity.setPlacedByPlayer(serverPlayer.getUUID());
                
                // Save also the team ID of the player if they belong to a team
                net.unfamily.iskautils.shop.ShopTeamManager teamManager = 
                    net.unfamily.iskautils.shop.ShopTeamManager.getInstance(serverPlayer.serverLevel());
                String teamName = teamManager.getPlayerTeam(serverPlayer);
                if (teamName != null) {
                    UUID teamId = teamManager.getTeamIdByName(teamName);
                    if (teamId != null) {
                        autoShopEntity.setOwnerTeamId(teamId);
                    }
                }
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
        
        // Check if the player can use this AutoShop
        if (player instanceof ServerPlayer serverPlayer) {
            if (!autoShop.canPlayerUse(serverPlayer)) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("You cannot use this AutoShop. You must belong to the team that placed it."));
                return InteractionResult.FAIL;
            }
        }
        
        // Shift + right click: change currency
        if (player.isShiftKeyDown()) {
            cycleCurrencies(autoShop, player);
            return InteractionResult.SUCCESS;
        }
        
        // Normal right click: open GUI
        if (player instanceof ServerPlayer serverPlayer) {
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
    
    /**
     * Cycles through available currencies
     */
    private void cycleCurrencies(AutoShopBlockEntity autoShop, Player player) {
                Map<String, net.unfamily.iskautils.shop.ShopCurrency> availableCurrencies =
                net.unfamily.iskautils.shop.ShopLoader.getCurrencies();
        List<String> currencyIds = new ArrayList<>();
        currencyIds.add("unset"); // First option
        currencyIds.addAll(availableCurrencies.keySet());
        
        String currentCurrency = autoShop.getSelectedValute();
        int currentIndex = currencyIds.indexOf(currentCurrency);
        if (currentIndex == -1) currentIndex = 0;
        
        // Move to next currency
        int nextIndex = (currentIndex + 1) % currencyIds.size();
        String newCurrency = currencyIds.get(nextIndex);
        
        autoShop.setSelectedValute(newCurrency);
        
        // Feedback message with translation and symbol
        if ("unset".equals(newCurrency)) {
            player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("block.iska_utils.auto_shop.currency_changed.unset"),
                true);
        } else {
            net.unfamily.iskautils.shop.ShopCurrency currency = availableCurrencies.get(newCurrency);
            String currencyName = net.minecraft.network.chat.Component.translatable(currency.name).getString();
            String currencySymbol = currency.charSymbol != null ? currency.charSymbol : newCurrency;
            
            // Concatenate symbol at the end of translated message
            String fullMessage = net.minecraft.network.chat.Component.translatable("block.iska_utils.auto_shop.currency_changed", currencyName).getString() + " " + currencySymbol;
            
            player.displayClientMessage(
                net.minecraft.network.chat.Component.literal(fullMessage),
                true);
        }
    }
    
    /**
     * Toggles between Auto Buy and Auto Sell mode
     */
    private void toggleAutoMode(AutoShopBlockEntity autoShop, Player player) {
        autoShop.toggleAutoMode();
        String modeName = autoShop.isAutoBuyMode() ? "Auto Buy" : "Auto Sell";
        player.displayClientMessage(
            net.minecraft.network.chat.Component.translatable("block.iska_utils.auto_shop.mode_changed", modeName),
            true
        );
    }
    
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof AutoShopBlockEntity autoShopEntity) {

            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
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

    @Override
	public void attack(BlockState blockstate, Level world, BlockPos pos, Player entity) {
        if (!world.isClientSide()) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof AutoShopBlockEntity autoShop) {
                
                if (entity instanceof ServerPlayer serverPlayer) {
                    if (!autoShop.canPlayerUse(serverPlayer)) {
                        entity.sendSystemMessage(net.minecraft.network.chat.Component.translatable("block.iska_utils.auto_shop.team.error"));
                    }
                }
                
                // Left click: toggle Buy/Sell mode
                toggleAutoMode(autoShop, entity);
            }
        }
		
		super.attack(blockstate, world, pos, entity);
	}
} 
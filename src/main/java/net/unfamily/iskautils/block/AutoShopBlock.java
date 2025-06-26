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

/**
 * Blocco per l'Auto Shop
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
        
        // Salva il giocatore che ha piazzato questo Auto Shop
        if (!level.isClientSide() && placer instanceof ServerPlayer serverPlayer) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof AutoShopBlockEntity autoShopEntity) {
                autoShopEntity.setPlacedByPlayer(serverPlayer.getUUID());
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
        
        // Shift + tasto destro: cambia valuta
        if (player.isShiftKeyDown()) {
            cycleValute(autoShop, player);
            return InteractionResult.SUCCESS;
        }
        
        // Tasto destro normale: apri la GUI
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
     * Cicla tra le valute disponibili
     */
    private void cycleValute(AutoShopBlockEntity autoShop, Player player) {
        Map<String, net.unfamily.iskautils.shop.ShopValute> availableValutes = 
            net.unfamily.iskautils.shop.ShopLoader.getValutes();
        List<String> valuteIds = new ArrayList<>();
        valuteIds.add("unset"); // Prima opzione
        valuteIds.addAll(availableValutes.keySet());
        
        String currentValute = autoShop.getSelectedValute();
        int currentIndex = valuteIds.indexOf(currentValute);
        if (currentIndex == -1) currentIndex = 0;
        
        // Passa alla prossima valuta
        int nextIndex = (currentIndex + 1) % valuteIds.size();
        String newValute = valuteIds.get(nextIndex);
        
        autoShop.setSelectedValute(newValute);
        
        // Messaggio di feedback con traduzione e simbolo
        if ("unset".equals(newValute)) {
            player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("block.iska_utils.auto_shop.valute_changed.unset"),
                true
            );
        } else {
            net.unfamily.iskautils.shop.ShopValute valute = availableValutes.get(newValute);
            String valuteName = net.minecraft.network.chat.Component.translatable(valute.name).getString();
            String valuteSymbol = valute.charSymbol != null ? valute.charSymbol : newValute;
            
            // Concatena il simbolo alla fine del messaggio tradotto
            String fullMessage = net.minecraft.network.chat.Component.translatable("block.iska_utils.auto_shop.valute_changed", valuteName).getString() + " " + valuteSymbol;
            
            player.displayClientMessage(
                net.minecraft.network.chat.Component.literal(fullMessage),
                true
            );
        }
    }
    
    /**
     * Cambia la modalità tra Auto Buy e Auto Sell
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
                // TODO: Implementare drops se necessario
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
                // Tasto sinistro: cambia modalità Buy/Sell
                toggleAutoMode(autoShop, entity);
            }
        }
		
		super.attack(blockstate, world, pos, entity);
	}
} 
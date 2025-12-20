package net.unfamily.iskautils.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.unfamily.iskautils.block.entity.DeepDrawerExtractorBlockEntity;
import net.unfamily.iskautils.block.entity.ModBlockEntities;
import org.jetbrains.annotations.Nullable;

/**
 * Blocco estrattore per Deep Drawer
 * Estrae item dal Deep Drawer adiacente usando l'API ottimizzata
 */
public class DeepDrawerExtractorBlock extends BaseEntityBlock {
    
    public static final MapCodec<DeepDrawerExtractorBlock> CODEC = simpleCodec(DeepDrawerExtractorBlock::new);
    
    public DeepDrawerExtractorBlock(Properties properties) {
        super(properties);
    }
    
    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DeepDrawerExtractorBlockEntity(pos, state);
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(blockEntityType, ModBlockEntities.DEEP_DRAWER_EXTRACTOR.get(), 
                DeepDrawerExtractorBlockEntity::serverTick);
    }
    
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
    
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        
        BlockEntity entity = level.getBlockEntity(pos);
        if (!(entity instanceof DeepDrawerExtractorBlockEntity extractor)) {
            return InteractionResult.PASS;
        }
        
        // Shift+click: show buffer status (actionbar message)
        if (player instanceof ServerPlayer serverPlayer && player.isShiftKeyDown()) {
            int occupiedSlots = extractor.getOccupiedSlots();
            int totalSlots = extractor.getTotalSlots();
            serverPlayer.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("message.iska_utils.deep_drawer_extractor.buffer_status", occupiedSlots, totalSlots),
                true); // true = actionbar
            return InteractionResult.CONSUME;
        }
        
        // Normal click: open GUI
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new net.minecraft.world.MenuProvider() {
                @Override
                public net.minecraft.network.chat.Component getDisplayName() {
                    return net.minecraft.network.chat.Component.translatable("block.iska_utils.deep_drawer_extractor");
                }
                
                @Override
                public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inv, Player player) {
                    return new net.unfamily.iskautils.client.gui.DeepDrawerExtractorMenu(id, inv, extractor);
                }
            }, pos);
        }
        
        return InteractionResult.CONSUME;
    }
}

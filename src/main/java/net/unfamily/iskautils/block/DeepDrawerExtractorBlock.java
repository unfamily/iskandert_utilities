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
        
            // Mostra informazioni sullo stato dell'estrattore
            if (level.getBlockEntity(pos) instanceof DeepDrawerExtractorBlockEntity extractor) {
                // Per ora solo feedback testuale, in futuro si può aggiungere GUI
                if (player instanceof ServerPlayer) {
                    // Mostra contenuto inventario
                    int filledSlots = 0;
                    for (int i = 0; i < 5; i++) {
                        if (!extractor.getItem(i).isEmpty()) {
                            filledSlots++;
                        }
                    }
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§6Deep Drawer Extractor§r\n" +
                        "Slot occupati: " + filledSlots + "/5"
                    ));
                }
            }
        
        return InteractionResult.CONSUME;
    }
}

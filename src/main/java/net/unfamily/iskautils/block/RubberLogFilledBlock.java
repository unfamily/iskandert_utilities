package net.unfamily.iskautils.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.block.entity.RubberLogEmptyBlockEntity;
import net.unfamily.iskautils.item.ModItems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Blocco di legno di gomma con sap pieno.
 * Direzionale verso i 4 punti cardinali.
 * Quando cliccato con un treetap, rilascia sap e diventa un blocco vuoto.
 */
public class RubberLogFilledBlock extends HorizontalDirectionalBlock {
    private static final Logger LOGGER = LoggerFactory.getLogger(RubberLogFilledBlock.class);
    public static final MapCodec<RubberLogFilledBlock> CODEC = simpleCodec(RubberLogFilledBlock::new);
    
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public RubberLogFilledBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }
    
    @Override
    public MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
    
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }
    
    /**
     * Implementazione alternativa utilizzando un metodo non ereditato ma nostro personalizzato
     * Verrà chiamato dal TreeTapItem invece di usare il metodo use standard
     */
    public InteractionResult onTapWithTreeTap(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand) {
        ItemStack itemInHand = player.getItemInHand(hand);
        
        // Solo sul server
        if (!level.isClientSide()) {
            // Danneggia il treetap se è danneggiabile e non siamo in creative
            if (itemInHand.isDamageableItem() && !player.getAbilities().instabuild) {
                // Danneggia l'item di 1
                int newDamage = itemInHand.getDamageValue() + 1;
                if (newDamage >= itemInHand.getMaxDamage()) {
                    // Se l'oggetto si romperebbe, rimuovilo
                    itemInHand.setCount(0);
                } else {
                    // Altrimenti aggiungi danno
                    itemInHand.setDamageValue(newDamage);
                }
            }
            
            // Rilascia un sap
            ItemStack sapStack = new ItemStack(ModItems.SAP.get());
            if (!player.getInventory().add(sapStack)) {
                player.drop(sapStack, false);
            }
            
            // Converti in un blocco vuoto
            int refillTime = Config.MIN_SAP_REFILL_TIME.get() + 
                    level.getRandom().nextInt(Config.MAX_SAP_REFILL_TIME.get() - Config.MIN_SAP_REFILL_TIME.get());
                    
            Direction facing = state.getValue(FACING);
            BlockState emptyState = ModBlocks.RUBBER_LOG_EMPTY.get().defaultBlockState()
                    .setValue(RubberLogEmptyBlock.FACING, facing);
                    
            level.setBlock(pos, emptyState, Block.UPDATE_ALL);
            
            // Ottieni il blocco entità e imposta il tempo di ricarica
            if (level.getBlockEntity(pos) instanceof RubberLogEmptyBlockEntity blockEntity) {
                blockEntity.setRefillTime(refillTime);
            }
        }
        
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
} 
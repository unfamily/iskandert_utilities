package net.unfamily.iskautils.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.block.RubberLogFilledBlock;
import net.unfamily.iskautils.item.ModItems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TreeTapItem extends Item {
    private static final Logger LOGGER = LoggerFactory.getLogger(TreeTapItem.class);

    public TreeTapItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        BlockState state = level.getBlockState(pos);
        Direction clickedFace = context.getClickedFace();
        ItemStack itemStack = context.getItemInHand();
        InteractionHand hand = context.getHand();
        
        // Log per debug
        if (state.is(ModBlocks.RUBBER_LOG_FILLED.get())) {
            LOGGER.info("Clicked on rubber log filled at {}", pos);
            LOGGER.info("BlockState: facing={}", state.getValue(RubberLogFilledBlock.FACING));
            LOGGER.info("Clicked face: {}", clickedFace);
        }
        
        // Check if the block is a filled rubber log
        if (state.is(ModBlocks.RUBBER_LOG_FILLED.get())) {
            // Check if the clicked face matches the facing direction
            if (clickedFace == state.getValue(RubberLogFilledBlock.FACING)) {
                // Utilizziamo il metodo personalizzato del blocco
                if (player != null) {
                    return ((RubberLogFilledBlock) state.getBlock()).onTapWithTreeTap(state, level, pos, player, hand);
                }
                
                return InteractionResult.sidedSuccess(level.isClientSide);
            } else {
                LOGGER.info("Clicked face {} does not match facing {}", clickedFace, state.getValue(RubberLogFilledBlock.FACING));
            }
        }
        
        return InteractionResult.PASS;
    }
} 
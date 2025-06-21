package net.unfamily.iskautils.events;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.minecraft.sounds.SoundEvents;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.ModBlocks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Event handler for log stripping functionality
 */
@EventBusSubscriber(modid = IskaUtils.MOD_ID)
public class LogStrippingEvent {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogStrippingEvent.class);

    @SubscribeEvent
    public static void onBlockInteract(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        Player player = event.getEntity();
        ItemStack itemStack = event.getItemStack();
        BlockState blockState = level.getBlockState(pos);

        // Check if player is using an axe
        if (!itemStack.is(ItemTags.AXES)) {
            return;
        }

        // Check if the block is one of our rubber logs that can be stripped
        Block strippedBlock = getStrippedBlock(blockState.getBlock());
        if (strippedBlock == null) {
            return;
        }

        // Cancel the event to prevent default behavior
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);

        // Only process on server side
        if (level.isClientSide()) {
            return;
        }

        // Create the stripped block state
        BlockState strippedState = strippedBlock.defaultBlockState();
        
        // Preserve the axis property if the original block has it
        if (blockState.hasProperty(BlockStateProperties.AXIS)) {
            strippedState = strippedState.setValue(BlockStateProperties.AXIS, 
                blockState.getValue(BlockStateProperties.AXIS));
        }

        // Set the new block state
        level.setBlock(pos, strippedState, 3);

        // Damage the axe if it's damageable and player is not in creative
        if (itemStack.isDamageableItem() && !player.getAbilities().instabuild) {
            itemStack.setDamageValue(itemStack.getDamageValue() + 1);
        }

        // Play sound effect
        level.playSound(null, pos, SoundEvents.AXE_STRIP, 
            player.getSoundSource(), 1.0F, 1.0F);

        LOGGER.debug("Stripped rubber log at {} for player {}", pos, player.getName().getString());
    }

    /**
     * Gets the corresponding stripped block for a given block
     */
    private static Block getStrippedBlock(Block originalBlock) {
        if (originalBlock == ModBlocks.RUBBER_LOG.get()) {
            return ModBlocks.STRIPPED_RUBBER_LOG.get();
        } else if (originalBlock == ModBlocks.RUBBER_WOOD.get()) {
            return ModBlocks.STRIPPED_RUBBER_WOOD.get();
        }
        return null;
    }
} 
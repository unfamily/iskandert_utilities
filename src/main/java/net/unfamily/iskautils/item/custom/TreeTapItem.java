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
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.block.RubberLogBlock;
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
        
        // Check if the block is a rubber log
        if (state.is(ModBlocks.RUBBER_LOG.get())) {
            // Check if the log has sap and is filled
            if (state.getValue(RubberLogBlock.HAS_SAP) && state.getValue(RubberLogBlock.SAP_FILLED)) {
                // Check if the clicked face matches the tap facing direction
                if (clickedFace == state.getValue(RubberLogBlock.TAP_FACING)) {
                    if (!level.isClientSide) {
                        // Empty the sap and drop a sap item
                        level.setBlock(pos, state.setValue(RubberLogBlock.SAP_FILLED, false), 3);
                        
                        // Create a new sap item
                        ItemStack sapStack = new ItemStack(ModItems.SAP.get(), 1);
                        
                        // Give the item to the player or drop it
                        if (player != null) {
                            if (!player.getInventory().add(sapStack)) {
                                player.drop(sapStack, false);
                            }
                        } else {
                            // Drop the item at the position
                            Block.popResource((ServerLevel) level, pos.relative(clickedFace), sapStack);
                        }
                        
                        // Check if this is a regular tree tap (has durability) and damage it
                        if (itemStack.isDamageableItem() && !player.getAbilities().instabuild) {
                            // Damage the item
                            int damage = itemStack.getDamageValue();
                            int maxDamage = itemStack.getMaxDamage();
                            
                            // Increment damage
                            damage++;
                            
                            // If the item would break, reduce to 1 size
                            if (damage >= maxDamage) {
                                itemStack.setCount(itemStack.getCount() - 1);
                                // Play break sound - removed problematic call
                            } else {
                                // Otherwise just set the damage
                                itemStack.setDamageValue(damage);
                            }
                        }
                    }
                    
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
            }
        }
        
        return InteractionResult.PASS;
    }
} 
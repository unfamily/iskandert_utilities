package net.unfamily.iskautils.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.unfamily.iskautils.block.entity.TemporalOverclockerBlockEntity;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Item to link blocks to the Temporal Overclocker
 */
public class TemporalOverclockerChipsetItem extends Item {
    
    public TemporalOverclockerChipsetItem(Properties properties) {
        super(properties);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        
        // Add description from lang file
        tooltip.add(Component.translatable("tooltip.iska_utils.temporal_overclocker_chip.desc0")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.iska_utils.temporal_overclocker_chip.desc1")
                .withStyle(ChatFormatting.GRAY));
    }
    
    @Override
    @NotNull
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        
        if (level.isClientSide || player == null) {
            return InteractionResult.SUCCESS;
        }
        
        BlockEntity be = level.getBlockEntity(pos);
        
        // If we click on a Temporal Overclocker, enter linking mode
        if (be instanceof TemporalOverclockerBlockEntity overclocker) {
            // Save the overclocker position in the stack
            CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            CompoundTag tag = customData.copyTag();
            if (tag.contains("LinkingOverclocker")) {
                // We're already linking, cancel
                tag.remove("LinkingOverclocker");
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                player.sendSystemMessage(Component.translatable("item.iska_utils.temporal_overclocker_chip.linking_cancelled"));
            } else {
                // Start linking
                tag.putLong("LinkingOverclocker", pos.asLong());
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                player.sendSystemMessage(Component.translatable("item.iska_utils.temporal_overclocker_chip.linking_started"));
            }
            return InteractionResult.CONSUME;
        }
        
        // If we're linking, try to link this block
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        if (tag.contains("LinkingOverclocker")) {
            long overclockerPosLong = tag.getLong("LinkingOverclocker");
            BlockPos overclockerPos = BlockPos.of(overclockerPosLong);
            
            BlockEntity overclockerBE = level.getBlockEntity(overclockerPos);
            if (overclockerBE instanceof TemporalOverclockerBlockEntity overclocker) {
                // Verify we're not trying to link the block to itself
                if (pos.equals(overclockerPos)) {
                    player.sendSystemMessage(Component.translatable("item.iska_utils.temporal_overclocker_chip.cannot_link_self"));
                    return InteractionResult.FAIL;
                }
                
                // Verify the target block is not air. It's acceptable if it doesn't have a BlockEntity.
                net.minecraft.world.level.block.state.BlockState targetState = level.getBlockState(pos);
                if (targetState.isAir()) {
                    player.sendSystemMessage(Component.translatable("item.iska_utils.temporal_overclocker_chip.link_failed"));
                    return InteractionResult.FAIL;
                }
                
                // If already linked, remove it
                if (overclocker.isLinked(pos)) {
                    if (overclocker.removeLinkedBlock(pos)) {
                        player.sendSystemMessage(Component.translatable("item.iska_utils.temporal_overclocker_chip.link_removed"));
                        // Keep the binding to the overclocker so the player can link more blocks
                    }
                } else {
                    // Check if it can be linked
                    String error = overclocker.canLinkBlock(pos);
                    if (error == null) {
                        // Try to add the link
                        if (overclocker.addLinkedBlock(pos)) {
                            player.sendSystemMessage(Component.translatable("item.iska_utils.temporal_overclocker_chip.link_success"));
                            // Keep the binding to the overclocker so the player can link more blocks
                        } else {
                            player.sendSystemMessage(Component.translatable("item.iska_utils.temporal_overclocker_chip.link_failed"));
                        }
                    } else {
                        // Show specific error message
                        switch (error) {
                            case "max_links" -> player.sendSystemMessage(Component.translatable("item.iska_utils.temporal_overclocker_chip.link_failed"));
                            case "too_far" -> player.sendSystemMessage(Component.translatable("item.iska_utils.temporal_overclocker_chip.link_too_far", net.unfamily.iskautils.Config.temporalOverclockerLinkRange));
                            case "already_linked" -> player.sendSystemMessage(Component.translatable("item.iska_utils.temporal_overclocker_chip.link_failed"));
                            default -> player.sendSystemMessage(Component.translatable("item.iska_utils.temporal_overclocker_chip.link_failed"));
                        }
                    }
                }
                return InteractionResult.CONSUME;
            } else {
                // The overclocker block no longer exists, cancel linking
                tag.remove("LinkingOverclocker");
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                player.sendSystemMessage(Component.translatable("item.iska_utils.temporal_overclocker_chip.overclocker_removed"));
            }
        }
        
        return InteractionResult.PASS;
    }
}


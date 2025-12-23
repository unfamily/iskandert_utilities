package net.unfamily.iskautils.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.unfamily.iskautils.block.entity.TemporalOverclockerBlockEntity;
import org.jetbrains.annotations.NotNull;

/**
 * Item per collegare blocchi al Temporal Overclocker
 */
public class TemporalOverclockerChipsetItem extends Item {
    
    public TemporalOverclockerChipsetItem(Properties properties) {
        super(properties);
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
        
        // Se clicchiamo su un Temporal Overclocker, entriamo in modalità linking
        if (be instanceof TemporalOverclockerBlockEntity overclocker) {
            // Salva la posizione del overclocker nello stack
            CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            CompoundTag tag = customData.copyTag();
            if (tag.contains("LinkingOverclocker")) {
                // Stiamo già linkando, cancella
                tag.remove("LinkingOverclocker");
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                player.sendSystemMessage(Component.translatable("item.iska_utils.temporal_overclocker_chipset.linking_cancelled"));
            } else {
                // Inizia il linking
                tag.putLong("LinkingOverclocker", pos.asLong());
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                player.sendSystemMessage(Component.translatable("item.iska_utils.temporal_overclocker_chipset.linking_started"));
            }
            return InteractionResult.CONSUME;
        }
        
        // Se stiamo linkando, prova a collegare questo blocco
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        if (tag.contains("LinkingOverclocker")) {
            long overclockerPosLong = tag.getLong("LinkingOverclocker");
            BlockPos overclockerPos = BlockPos.of(overclockerPosLong);
            
            BlockEntity overclockerBE = level.getBlockEntity(overclockerPos);
            if (overclockerBE instanceof TemporalOverclockerBlockEntity overclocker) {
                // Verifica che non stiamo cercando di linkare il blocco a se stesso
                if (pos.equals(overclockerPos)) {
                    player.sendSystemMessage(Component.translatable("item.iska_utils.temporal_overclocker_chipset.cannot_link_self"));
                    return InteractionResult.FAIL;
                }
                
                // Verifica che il blocco target abbia un BlockEntity
                BlockEntity targetBE = level.getBlockEntity(pos);
                if (targetBE == null) {
                    player.sendSystemMessage(Component.translatable("item.iska_utils.temporal_overclocker_chipset.no_block_entity"));
                    return InteractionResult.FAIL;
                }
                
                // Prova ad aggiungere il link
                if (overclocker.addLinkedBlock(pos)) {
                    player.sendSystemMessage(Component.translatable("item.iska_utils.temporal_overclocker_chipset.link_success"));
                    // Rimuovi la modalità linking
                    tag.remove("LinkingOverclocker");
                    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                } else {
                    if (overclocker.isLinked(pos)) {
                        // Se è già collegato, rimuovilo
                        if (overclocker.removeLinkedBlock(pos)) {
                            player.sendSystemMessage(Component.translatable("item.iska_utils.temporal_overclocker_chipset.link_removed"));
                            tag.remove("LinkingOverclocker");
                            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                        }
                    } else {
                        player.sendSystemMessage(Component.translatable("item.iska_utils.temporal_overclocker_chipset.link_failed"));
                    }
                }
                return InteractionResult.CONSUME;
            } else {
                // Il blocco overclocker non esiste più, cancella il linking
                tag.remove("LinkingOverclocker");
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                player.sendSystemMessage(Component.translatable("item.iska_utils.temporal_overclocker_chipset.overclocker_removed"));
            }
        }
        
        return InteractionResult.PASS;
    }
}


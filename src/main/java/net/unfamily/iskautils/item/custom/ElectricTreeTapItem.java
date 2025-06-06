package net.unfamily.iskautils.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.block.RubberLogEmptyBlock;
import net.unfamily.iskautils.block.RubberLogFilledBlock;

import java.util.List;

/**
 * Electric Tree Tap - Electric variant of the TreeTap that consumes energy
 */
public class ElectricTreeTapItem extends TreeTapItem {
    // Energy storage tag
    private static final String ENERGY_TAG = "Energy";

    public ElectricTreeTapItem(Properties properties) {
        super(properties);
    }
    
    /**
     * Adds tooltip information to the item
     */
    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        
        // Energy information
        if (canStoreEnergy()) {
            int energy = getEnergyStored(stack);
            int maxEnergy = getMaxEnergyStored(stack);
            float percentage = (float) energy / Math.max(1, maxEnergy) * 100f;
            
            String energyString = String.format("%,d / %,d RF (%.1f%%)", energy, maxEnergy, percentage);
            Component energyText = Component.translatable("item.iska_utils.electric_treetap.tooltip.energy")
                .withStyle(style -> style.withColor(ChatFormatting.RED))
                .append(Component.literal(energyString).withStyle(ChatFormatting.RED));
            
            tooltipComponents.add(energyText);
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockState state = level.getBlockState(context.getClickedPos());
        Player player = context.getPlayer();
        ItemStack itemStack = context.getItemInHand();
        InteractionHand hand = context.getHand();

        // Check if the block is a filled or empty rubber log
        if (state.is(ModBlocks.RUBBER_LOG_FILLED.get()) || state.is(ModBlocks.RUBBER_LOG_EMPTY.get())) {
            // Check if we have enough energy
            if (!level.isClientSide() && requiresEnergyToFunction() && !hasEnoughEnergy(itemStack)) {
                // Not enough energy, can't use the treetap
                if (player != null) {
                    player.displayClientMessage(
                        Component.translatable("item.iska_utils.electric_treetap.message.no_energy"),
                        true);
                }
                return InteractionResult.FAIL;
            }
            
            // Use the standard TreeTap
            InteractionResult result = super.useOn(context);
            
            // If the operation was successful, consume energy
            if (result.consumesAction() && !level.isClientSide()) {
                consumeEnergyForOperation(itemStack);
            }
            
            return result;
        }
        
        return InteractionResult.PASS;
    }
    
    // ===== ENERGY MANAGEMENT METHODS =====
    
    /**
     * Check if the item can store energy
     */
    public boolean canStoreEnergy() {
        // If buffer is 0, disable energy system completely
        return Config.electricTreetapEnergyBuffer > 0;
    }
    
    /**
     * Check if the item requires energy to function
     */
    public boolean requiresEnergyToFunction() {
        return Config.electricTreetapEnergyConsume > 0 && canStoreEnergy();
    }
    
    /**
     * Gets the energy stored in the ItemStack
     */
    public int getEnergyStored(ItemStack stack) {
        if (!canStoreEnergy()) {
            return 0;
        }
        
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.getInt(ENERGY_TAG);
    }
    
    /**
     * Sets the energy stored in the ItemStack
     */
    public void setEnergyStored(ItemStack stack, int energy) {
        if (!canStoreEnergy()) {
            return;
        }
        
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        int maxCapacity = Config.electricTreetapEnergyBuffer;
        tag.putInt(ENERGY_TAG, Math.max(0, Math.min(energy, maxCapacity)));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
    
    /**
     * Gets the maximum energy that can be stored
     */
    public int getMaxEnergyStored(ItemStack stack) {
        if (!canStoreEnergy()) {
            return 0;
        }
        return Config.electricTreetapEnergyBuffer;
    }
    
    /**
     * Checks if the treetap has enough energy for operation
     */
    public boolean hasEnoughEnergy(ItemStack stack) {
        if (!requiresEnergyToFunction()) {
            return true; // No energy required
        }
        
        int currentEnergy = getEnergyStored(stack);
        return currentEnergy >= Config.electricTreetapEnergyConsume;
    }
    
    /**
     * Consumes energy for operation
     * @param stack The ItemStack
     * @return true if energy was consumed or no energy is required, false if insufficient energy
     */
    public boolean consumeEnergyForOperation(ItemStack stack) {
        if (!requiresEnergyToFunction()) {
            return true; // No energy required
        }
        
        int consumption = Config.electricTreetapEnergyConsume;
        if (consumption <= 0) {
            return true; // No consumption
        }
        
        int currentEnergy = getEnergyStored(stack);
        if (currentEnergy >= consumption) {
            setEnergyStored(stack, currentEnergy - consumption);
            return true;
        }
        
        return false; // Insufficient energy
    }
} 
package net.unfamily.iskautils.item.custom;

import com.mojang.logging.LogUtils;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.unfamily.iskautils.util.ModUtils;
import net.unfamily.iskautils.stage.StageRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.neoforged.fml.ModList;
import net.unfamily.iskautils.Config;

import java.util.List;

/**
 * The Necrotic Crystal Heart is an item that, when worn as a curio,
 * intercepts damage events and can modify their behavior.
 * Completely blocks all damage when equipped.
 */
public class NecroticCrystalHeartItem extends Item {
    private static final Logger LOGGER = LoggerFactory.getLogger(NecroticCrystalHeartItem.class);
    
    // Damage reduction setting
    private static final float DAMAGE_REDUCTION = 0.0f; // Complete reduction (0% of original damage)
    
    public NecroticCrystalHeartItem(Properties properties) {
        super(properties);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        
        tooltipComponents.add(Component.literal(Component.translatable("tooltip.iska_utils.necrotic_crystal_heart.desc0").getString()));
        tooltipComponents.add(Component.literal(Component.translatable("tooltip.iska_utils.necrotic_crystal_heart.desc1").getString()));
        tooltipComponents.add(Component.literal(Component.translatable("tooltip.iska_utils.necrotic_crystal_heart.desc2").getString()));
        tooltipComponents.add(Component.literal(Component.translatable("tooltip.iska_utils.necrotic_crystal_heart.desc3").getString()));
        
        if (Config.artifactsInfo) {
            if (!isArtifactsLoaded()) {
                tooltipComponents.add(Component.literal(Component.translatable("tooltip.iska_utils.necrotic_crystal_heart.artifacts_required").getString()));
            } else {
                tooltipComponents.add(Component.literal(Component.translatable("tooltip.iska_utils.necrotic_crystal_heart.artifacts_loaded").getString()));
            }
        }
    }
    
    /**
     * Handles logic when an entity wearing the heart takes damage.
     * This method is called by the event handler when it detects that
     * the entity taking damage is wearing the Necrotic Crystal Heart.
     * 
     * @param stack The item stack
     * @param entity The entity wearing the heart
     * @param damageAmount The original damage amount
     * @return The new damage amount to apply, or -1 to cancel the event
     */
    public static float onDamageReceived(ItemStack stack, LivingEntity entity, float damageAmount) {
        if (entity instanceof Player player) {
            // Reduce damage based on DAMAGE_REDUCTION constant (0% = no damage)
            float reducedDamage = damageAmount * DAMAGE_REDUCTION;
            
            // Display protection message
            if (player.level().isClientSide) {
                player.displayClientMessage(
                    Component.literal("§5The Necrotic Crystal Heart protected you!"), 
                    true // actionbar
                );
            }
            
            return reducedDamage;
        }
        
        // For non-player entities, leave damage unchanged
        return damageAmount;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        
        if (entity instanceof Player player) {
            StageRegistry.addPlayerStage(player, "necro_crystal_heart_equip");
        }
    }

    private boolean isArtifactsLoaded() {
        return net.neoforged.fml.ModList.get().isLoaded("artifacts");
    }
} 
package net.unfamily.iskautils.item.custom;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.unfamily.iskautils.util.RubberNegateFallHandler;
import net.minecraft.core.Holder;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.unfamily.iskautils.item.ModItems;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.Util;

import java.util.List;
import java.util.EnumMap;

/**
 * Rubber boots that negate fall damage and make the player bounce
 */
@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class RubberBootsItem extends ArmorItem {
    
    public static Holder<ArmorMaterial> ARMOR_MATERIAL = null;


    @SubscribeEvent
	public static void registerArmorMaterial(RegisterEvent event) {
		event.register(Registries.ARMOR_MATERIAL, registerHelper -> {
			ArmorMaterial armorMaterial = new ArmorMaterial(Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
				map.put(ArmorItem.Type.BOOTS, 1);
			}), 9, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.EMPTY), () -> Ingredient.of(new ItemStack(ModItems.RUBBER.get())), List.of(new ArmorMaterial.Layer(ResourceLocation.parse("iska_utils:rubber"))), 0f, 0f);
			registerHelper.register(ResourceLocation.parse("iska_utils:rubber_boots"), armorMaterial);
			ARMOR_MATERIAL = BuiltInRegistries.ARMOR_MATERIAL.wrapAsHolder(armorMaterial);
		});
	}

    public RubberBootsItem(Item.Properties properties) {
        super(ARMOR_MATERIAL, ArmorItem.Type.BOOTS, properties);
    }
    
    /**
     * Returns the enchantment value for this item (same as leather/iron boots)
     */
    @Override
    public int getEnchantmentValue() {
        return 2;
    }
    
    /**
     * Handles the fall damage when the player wears the rubber boots
     * @param stack The stack of the item
     * @param entity The entity that wears the boots
     * @param fallDistance The distance of the fall
     * @return true if the fall damage should be negated
     */
    public static boolean handleFallDamage(ItemStack stack, LivingEntity entity, float fallDistance) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        
        Level level = entity.level();
        
        // If the player is crouched, reduce the damage but don't bounce
        if (player.isShiftKeyDown()) {
            // Reduced damage by 80%
            return true;
        }
        
        // Otherwise, negate the damage and make the player bounce
        if (fallDistance > 2) {
            player.resetFallDistance();
            
            // Add bounce effect
            Vec3 motion = player.getDeltaMovement();
            double bounce = -0.9F;
            
            player.setDeltaMovement(motion.x, motion.y * bounce, motion.z);
            player.hasImpulse = true;
            player.setOnGround(false);
            
            // Slightly slow down the horizontal movement
            double horizontalSlowdown = 0.95D;
            player.setDeltaMovement(motion.x / horizontalSlowdown, player.getDeltaMovement().y, motion.z / horizontalSlowdown);
            
            // Add the player to the list of bouncers
            RubberNegateFallHandler.addBouncingPlayer(player, player.getDeltaMovement().y);
            
            // Slightly damage the boots (1 point of durability)
            if (stack.isDamageableItem() && !player.getAbilities().instabuild) {
                // Damage the item by 1
                int newDamage = stack.getDamageValue() + 1;
                if (newDamage >= stack.getMaxDamage()) {
                    // If the item would break, remove it
                    stack.setCount(0);
                } else {
                    // Otherwise add damage
                    stack.setDamageValue(newDamage);
                }
            }
            
            return true;
        }
        
        return false;
    }
} 
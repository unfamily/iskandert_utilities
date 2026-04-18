package net.unfamily.iskautils.item.custom;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.unfamily.iskautils.util.RubberNegateFallHandler;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.core.component.DataComponents;

import java.util.Map;

/**
 * Rubber boots that negate fall damage and make the player bounce
 */
public class RubberBootsItem extends Item {

    private static final ResourceKey<EquipmentAsset> RUBBER_ASSET = ResourceKey.create(
            EquipmentAssets.ROOT_ID,
            Identifier.fromNamespaceAndPath("iska_utils", "rubber")
    );

    private static final TagKey<Item> REPAIRS_RUBBER = TagKey.create(
            net.minecraft.core.registries.Registries.ITEM,
            Identifier.fromNamespaceAndPath("iska_utils", "rubber")
    );

    private static final ArmorMaterial RUBBER_MATERIAL = new ArmorMaterial(
            9,
            Map.of(ArmorType.BOOTS, 1),
            2,
            SoundEvents.ARMOR_EQUIP_GENERIC,
            0.0F,
            0.0F,
            REPAIRS_RUBBER,
            RUBBER_ASSET
    );

    public RubberBootsItem(Item.Properties properties) {
        super(properties
                .enchantable(2)
                .component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.FEET).setAsset(RUBBER_ASSET).build())
                .component(DataComponents.ATTRIBUTE_MODIFIERS, RUBBER_MATERIAL.createAttributes(ArmorType.BOOTS)));
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
        
        // If the player is crouched, reduce the damage but don't bounce
        if (player.isShiftKeyDown()) {
            damageBootsForFallNegation(stack, player);
            return true;
        }
        
        // Otherwise, negate the damage and make the player bounce
        if (fallDistance > 2) {
            player.resetFallDistance();
            
            // Add bounce effect
            Vec3 motion = player.getDeltaMovement();
            double bounce = -0.9F;
            
            player.setDeltaMovement(motion.x, motion.y * bounce, motion.z);
            player.hurtMarked = true;
            player.setOnGround(false);
            
            // Slightly slow down the horizontal movement
            double horizontalSlowdown = 0.95D;
            player.setDeltaMovement(motion.x / horizontalSlowdown, player.getDeltaMovement().y, motion.z / horizontalSlowdown);
            
            // Add the player to the list of bouncers
            RubberNegateFallHandler.addBouncingPlayer(player, player.getDeltaMovement().y);
            
            damageBootsForFallNegation(stack, player);
            
            return true;
        }
        
        return false;
    }

    private static void damageBootsForFallNegation(ItemStack stack, Player player) {
        if (!stack.isDamageableItem() || player.getAbilities().instabuild) {
            return;
        }
        stack.hurtAndBreak(1, player, EquipmentSlot.FEET);
    }
} 
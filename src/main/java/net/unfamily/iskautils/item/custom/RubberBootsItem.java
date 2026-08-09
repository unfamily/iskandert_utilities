package net.unfamily.iskautils.item.custom;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

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
import java.util.function.Consumer;

/**
 * Rubber boots that negate fall damage.
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

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltip, flag);
        tooltip.accept(Component.translatable("tooltip.iska_utils.rubber_boots.desc"));
    }
    
    /**
     * Negates fall damage when the player wears rubber boots.
     * @return true if fall damage should be canceled
     */
    public static boolean handleFallDamage(ItemStack stack, LivingEntity entity, float fallDistance) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        if (fallDistance <= 0) {
            return false;
        }
        player.resetFallDistance();
        damageBootsForFallNegation(stack, player);
        return true;
    }

    private static void damageBootsForFallNegation(ItemStack stack, Player player) {
        if (!stack.isDamageableItem() || player.getAbilities().instabuild) {
            return;
        }
        stack.hurtAndBreak(1, player, EquipmentSlot.FEET);
    }
}

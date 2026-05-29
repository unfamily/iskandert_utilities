package net.unfamily.iskautils.item.entropic;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.util.Unit;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.unfamily.iskautils.IskaUtils;

import java.util.Map;

/**
 * Shared stats and item properties for the indestructible entropic gear set.
 */
public final class EntropicGear {
    public static final ResourceKey<EquipmentAsset> EQUIPMENT_ASSET = ResourceKey.create(
            EquipmentAssets.ROOT_ID,
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "entropic"));

    public static final TagKey<Item> NO_REPAIR = TagKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "entropic_no_repair"));

    /** Slightly above netherite: faster mining and +1 attack bonus. */
    public static final ToolMaterial TIER = new ToolMaterial(
            BlockTags.INCORRECT_FOR_NETHERITE_TOOL,
            0,
            10.0F,
            5.0F,
            15,
            NO_REPAIR);

    /** Netherite + 1 armor per slot until custom balance. */
    public static final ArmorMaterial ARMOR_MATERIAL = new ArmorMaterial(
            0,
            Map.of(
                    ArmorType.HELMET, 4,
                    ArmorType.CHESTPLATE, 9,
                    ArmorType.LEGGINGS, 7,
                    ArmorType.BOOTS, 4),
            15,
            SoundEvents.ARMOR_EQUIP_NETHERITE,
            3.0F,
            0.1F,
            NO_REPAIR,
            EQUIPMENT_ASSET);

    private EntropicGear() {}

    public static Item.Properties baseProperties() {
        return new Item.Properties()
                .fireResistant()
                .rarity(Rarity.EPIC)
                .enchantable(15)
                .component(DataComponents.UNBREAKABLE, Unit.INSTANCE);
    }

    public static Item.Properties armorProperties() {
        return baseProperties().stacksTo(1);
    }

    public static Item.Properties pickaxeProperties() {
        return TIER.applyToolProperties(baseProperties(), BlockTags.MINEABLE_WITH_PICKAXE, 1.0F, -2.8F, 0.0F);
    }

    public static Item.Properties axeProperties() {
        return TIER.applyToolProperties(baseProperties(), BlockTags.MINEABLE_WITH_AXE, 7.0F, -3.0F, 5.0F);
    }

    public static Item.Properties shovelProperties() {
        return TIER.applyToolProperties(baseProperties(), BlockTags.MINEABLE_WITH_SHOVEL, 1.5F, -3.0F, 0.0F);
    }

    public static Item.Properties swordProperties() {
        return TIER.applySwordProperties(baseProperties(), 5.0F, -2.4F);
    }

    /** Netherite spear tuning with +1 attack damage from {@link #SPEAR_TIER}. */
    private static final ToolMaterial SPEAR_TIER = new ToolMaterial(
            BlockTags.INCORRECT_FOR_NETHERITE_TOOL,
            0,
            10.0F,
            6.0F,
            15,
            NO_REPAIR);

    public static Item.Properties spearProperties() {
        return baseProperties().spear(SPEAR_TIER, 1.15F, 1.2F, 0.4F, 2.5F, 9.0F, 5.5F, 5.1F, 8.75F, 4.6F);
    }


    public static ItemAttributeModifiers paxelAttributes() {
        return ItemAttributeModifiers.builder()
                .add(
                        Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 6.0F, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(
                        Attributes.ATTACK_SPEED,
                        new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -2.8F, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }
}

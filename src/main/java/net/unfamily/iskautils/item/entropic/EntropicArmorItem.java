package net.unfamily.iskautils.item.entropic;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.Equippable;

import java.util.function.Consumer;

public class EntropicArmorItem extends Item {
    private final ArmorType armorType;

    public EntropicArmorItem(ArmorType armorType, Properties properties) {
        super(properties
                .component(
                        DataComponents.EQUIPPABLE,
                        Equippable.builder(armorType.getSlot())
                                .setAsset(EntropicGear.EQUIPMENT_ASSET)
                                .setEquipSound(SoundEvents.ARMOR_EQUIP_NETHERITE)
                                .setDamageOnHurt(false)
                                .build())
                .component(
                        DataComponents.ATTRIBUTE_MODIFIERS,
                        EntropicGear.ARMOR_MATERIAL.createAttributes(armorType)));
        this.armorType = armorType;
    }

    public ArmorType armorType() {
        return armorType;
    }

    public EquipmentSlot getEquipmentSlot() {
        return armorType.getSlot();
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay tooltipDisplay,
            Consumer<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltip, flag);
        tooltip.accept(Component.translatable("tooltip.iska_utils.entropic.unbreakable"));
    }
}

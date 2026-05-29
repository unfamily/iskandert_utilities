package net.unfamily.iskautils.item.entropic;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.Util;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.item.ModItems;

import java.util.EnumMap;
import java.util.List;

@EventBusSubscriber(modid = IskaUtils.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class EntropicArmorItem extends ArmorItem {
    public static Holder<ArmorMaterial> ARMOR_MATERIAL;

    @SubscribeEvent
    public static void registerArmorMaterial(RegisterEvent event) {
        event.register(Registries.ARMOR_MATERIAL, helper -> {
            ArmorMaterial material = new ArmorMaterial(
                    Util.make(new EnumMap<>(Type.class), map -> {
                        map.put(Type.HELMET, 4);
                        map.put(Type.CHESTPLATE, 9);
                        map.put(Type.LEGGINGS, 7);
                        map.put(Type.BOOTS, 4);
                    }),
                    15,
                    SoundEvents.ARMOR_EQUIP_NETHERITE,
                    () -> Ingredient.EMPTY,
                    List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "entropic"))),
                    3.0F,
                    0.1F);
            helper.register(ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "entropic"), material);
            ARMOR_MATERIAL = BuiltInRegistries.ARMOR_MATERIAL.wrapAsHolder(material);
        });
    }

    public EntropicArmorItem(Type type, Properties properties) {
        super(ARMOR_MATERIAL, type, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (stack.is(ModItems.ENTROPIC_HELMET.get())) {
            EntropicTooltip.appendToolLines(tooltip, "entropic_helmet");
        } else if (stack.is(ModItems.ENTROPIC_CHESTPLATE.get())) {
            EntropicTooltip.appendToolLines(tooltip, "entropic_chestplate");
        } else if (stack.is(ModItems.ENTROPIC_LEGGINGS.get())) {
            EntropicTooltip.appendToolLines(tooltip, "entropic_leggings");
        } else if (stack.is(ModItems.ENTROPIC_BOOTS.get())) {
            EntropicTooltip.appendToolLines(tooltip, "entropic_boots");
        }
    }
}

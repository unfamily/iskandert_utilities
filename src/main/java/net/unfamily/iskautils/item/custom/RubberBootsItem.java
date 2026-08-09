package net.unfamily.iskautils.item.custom;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.network.chat.Component;
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
import org.jetbrains.annotations.NotNull;

/**
 * Rubber boots that negate fall damage.
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

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.iska_utils.rubber_boots.desc"));
    }
    
    /**
     * Returns the enchantment value for this item (same as leather/iron boots)
     */
    @Override
    public int getEnchantmentValue() {
        return 2;
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
        if (stack.isDamageableItem() && !player.getAbilities().instabuild) {
            int newDamage = stack.getDamageValue() + 1;
            if (newDamage >= stack.getMaxDamage()) {
                stack.setCount(0);
            } else {
                stack.setDamageValue(newDamage);
            }
        }
        return true;
    }
} 
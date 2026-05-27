package net.unfamily.iskautils.item.custom.relic;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.unfamily.iskautils.util.CurioEquipUtil;

/**
 * Old Brick relic.
 * While equipped, grants +2 armor.
 */
public class OldBrickItem extends Item {
    private static final ResourceLocation ARMOR_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath("iska_utils", "old_brick_armor");

    public OldBrickItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (level.isClientSide) return;
        if (!(entity instanceof Player player)) return;

        boolean equipped = CurioEquipUtil.hasEquipped(player, this);
        AttributeInstance armor = player.getAttribute(Attributes.ARMOR);
        if (armor == null) return;

        AttributeModifier existing = armor.getModifier(ARMOR_MODIFIER_ID);
        if (equipped) {
            if (existing == null) {
                armor.addTransientModifier(new AttributeModifier(
                        ARMOR_MODIFIER_ID,
                        2.0,
                        AttributeModifier.Operation.ADD_VALUE
                ));
            }
        } else {
            if (existing != null) {
                armor.removeModifier(ARMOR_MODIFIER_ID);
            }
        }
    }
}


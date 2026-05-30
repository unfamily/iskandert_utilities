package net.unfamily.iskautils.arcane;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public final class ArcaneDictionaryAttributes {
    private ArcaneDictionaryAttributes() {}

    public static void applyTransient(AttributeInstance attribute, Identifier id, double amount) {
        if (attribute == null) {
            return;
        }
        if (amount == 0.0D) {
            remove(attribute, id);
            return;
        }
        AttributeModifier existing = attribute.getModifier(id);
        if (existing != null && existing.amount() == amount) {
            return;
        }
        if (existing != null) {
            attribute.removeModifier(id);
        }
        attribute.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
    }

    public static void applyTransientMultiplied(AttributeInstance attribute, Identifier id, double amount) {
        if (attribute == null) {
            return;
        }
        if (amount == 0.0D) {
            remove(attribute, id);
            return;
        }
        AttributeModifier existing = attribute.getModifier(id);
        if (existing != null && existing.amount() == amount) {
            return;
        }
        if (existing != null) {
            attribute.removeModifier(id);
        }
        attribute.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    public static void remove(AttributeInstance attribute, Identifier id) {
        if (attribute != null && attribute.getModifier(id) != null) {
            attribute.removeModifier(id);
        }
    }

    public static void clampHealth(ServerPlayer player) {
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }
}

package net.unfamily.iskautils.arcane;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class ArcaneDictionaryPassiveCleanup {
    private static final Identifier STONE_SKIN_ARMOR =
            Identifier.fromNamespaceAndPath("iska_utils", "arcane/stone_skin_armor");
    private static final Identifier ENTROPY_SHELL_TOUGHNESS =
            Identifier.fromNamespaceAndPath("iska_utils", "arcane/entropy_shell_toughness");
    private static final Identifier ENTROPY_SHELL_HP =
            Identifier.fromNamespaceAndPath("iska_utils", "arcane/entropy_shell_hp");
    private static final Identifier AGILITY_SPEED =
            Identifier.fromNamespaceAndPath("iska_utils", "arcane/agility_speed");
    private static final Identifier ENTROPY_OVERFLOW_HP =
            Identifier.fromNamespaceAndPath("iska_utils", "arcane/entropy_overflow_hp");

    private ArcaneDictionaryPassiveCleanup() {}

    public static void clear(ServerPlayer player) {
        remove(player.getAttribute(Attributes.ARMOR), STONE_SKIN_ARMOR);
        remove(player.getAttribute(Attributes.ARMOR_TOUGHNESS), ENTROPY_SHELL_TOUGHNESS);
        remove(player.getAttribute(Attributes.MAX_HEALTH), ENTROPY_SHELL_HP);
        remove(player.getAttribute(Attributes.MAX_HEALTH), ENTROPY_OVERFLOW_HP);
        remove(player.getAttribute(Attributes.MOVEMENT_SPEED), AGILITY_SPEED);
        ArcaneDictionaryAttributes.clampHealth(player);
    }

    private static void remove(AttributeInstance attribute, Identifier id) {
        ArcaneDictionaryAttributes.remove(attribute, id);
    }
}

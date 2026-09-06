package net.unfamily.iskautils.shop;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * Built-in shop entry type ids ({@code iska_utils:*}).
 */
public final class ShopEntryTypes {
    public static final Identifier ITEM = ShopEntryTypeRegistry.modId("item");
    public static final Identifier FLUID = ShopEntryTypeRegistry.modId("fluid");
    public static final Identifier GAS = ShopEntryTypeRegistry.modId("gas");
    public static final Identifier RF = ShopEntryTypeRegistry.modId("rf");
    public static final Identifier COMMAND = ShopEntryTypeRegistry.modId("command");
    public static final Identifier STAGE = ShopEntryTypeRegistry.modId("stage");
    public static final Identifier CURRENCY = ShopEntryTypeRegistry.modId("currency");

    private ShopEntryTypes() {}

    public static boolean isItem(@Nullable ShopEntry entry) {
        return entry != null && ITEM.equals(entry.typeId);
    }

    public static boolean isFluid(@Nullable ShopEntry entry) {
        return entry != null && FLUID.equals(entry.typeId);
    }

    public static boolean isGas(@Nullable ShopEntry entry) {
        return entry != null && GAS.equals(entry.typeId);
    }

    public static boolean isRf(@Nullable ShopEntry entry) {
        return entry != null && RF.equals(entry.typeId);
    }

    public static boolean isCommand(@Nullable ShopEntry entry) {
        return entry != null && COMMAND.equals(entry.typeId);
    }

    public static boolean isStage(@Nullable ShopEntry entry) {
        return entry != null && STAGE.equals(entry.typeId);
    }

    public static boolean isCurrency(@Nullable ShopEntry entry) {
        return entry != null && CURRENCY.equals(entry.typeId);
    }

    public static boolean equals(@Nullable ShopEntry entry, Identifier id) {
        return entry != null && id != null && id.equals(entry.typeId);
    }
}

package net.unfamily.iskautils.client.gui;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Upgrade slot that only accepts iska_utils modules: slot 0 = logic module, slot 1 = speed modules.
 * When iska_utils is not present, no items are allowed (registry returns empty item).
 */
public class UpgradeSlot extends SlotItemHandler {

    private static final String ISKA_UTILS = "iska_utils";
    private static final Identifier LOGIC_MODULE_ID = Identifier.fromNamespaceAndPath(ISKA_UTILS, "logic_module");
    private static final Identifier PRODUCTION_MODULE_ID = Identifier.fromNamespaceAndPath(ISKA_UTILS, "production_module");
    private static final Identifier[] SPEED_MODULE_IDS = new Identifier[]{
            Identifier.fromNamespaceAndPath(ISKA_UTILS, "slow_module"),
            Identifier.fromNamespaceAndPath(ISKA_UTILS, "moderate_module"),
            Identifier.fromNamespaceAndPath(ISKA_UTILS, "fast_module"),
            Identifier.fromNamespaceAndPath(ISKA_UTILS, "extreme_module"),
            Identifier.fromNamespaceAndPath(ISKA_UTILS, "ultra_module")
    };

    private final int upgradeIndex; // 0 = logic, 1 = speed

    public UpgradeSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
        super(itemHandler, index, xPosition, yPosition);
        this.upgradeIndex = index;
    }

    @Override
    public boolean mayPlace(@NotNull ItemStack stack) {
        if (stack.isEmpty()) return true;
        if (getItemHandler().getSlotLimit(upgradeIndex) <= 0) return false;
        Item item = stack.getItem();
        if (upgradeIndex == 0) {
            Item logic = BuiltInRegistries.ITEM.getValue(LOGIC_MODULE_ID);
            return logic != null && logic != Items.AIR && item.equals(logic);
        } else if (upgradeIndex == 1) {
            for (Identifier id : SPEED_MODULE_IDS) {
                Item speed = BuiltInRegistries.ITEM.getValue(id);
                if (speed != null && speed != Items.AIR && item.equals(speed)) {
                    return true;
                }
            }
            return false;
        }
        Item production = BuiltInRegistries.ITEM.getValue(PRODUCTION_MODULE_ID);
        return production != null && production != Items.AIR && item.equals(production);
    }
}

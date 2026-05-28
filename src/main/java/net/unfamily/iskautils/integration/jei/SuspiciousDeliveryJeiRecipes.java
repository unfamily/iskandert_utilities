package net.unfamily.iskautils.integration.jei;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.unfamily.iskautils.command.CommandItemAction;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.obtaining.SuspiciousDeliveryDefinition;
import net.unfamily.iskautils.obtaining.SuspiciousDeliveryJeiMode;
import net.unfamily.iskautils.obtaining.SuspiciousDeliveryLoader;
import net.unfamily.iskautils.obtaining.SuspiciousDeliveryLoot;

import java.util.ArrayList;
import java.util.List;

public final class SuspiciousDeliveryJeiRecipes {

    public static final int OUTPUTS_PER_PAGE = 27;

    private SuspiciousDeliveryJeiRecipes() {}

    public static void reloadForClient(Minecraft mc) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null && mc != null) {
            server = mc.getSingleplayerServer();
        }
        if (server != null) {
            SuspiciousDeliveryLoader.loadAll(server.getResourceManager());
        }
    }

    public static List<SuspiciousDeliveryJeiRecipe> buildAll() {
        SuspiciousDeliveryDefinition def = SuspiciousDeliveryLoader.get();
        int total = SuspiciousDeliveryLoot.totalWeight(def);
        if (total <= 0) {
            return List.of();
        }

        ItemStack mask = new ItemStack(ModItems.SUSPICIOUS_DELIVERY_UNDEFINED.get());
        List<SuspiciousDeliveryJeiEntry> slots = new ArrayList<>();

        for (SuspiciousDeliveryDefinition.Entry entry : def.entries()) {
            if (entry.jeiMode() == SuspiciousDeliveryJeiMode.HIDDEN) {
                continue;
            }
            List<ItemStack> stacks = resolveDisplayStacks(entry, mask);
            if (stacks.isEmpty()) {
                continue;
            }
            double pct = 100.0 * entry.weight() / total;
            slots.add(new SuspiciousDeliveryJeiEntry(pct, stacks, entry.luck()));
        }

        if (slots.isEmpty()) {
            return List.of();
        }

        List<SuspiciousDeliveryJeiRecipe> pages = new ArrayList<>();
        for (int p = 0; p < slots.size(); p += OUTPUTS_PER_PAGE) {
            int end = Math.min(p + OUTPUTS_PER_PAGE, slots.size());
            pages.add(new SuspiciousDeliveryJeiRecipe(slots.subList(p, end)));
        }
        return pages;
    }

    /**
     * Mask wins over drops; multiple drops cycle in the same JEI slot via {@code addItemStacks}.
     */
    private static List<ItemStack> resolveDisplayStacks(
            SuspiciousDeliveryDefinition.Entry entry,
            ItemStack mask) {
        if (entry.jeiMode() == SuspiciousDeliveryJeiMode.MASK) {
            return List.of(mask.copy());
        }
        List<ItemStack> stacks = new ArrayList<>();
        for (CommandItemAction action : entry.actions()) {
            if (action.getType() != CommandItemAction.ActionType.DROP || action.getDropItemId() == null) {
                continue;
            }
            Item item = BuiltInRegistries.ITEM.getOptional(action.getDropItemId()).orElse(null);
            if (item != null) {
                stacks.add(new ItemStack(item));
            }
        }
        return stacks;
    }
}

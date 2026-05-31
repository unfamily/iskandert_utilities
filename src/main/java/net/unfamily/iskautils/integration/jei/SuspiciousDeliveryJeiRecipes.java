package net.unfamily.iskautils.integration.jei;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.unfamily.iskautils.command.CommandItemAction;
import net.unfamily.iskautils.data.load.CraftingEntryPools;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.obtaining.SuspiciousDeliveryDefinition;
import net.unfamily.iskautils.obtaining.SuspiciousDeliveryJeiMode;
import net.unfamily.iskautils.obtaining.SuspiciousDeliveryLoader;
import net.unfamily.iskautils.obtaining.SuspiciousDeliveryLoot;
import net.unfamily.iskautils.script.LoadModGate;

import java.util.ArrayList;
import java.util.Comparator;
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
        Minecraft mc = Minecraft.getInstance();
        ServerPlayer player = CraftingEntryPools.resolveJeiPlayer(mc);
        SuspiciousDeliveryDefinition def = SuspiciousDeliveryLoader.get();
        List<SuspiciousDeliveryDefinition.Entry> pool =
                player != null ? SuspiciousDeliveryLoot.eligiblePool(player, def) : def.entries();
        int total = CraftingEntryPools.deliveryPoolTotalWeight(pool);
        if (total <= 0) {
            return List.of();
        }

        ItemStack mask = new ItemStack(ModItems.SUSPICIOUS_DELIVERY_UNDEFINED.get());
        List<SuspiciousDeliveryJeiEntry> slots = new ArrayList<>();

        for (SuspiciousDeliveryDefinition.Entry entry : def.entries()) {
            if (entry.jeiMode() == SuspiciousDeliveryJeiMode.HIDDEN) {
                continue;
            }
            if (!entry.checkAllMods()) {
                continue;
            }
            if (player != null && !entry.isPoolEligible(player)
                    && !LoadModGate.isDeferredLogic(entry.stageHost().getStagesLogic())
                    && !LoadModGate.isDeferredLogic(entry.stageHost().getModsLogic())) {
                continue;
            }
            List<ItemStack> stacks = resolveDisplayStacks(entry, mask);
            if (stacks.isEmpty()) {
                continue;
            }
            double pct = CraftingEntryPools.deliveryChancePercent(entry, pool);
            slots.add(new SuspiciousDeliveryJeiEntry(pct, stacks, entry.luck()));
        }

        if (slots.isEmpty()) {
            return List.of();
        }

        slots.sort(Comparator
                .comparingDouble(SuspiciousDeliveryJeiEntry::weightPercent)
                .thenComparing(Comparator.comparingInt(SuspiciousDeliveryJeiEntry::entryLuck).reversed()));

        List<SuspiciousDeliveryJeiRecipe> pages = new ArrayList<>();
        for (int p = 0; p < slots.size(); p += OUTPUTS_PER_PAGE) {
            int end = Math.min(p + OUTPUTS_PER_PAGE, slots.size());
            pages.add(new SuspiciousDeliveryJeiRecipe(slots.subList(p, end)));
        }
        return pages;
    }

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

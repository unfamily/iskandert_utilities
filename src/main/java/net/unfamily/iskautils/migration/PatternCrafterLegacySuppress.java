package net.unfamily.iskautils.migration;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

public final class PatternCrafterLegacySuppress {
    private static final String LEGACY_MOD_ID = "pattern_crafter";

    private PatternCrafterLegacySuppress() {}

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(EventPriority.LOWEST, PatternCrafterLegacySuppress::onCreativeTab);
        NeoForge.EVENT_BUS.addListener(PatternCrafterLegacySuppress::onTooltip);
    }

    private static void onCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (!ModList.get().isLoaded(LEGACY_MOD_ID)) return;
        for (ItemStack stack : List.copyOf(event.getParentEntries())) {
            if (isLegacy(stack)) event.remove(stack, CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
        }
        for (ItemStack stack : List.copyOf(event.getSearchEntries())) {
            if (isLegacy(stack)) event.remove(stack, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        }
    }

    private static void onTooltip(ItemTooltipEvent event) {
        if (!isLegacy(event.getItemStack())) return;
        event.getToolTip().add(Component.translatable("item.pattern_crafter.deprecated.line1"));
        event.getToolTip().add(Component.translatable("item.pattern_crafter.deprecated.line2"));
    }

    private static boolean isLegacy(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null && LEGACY_MOD_ID.equals(id.getNamespace());
    }
}

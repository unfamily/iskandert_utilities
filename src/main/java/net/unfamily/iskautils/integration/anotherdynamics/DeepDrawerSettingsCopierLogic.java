package net.unfamily.iskautils.integration.anotherdynamics;

import net.unfamily.iskautils.util.ModLogger;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.unfamily.iskautils.block.entity.DeepDrawerExtractorBlockEntity;
import net.unfamily.iskautils.client.gui.DeepDrawerExtractorMenu;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Copy/paste Deep Drawer Extractor filter lists via Another Dynamics Settings Copier (reflection, optional mod).
 */
public final class DeepDrawerSettingsCopierLogic {
    private static final ModLogger LOGGER = ModLogger.of(DeepDrawerSettingsCopierLogic.class);

    private DeepDrawerSettingsCopierLogic() {}

    public static void copyToCopier(ServerPlayer player, DeepDrawerExtractorMenu menu, boolean allowList) {
        if (!AnotherDynamicsCompat.isLoaded()) {
            return;
        }
        int slotIdx = menu.copySettingsSlotIndex();
        if (slotIdx < 0) {
            return;
        }
        ItemStack copier = menu.getSlot(slotIdx).getItem();
        if (copier.isEmpty() || !isSettingsCopier(copier)) {
            player.sendSystemMessage(Component.translatable("gui.iska_utils.deep_drawer_extractor.settings_copier.empty_copier"), true);
            return;
        }
        DeepDrawerExtractorBlockEntity be = menu.getBlockEntity();
        if (be == null) {
            return;
        }
        List<String> lines = collectNonEmptyLines(allowList ? be.getFilterFields() : be.getInvertedFilterFields());
        List<Integer> concat = collectConcatForLines(
                allowList ? be.getAllowConcatChannels() : be.getDenyConcatChannels(),
                allowList ? be.getFilterFields() : be.getInvertedFilterFields());
        try {
            HolderLookup.Provider registries = player.registryAccess();
            CompoundTag snap = buildPortableSnapshot(lines, concat, registries);
            if (snap == null) {
                return;
            }
            writeToCopier(copier, snap, registries);
            menu.getSlot(slotIdx).set(copier);
            player.sendSystemMessage(Component.translatable("gui.iska_utils.deep_drawer_extractor.settings_copier.copied"), true);
        } catch (ReflectiveOperationException e) {
            LOGGER.warn("Settings copier copy failed", e);
        }
    }

    public static void pasteFromCopier(ServerPlayer player, DeepDrawerExtractorMenu menu, boolean allowList) {
        if (!AnotherDynamicsCompat.isLoaded()) {
            return;
        }
        int slotIdx = menu.copySettingsSlotIndex();
        if (slotIdx < 0) {
            return;
        }
        ItemStack copier = menu.getSlot(slotIdx).getItem();
        if (copier.isEmpty() || !isSettingsCopier(copier)) {
            player.sendSystemMessage(Component.translatable("gui.iska_utils.deep_drawer_extractor.settings_copier.empty_copier"), true);
            return;
        }
        DeepDrawerExtractorBlockEntity be = menu.getBlockEntity();
        if (be == null) {
            return;
        }
        try {
            HolderLookup.Provider registries = player.registryAccess();
            CompoundTag snap = readFromCopier(copier, registries);
            if (snap == null || !isFilterPayload(snap)) {
                player.sendSystemMessage(Component.translatable("gui.iska_utils.deep_drawer_extractor.settings_copier.paste.invalid_kind"), true);
                return;
            }
            if (!isItemMaterialKind(snap)) {
                player.sendSystemMessage(Component.translatable("gui.iska_utils.deep_drawer_extractor.settings_copier.paste.invalid_kind"), true);
                return;
            }
            List<String> lines = readStringList(snap, "Lines");
            List<Integer> concat = readConcatFromSnapshot(snap, lines.size());
            if (allowList) {
                be.replaceAllowFilterList(lines, concat);
            } else {
                be.replaceDenyFilterList(lines, concat);
            }
            player.sendSystemMessage(Component.translatable("gui.iska_utils.deep_drawer_extractor.settings_copier.pasted"), true);
        } catch (ReflectiveOperationException e) {
            LOGGER.warn("Settings copier paste failed", e);
        }
    }

    public static boolean isSettingsCopier(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        var key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        return AnotherDynamicsCompat.MOD_ID.equals(key.getNamespace()) && "settings_copier".equals(key.getPath());
    }

    private static List<String> collectNonEmptyLines(List<String> source) {
        List<String> out = new ArrayList<>();
        if (source == null) {
            return out;
        }
        for (String line : source) {
            if (line != null && !line.trim().isEmpty()) {
                out.add(line);
            }
        }
        return out;
    }

    private static List<Integer> collectConcatForLines(List<Integer> concatSource, List<String> allLines) {
        List<Integer> out = new ArrayList<>();
        if (allLines == null) {
            return out;
        }
        for (int i = 0; i < allLines.size(); i++) {
            String line = allLines.get(i);
            if (line != null && !line.trim().isEmpty()) {
                int ch = (concatSource != null && i < concatSource.size() && concatSource.get(i) != null)
                        ? concatSource.get(i) : 0;
                out.add(ch);
            }
        }
        return out;
    }

    private static CompoundTag buildPortableSnapshot(
            List<String> lines, List<Integer> concat, HolderLookup.Provider registries) throws ReflectiveOperationException {
        Class<?> materialKind = Class.forName("net.unfamily.another_dynamics.duct.settings.FilterListMaterialKind");
        Object itemKind = Enum.valueOf((Class<Enum>) materialKind, "ITEM");
        Class<?> snapClass = Class.forName("net.unfamily.another_dynamics.duct.settings.DuctFilterListSnapshot");
        Method build = snapClass.getMethod("buildPortableAllowList", List.class, List.class, List.class, materialKind);
        return (CompoundTag) build.invoke(null, lines, null, concat, itemKind);
    }

    private static void writeToCopier(ItemStack copier, CompoundTag snap, HolderLookup.Provider registries)
            throws ReflectiveOperationException {
        Class<?> faceSnap = Class.forName("net.unfamily.another_dynamics.duct.settings.DuctFaceSettingsSnapshot");
        Method write = faceSnap.getMethod("writeToCopier", ItemStack.class, CompoundTag.class);
        write.invoke(null, copier, snap);
        Class<?> storeKind = Class.forName("net.unfamily.another_dynamics.duct.settings.SettingsCopierStoreKind");
        Object filterMode = Enum.valueOf((Class<Enum>) storeKind, "FILTER");
        Method setMode = storeKind.getMethod("setMode", ItemStack.class, storeKind);
        setMode.invoke(null, copier, filterMode);
    }

    private static CompoundTag readFromCopier(ItemStack copier, HolderLookup.Provider registries)
            throws ReflectiveOperationException {
        Class<?> faceSnap = Class.forName("net.unfamily.another_dynamics.duct.settings.DuctFaceSettingsSnapshot");
        Method read = faceSnap.getMethod("readFromCopier", ItemStack.class);
        Object opt = read.invoke(null, copier);
        if (opt instanceof java.util.Optional<?> optional && optional.isPresent()) {
            return (CompoundTag) optional.get();
        }
        return null;
    }

    private static boolean isFilterPayload(CompoundTag tag) throws ReflectiveOperationException {
        Class<?> snapClass = Class.forName("net.unfamily.another_dynamics.duct.settings.DuctFilterListSnapshot");
        Method m = snapClass.getMethod("isFilterPayload", CompoundTag.class);
        return Boolean.TRUE.equals(m.invoke(null, tag));
    }

    private static boolean isItemMaterialKind(CompoundTag tag) throws ReflectiveOperationException {
        Class<?> snapClass = Class.forName("net.unfamily.another_dynamics.duct.settings.DuctFilterListSnapshot");
        Method m = snapClass.getMethod("getMaterialKind", CompoundTag.class);
        Object kind = m.invoke(null, tag);
        Class<?> materialKind = Class.forName("net.unfamily.another_dynamics.duct.settings.FilterListMaterialKind");
        Object itemKind = Enum.valueOf((Class<Enum>) materialKind, "ITEM");
        return itemKind.equals(kind);
    }

    private static List<String> readStringList(CompoundTag tag, String key) {
        List<String> out = new ArrayList<>();
        ListTag list = tag.getList(key).orElse(null);
        if (list == null) {
            return out;
        }
        for (int i = 0; i < list.size(); i++) {
            Tag element = list.get(i);
            if (element instanceof StringTag stringTag) {
                out.add(stringTag.value());
            }
        }
        return out;
    }

    private static List<Integer> readConcatFromSnapshot(CompoundTag tag, int lineCount) {
        List<Integer> out = new ArrayList<>();
        String key = "AllowConcat";
        tag.getByteArray(key).ifPresent(arr -> {
            for (byte b : arr) {
                out.add(b & 0xFF);
            }
        });
        while (out.size() < lineCount) {
            out.add(0);
        }
        while (out.size() > lineCount) {
            out.remove(out.size() - 1);
        }
        return out;
    }
}

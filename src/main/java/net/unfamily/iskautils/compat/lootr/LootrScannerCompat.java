package net.unfamily.iskautils.compat.lootr;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.fml.ModList;

/**
 * Optional Lootr integration for loot scanner chip. Uses reflection so the mod loads without Lootr present.
 */
public final class LootrScannerCompat {
    private static final String PROVIDER_CLASS = "noobanidus.mods.lootr.common.api.data.ILootrInfoProvider";
    private static final String LOOTR_ENTITY_CLASS = "noobanidus.mods.lootr.common.api.data.entity.ILootrEntity";

    private LootrScannerCompat() {}

    public static boolean isLoaded() {
        if (!ModList.get().isLoaded("lootr")) {
            return false;
        }
        try {
            Class<?> api = Class.forName("noobanidus.mods.lootr.common.api.LootrAPI");
            return (boolean) api.getMethod("isReady").invoke(null);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    public static boolean isLootrContainer(ServerLevel level, BlockPos pos) {
        return resolveProvider(level, pos) != null;
    }

    public static boolean matchesMode(ServerLevel level, BlockPos pos, ServerPlayer player, int mode) {
        Object provider = resolveProvider(level, pos);
        if (provider == null) {
            return false;
        }
        return matchesProviderMode(provider, player, mode);
    }

    public static boolean isLootrEntity(Entity entity) {
        if (!isLoaded() || entity == null) {
            return false;
        }
        try {
            Class<?> clazz = Class.forName(LOOTR_ENTITY_CLASS);
            return clazz.isInstance(entity);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    public static boolean matchesEntityMode(Entity entity, ServerPlayer player, int mode) {
        if (!isLootrEntity(entity)) {
            return false;
        }
        return matchesProviderMode(entity, player, mode);
    }

    public static ResourceLocation getEntityTypeId(Entity entity) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
    }

    private static boolean matchesProviderMode(Object provider, ServerPlayer player, int mode) {
        try {
            boolean opened = (boolean) provider.getClass().getMethod("hasServerOpened", ServerPlayer.class).invoke(provider, player);
            boolean hasLoot = (boolean) provider.getClass().getMethod("hasLootAvailable", ServerPlayer.class).invoke(provider, player);
            return switch (mode) {
                case 1 -> !opened;
                case 2 -> opened && !hasLoot;
                case 3 -> opened && hasLoot;
                default -> false;
            };
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    private static Object resolveProvider(ServerLevel level, BlockPos pos) {
        if (!isLoaded()) {
            return null;
        }
        try {
            Class<?> providerClass = Class.forName(PROVIDER_CLASS);
            return providerClass.getMethod("of", BlockPos.class, net.minecraft.world.level.BlockGetter.class)
                    .invoke(null, pos, level);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    public static boolean isEmptyContainer(Container container) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (!container.getItem(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public static boolean hasLootTable(BlockEntity blockEntity) {
        try {
            var method = blockEntity.getClass().getMethod("getLootTable");
            Object lootTable = method.invoke(blockEntity);
            return lootTable != null;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }
}

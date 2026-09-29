package net.unfamily.iskautils.iska_utils_stages;

import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

/**
 * Thin bridge to Library {@link net.unfamily.iskalib.stage.StageItemHandler}.
 */
public final class StageItemHandler {
    private StageItemHandler() {}

    public static void loadAll(ResourceManager resourceManagerOrNull) {
        net.unfamily.iskalib.stage.StageItemHandler.loadAll(resourceManagerOrNull);
    }

    public static Set<String> collectReferencedStages() {
        return net.unfamily.iskalib.stage.StageItemHandler.collectReferencedStages();
    }

    public static void checkContainer(AbstractContainerMenu container, Player player) {
        net.unfamily.iskalib.stage.StageItemHandler.checkContainer(container, player);
    }

    public static boolean shouldBlockRightClick(Player player, ItemStack itemStack) {
        return net.unfamily.iskalib.stage.StageItemHandler.shouldBlockRightClick(player, itemStack);
    }

    public static boolean shouldBlockLeftClick(Player player, ItemStack itemStack) {
        return net.unfamily.iskalib.stage.StageItemHandler.shouldBlockLeftClick(player, itemStack);
    }

    public static String checkMainHandRestriction(Player player, ItemStack itemStack) {
        return net.unfamily.iskalib.stage.StageItemHandler.checkMainHandRestriction(player, itemStack);
    }

    public static String checkOffHandRestriction(Player player, ItemStack itemStack) {
        return net.unfamily.iskalib.stage.StageItemHandler.checkOffHandRestriction(player, itemStack);
    }

    public static boolean applyHandConsequence(ServerPlayer player, InteractionHand hand, String consequence) {
        return net.unfamily.iskalib.stage.StageItemHandler.applyHandConsequence(player, hand, consequence);
    }
}

package net.unfamily.iskautils.item.custom.relic;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.unfamily.iskautils.util.RelicActivationUtil;

import java.util.List;

/**
 * The Roots relic.
 * Mining speed boost is handled via events.
 */
public class TheRootsItem extends Item {
    private static final String STAGE_ID = "iska_utils_internal-the_roots_equip";

    public TheRootsItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!(entity instanceof Player player)) return;
        RelicActivationUtil.syncCurioOnlyStage(player, stack, STAGE_ID);
    }

    @Override
    public Component getName(ItemStack stack) {
        if (isUnixLike()) {
            return Component.translatable("item.iska_utils.the_roots.unix");
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.iska_utils.the_roots.desc0"));
        tooltip.add(Component.translatable("tooltip.iska_utils.the_roots.desc1"));
    }

    private static boolean isUnixLike() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("linux") || os.contains("mac") || os.contains("unix") || os.contains("android");
    }
}


package net.unfamily.iskautils.item.custom.artifact;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.util.ArtifactActivationUtil;
import net.unfamily.iskautils.util.ArtifactBalanceFormat;
import net.unfamily.iskautils.util.ArtifactTooltipUtil;

import java.util.List;

/**
 * The Roots artifact.
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
        ArtifactActivationUtil.syncCurioOnlyStage(player, stack, STAGE_ID);
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
        double maxMult = Config.theRootsBreakSpeedMinMultiplier + Config.theRootsBreakSpeedMaxBonus;
        ArtifactTooltipUtil.appendDescLines(
                tooltip,
                "the_roots",
                1,
                2,
                ArtifactBalanceFormat.speedBonusPercent(Config.theRootsBreakSpeedMinMultiplier),
                ArtifactBalanceFormat.speedBonusPercent(maxMult));
    }

    public static boolean isUnixLike() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("linux") || os.contains("mac") || os.contains("unix") || os.contains("android");
    }
}


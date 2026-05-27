package net.unfamily.iskautils.item.custom.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.item.custom.BurningBrazierItem;
import net.unfamily.iskautils.util.RelicEquipStages;
import net.unfamily.iskalib.stage.StageRegistry;

import java.util.function.Consumer;

/**
 * Cursed Candle - eternal brazier extension: cursed flames, no durability.
 * Player ignite on place uses the same rules as {@link BurningBrazierItem}
 * ({@code burning_brazier_super_hot} or {@code iska_utils_internal-curse_flame} stage).
 */
public class CursedCandleItem extends BurningBrazierItem {
    private static final String PATH = "cursed_candle";

    public CursedCandleItem(Properties properties) {
        super(properties);
    }

    @Override
    protected Block getFlameBlock() {
        return ModBlocks.CURSED_BURNING_FLAME.get();
    }

    @Override
    protected boolean isManagedFlame(Block block) {
        return block == ModBlocks.CURSED_BURNING_FLAME.get();
    }

    @Override
    protected boolean consumesDurability() {
        return false;
    }

    @Override
    protected boolean canAutoPlace(ServerPlayer player, ServerLevel level, ItemStack stack) {
        return StageRegistry.playerHasStage(player, RelicEquipStages.CURSED_CANDLE);
    }

    @Override
    protected String flamesTooltipDesc0Key() {
        return "tooltip.iska_utils.burning_flames.desc0.cursed";
    }

    @Override
    protected String flamesTooltipDesc2Key() {
        return "tooltip.iska_utils.burning_flames.desc2.cursed";
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.iska_utils." + PATH + ".cursed"));
        appendFlamesTooltip(tooltip, flamesTooltipDesc0Key(), flamesTooltipDesc2Key());
    }
}

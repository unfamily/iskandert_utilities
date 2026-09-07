package net.unfamily.iskautils.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.ModBlocks;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Consumable upgrade: Shift+use on a normal Pattern Crafter converts it to Improved
 * while preserving all BlockEntity data.
 */
public class PatternCrafterImproverItem extends Item {

    public PatternCrafterImproverItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay tooltipDisplay,
            Consumer<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltip, flag);
        tooltip.accept(Component.translatable("tooltip.iska_utils.pattern_crafter_improver.desc0")
                .withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.iska_utils.pattern_crafter_improver.desc1")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    @NotNull
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        if (!state.is(ModBlocks.PATTERN_CRAFTER.get())) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.FAIL;
        }

        if (!upgradeToImproved(serverLevel, pos)) {
            return InteractionResult.FAIL;
        }

        ItemStack stack = context.getItemInHand();
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        player.sendOverlayMessage(Component.translatable("item.iska_utils.pattern_crafter_improver.upgraded"));
        return InteractionResult.CONSUME;
    }

    /**
     * Converts a placed normal Pattern Crafter into an Improved one without dropping contents.
     *
     * @return true if conversion succeeded
     */
    public static boolean upgradeToImproved(ServerLevel level, BlockPos pos) {
        BlockState oldState = level.getBlockState(pos);
        if (!oldState.is(ModBlocks.PATTERN_CRAFTER.get())) {
            return false;
        }

        BlockEntity oldEntity = level.getBlockEntity(pos);
        CompoundTag tag = oldEntity == null ? null : oldEntity.saveWithFullMetadata(level.registryAccess());

        BlockState replacementState = ModBlocks.IMPROVED_PATTERN_CRAFTER.get().defaultBlockState();

        // Remove BE first so onRemove does not spill inventory.
        level.removeBlockEntity(pos);
        level.setBlock(pos, replacementState, Block.UPDATE_ALL);

        if (tag != null) {
            tag.putString("id", IskaUtils.MOD_ID + ":improved_pattern_crafter");
            tag.putInt("x", pos.getX());
            tag.putInt("y", pos.getY());
            tag.putInt("z", pos.getZ());
            expandUpgradeSlotsIfNeeded(tag);
            BlockEntity converted = BlockEntity.loadStatic(pos, replacementState, tag, level.registryAccess());
            if (converted != null) {
                level.setBlockEntity(converted);
                converted.setChanged();
            }
        }

        level.sendBlockUpdated(pos, replacementState, replacementState, Block.UPDATE_ALL);
        return true;
    }

    private static void expandUpgradeSlotsIfNeeded(CompoundTag tag) {
        if (!tag.contains("upgrades")) {
            return;
        }
        CompoundTag upgrades = tag.getCompound("upgrades").orElse(new CompoundTag());
        if (upgrades.contains("Size") && upgrades.getIntOr("Size", 0) < 3) {
            upgrades.putInt("Size", 3);
            tag.put("upgrades", upgrades);
        }
    }
}

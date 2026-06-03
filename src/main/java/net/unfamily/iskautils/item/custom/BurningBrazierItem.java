package net.unfamily.iskautils.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.client.KeyBindings;
import net.unfamily.iskautils.data.BurningBrazierData;
import net.unfamily.iskautils.util.ArtifactTooltipUtil;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.stage.StageRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

/**
 * Burning Brazier Item - Places burning flame blocks when light level is low
 * Has 512 durability and consumes 1 durability per successful placement.
 */
public class BurningBrazierItem extends Item {

    public static final int MAX_DURABILITY = 512;
    private static final String CURSE_FLAME_STAGE = "iska_utils_internal-curse_flame";

    protected static boolean hasCurseFlame(ServerPlayer player) {
        return StageRegistry.playerHasStage(player, CURSE_FLAME_STAGE)
                || StageRegistry.playerTeamHasStage(player, CURSE_FLAME_STAGE)
                || StageRegistry.worldHasStage(player.level(), CURSE_FLAME_STAGE);
    }

    public BurningBrazierItem(Properties properties) {
        super(properties);
    }

    protected Block getFlameBlock() {
        return ModBlocks.BURNING_FLAME.get();
    }

    /** Flame blocks this item places (subtype per tool). */
    protected boolean isManagedFlame(Block block) {
        return block == getFlameBlock();
    }

    /** Both normal and cursed flame blocks can be picked up with brazier or candle. */
    protected static boolean isModFlameBlock(Block block) {
        return block == ModBlocks.BURNING_FLAME.get() || block == ModBlocks.CURSED_BURNING_FLAME.get();
    }

    /** Only normal burning flames restore brazier durability (cursed flames do not). */
    protected boolean restoresDurabilityFromFlameBlock(Block block) {
        return consumesDurability() && block == ModBlocks.BURNING_FLAME.get();
    }

    protected boolean consumesDurability() {
        return true;
    }

    protected boolean shouldBurnPlayerOnPlace(ServerPlayer player) {
        return Config.burningBrazierSuperHot || hasCurseFlame(player);
    }

    protected boolean canAutoPlace(ServerPlayer player, ServerLevel level, ItemStack stack) {
        return !consumesDurability() || stack.getDamageValue() < MAX_DURABILITY;
    }

    protected void onFlamePlaced(ServerPlayer player, ItemStack stack) {
        if (consumesDurability()) {
            stack.setDamageValue(stack.getDamageValue() + 1);
        }
    }

    protected void onFlameRemoved(ServerPlayer player, ItemStack stack, Block removedFlame) {
        if (restoresDurabilityFromFlameBlock(removedFlame)) {
            stack.setDamageValue(Math.max(0, stack.getDamageValue() - 1));
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        var player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockPos clickedPos = context.getClickedPos();

        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }

        BlockState clickedState = level.getBlockState(clickedPos);
        Block clickedBlock = clickedState.getBlock();
        if (isModFlameBlock(clickedBlock)) {
            level.destroyBlock(clickedPos, false);
            onFlameRemoved(serverPlayer, stack, clickedBlock);
            return InteractionResult.SUCCESS;
        }

        if (consumesDurability() && stack.getDamageValue() >= MAX_DURABILITY) {
            return InteractionResult.FAIL;
        }

        BlockPos placePos = clickedPos.above();
        BlockState targetState = level.getBlockState(placePos);
        if (!targetState.isAir()) {
            return InteractionResult.FAIL;
        }

        int maxBrightness = level.getMaxLocalRawBrightness(clickedPos);
        if (maxBrightness >= 8) {
            return InteractionResult.FAIL;
        }

        level.setBlock(placePos, getFlameBlock().defaultBlockState(), 3);

        if (shouldBurnPlayerOnPlace(serverPlayer)) {
            serverPlayer.setRemainingFireTicks(5 * 20);
        }

        onFlamePlaced(serverPlayer, stack);
        return InteractionResult.SUCCESS;
    }

    public static void tickEquipped(ServerPlayer player, ServerLevel level, ItemStack stack) {
        if (!(stack.getItem() instanceof BurningBrazierItem brazier)) {
            return;
        }
        brazier.tickEquippedItem(player, level, stack);
    }

    protected void tickEquippedItem(ServerPlayer player, ServerLevel level, ItemStack stack) {
        if (consumesDurability() && stack.isDamaged() && level.getGameTime() % 20 == 0) {
            var flameItem = ModBlocks.BURNING_FLAME.get().asItem();
            Inventory inv = player.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack slotStack = inv.getItem(i);
                if (!slotStack.isEmpty() && slotStack.is(flameItem)) {
                    slotStack.shrink(1);
                    stack.setDamageValue(Math.max(0, stack.getDamageValue() - 1));
                    break;
                }
            }
        }

        if (!BurningBrazierData.getAutoPlacementEnabledFromPlayer(player)) {
            return;
        }
        if (!canAutoPlace(player, level, stack)) {
            return;
        }
        if (level.getGameTime() % 40 != 0) {
            return;
        }

        BlockPos playerPos = player.blockPosition();
        int maxLocalBrightness = level.getMaxLocalRawBrightness(playerPos);
        boolean isPositionEmpty = level.isEmptyBlock(playerPos);
        if (maxLocalBrightness > 8 || !isPositionEmpty) {
            return;
        }

        level.setBlock(playerPos, getFlameBlock().defaultBlockState(), 3);

        if (shouldBurnPlayerOnPlace(player)) {
            player.setRemainingFireTicks(5 * 20);
        }
        onFlamePlaced(player, stack);
    }

    /** Lang path {@code tooltip.iska_utils.<path>.descN}: desc0 lore, desc1+ mechanical. */
    protected String flamesTooltipPath() {
        return "burning_brazier";
    }

    protected void appendFlameTooltip(java.util.function.Consumer<Component> tooltip) {
        String keybindName = KeyBindings.BURNING_BRAZIER_TOGGLE_KEY.getTranslatedKeyMessage().getString();
        ArtifactTooltipUtil.appendDescLines(tooltip, flamesTooltipPath(), 1, 2, keybindName);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        appendFlameTooltip(tooltip::add);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }
}

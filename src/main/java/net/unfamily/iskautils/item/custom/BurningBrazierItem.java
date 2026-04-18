package net.unfamily.iskautils.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.client.KeyBindings;
import net.unfamily.iskautils.data.BurningBrazierData;
import net.unfamily.iskautils.Config;
import net.unfamily.iskalib.stage.StageRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;

/**
 * Burning Brazier Item - Places burning flame blocks when light level is low
 * Has 512 durability and consumes 1 durability per successful placement.
 */
public class BurningBrazierItem extends Item {

    private static final Logger LOGGER = LoggerFactory.getLogger(BurningBrazierItem.class);
    private static final int MAX_DURABILITY = 512;
    private static final String CURSE_FLAME_STAGE = "iska_utils_internal-curse_flame";

    /** Curse flame is scalable: player (single), team (player's team), world (everyone). */
    private static boolean hasCurseFlame(ServerPlayer player) {
        return StageRegistry.playerHasStage(player, CURSE_FLAME_STAGE)
                || StageRegistry.playerTeamHasStage(player, CURSE_FLAME_STAGE)
                || StageRegistry.worldHasStage(player.level(), CURSE_FLAME_STAGE);
    }

    public BurningBrazierItem(Properties properties) {
        super(properties.durability(MAX_DURABILITY));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        var player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockPos clickedPos = context.getClickedPos();

        // Only work on server side
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }

        // Get the clicked block state
        BlockState clickedState = level.getBlockState(clickedPos);

        // If clicking on a burning flame, remove it and restore durability
        if (clickedState.getBlock() == ModBlocks.BURNING_FLAME.get()) {
            level.destroyBlock(clickedPos, false);
            stack.setDamageValue(Math.max(0, stack.getDamageValue() - 1));
            return InteractionResult.SUCCESS;
        }

        // Check if item has durability left
        if (stack.getDamageValue() >= MAX_DURABILITY) {
            return InteractionResult.FAIL;
        }

        // Get the position where we want to place the flame (above the clicked block)
        BlockPos placePos = clickedPos.above();

        // Check if the target position is air
        BlockState targetState = level.getBlockState(placePos);
        if (!targetState.isAir()) {
            return InteractionResult.FAIL;
        }

        // Check max local brightness at the clicked position (not the place position)
        int maxBrightness = level.getMaxLocalRawBrightness(clickedPos);
        if (maxBrightness >= 8) {
            return InteractionResult.FAIL;
        }

        // Place the burning flame block
        BlockState flameState = ModBlocks.BURNING_FLAME.get().defaultBlockState();
        level.setBlock(placePos, flameState, 3);

        // If super hot mode is enabled OR curse_flame on player/team/world, set the player on fire
        boolean shouldBurn = Config.burningBrazierSuperHot
                || hasCurseFlame(serverPlayer);

        if (shouldBurn) {
            serverPlayer.setRemainingFireTicks(5 * 20); // 5 seconds of fire (5 * 20 ticks)
        }

        // Consume durability
        stack.setDamageValue(stack.getDamageValue() + 1);

        return InteractionResult.SUCCESS;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @org.jspecify.annotations.Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);

        if (!(entity instanceof ServerPlayer player)) return;

        // Check if auto-placement is enabled for this player
        if (!BurningBrazierData.getAutoPlacementEnabledFromPlayer(player)) {
            return; // Auto-placement is disabled
        }

        // Check if item has durability left
        if (stack.getDamageValue() >= MAX_DURABILITY) {
            return; // No durability left, don't place flames automatically
        }

        // Only tick occasionally (every 40 ticks = 2 seconds)
        if (level.getGameTime() % 40 != 0) {
            return;
        }

        BlockPos feet = player.blockPosition();
        BlockPos flamePos = null;
        for (int dy = 1; dy <= 4; dy++) {
            BlockPos candidate = feet.above(dy);
            if (!level.isEmptyBlock(candidate)) {
                continue;
            }
            if (level.getMaxLocalRawBrightness(candidate) > 8) {
                continue;
            }
            flamePos = candidate;
            break;
        }

        if (flamePos == null) {
            return;
        }

        // Place burning flame
        BlockState flameState = ModBlocks.BURNING_FLAME.get().defaultBlockState();
        level.setBlock(flamePos, flameState, 3);

        // If super hot mode is enabled OR curse_flame on player/team/world, set the player on fire
        boolean shouldBurn = Config.burningBrazierSuperHot
                || hasCurseFlame(player);

        if (shouldBurn) {
            player.setRemainingFireTicks(5 * 20); // 5 seconds of fire (5 * 20 ticks)
        }

        // Consume durability (reduce by 1)
        stack.setDamageValue(stack.getDamageValue() + 1);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltip, flag);

        // Get the keybind name
        String keybindName = KeyBindings.BURNING_BRAZIER_TOGGLE_KEY.getTranslatedKeyMessage().getString();

        // Show description
        tooltip.accept(Component.translatable("tooltip.iska_utils.burning_brazier.desc0"));
        tooltip.accept(Component.translatable("tooltip.iska_utils.burning_brazier.desc1", keybindName));
        tooltip.accept(Component.translatable("tooltip.iska_utils.burning_brazier.desc2"));
    }
}

package net.unfamily.iskautils.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.client.KeyBindings;
import net.unfamily.iskautils.data.BurningBrazierData;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.stage.StageRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Burning Brazier Item - Places burning flame blocks when light level is low
 * Has 512 durability and consumes 1 durability per successful placement.
 */
public class BurningBrazierItem extends Item {

    private static final Logger LOGGER = LoggerFactory.getLogger(BurningBrazierItem.class);
    private static final int MAX_DURABILITY = 512;

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
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
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

        // If super hot mode is enabled OR player has flame curse stage, set the player on fire
        boolean shouldBurn = Config.burningBrazierSuperHot;

        if (!shouldBurn) {
            // Check if player has the flame curse stage
            shouldBurn = StageRegistry.playerHasStage(serverPlayer, "iska_utils_internal-flame_curse");
        }

        if (shouldBurn) {
            serverPlayer.setRemainingFireTicks(5 * 20); // 5 seconds of fire (5 * 20 ticks)
        }

        // Consume durability
        stack.setDamageValue(stack.getDamageValue() + 1);

        return InteractionResult.SUCCESS;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        // Only work on server side and only if entity is a player
        if (level.isClientSide || !(entity instanceof ServerPlayer player)) {
            return;
        }

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

        BlockPos playerPos = player.blockPosition();

        // Check if the area is dark enough and position is valid
        int maxLocalBrightness = level.getMaxLocalRawBrightness(playerPos);
        boolean isPositionEmpty = level.isEmptyBlock(playerPos);

        if (maxLocalBrightness > 8 || !isPositionEmpty) {
            return; // Conditions not met, don't place flame
        }

        // Place flame at player's position
        BlockPos flamePos = playerPos;

        // Place burning flame
        BlockState flameState = ModBlocks.BURNING_FLAME.get().defaultBlockState();
        level.setBlock(flamePos, flameState, 3);

        // If super hot mode is enabled OR player has flame curse stage, set the player on fire
        boolean shouldBurn = Config.burningBrazierSuperHot;

        if (!shouldBurn) {
            // Check if player has the flame curse stage
            shouldBurn = StageRegistry.playerHasStage(player, "iska_utils_internal-flame_curse");
        }

        if (shouldBurn) {
            player.setRemainingFireTicks(5 * 20); // 5 seconds of fire (5 * 20 ticks)
        }

        // Consume durability (reduce by 1)
        stack.setDamageValue(stack.getDamageValue() + 1);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        // Get the keybind name
        String keybindName = KeyBindings.BURNING_BRAZIER_TOGGLE_KEY.getTranslatedKeyMessage().getString();

        // Show description
        tooltip.add(Component.translatable("tooltip.iska_utils.burning_brazier.desc0"));
        tooltip.add(Component.translatable("tooltip.iska_utils.burning_brazier.desc1", keybindName));
        tooltip.add(Component.translatable("tooltip.iska_utils.burning_brazier.desc2"));
    }


    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }
}

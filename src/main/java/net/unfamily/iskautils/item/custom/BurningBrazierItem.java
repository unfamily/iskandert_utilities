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
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.client.KeyBindings;
import net.unfamily.iskautils.Config;
import net.unfamily.iskalib.stage.StageRegistry;
import org.jspecify.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Burning Brazier: manual placement with {@link #useOn}; optional auto-placement only while
 * {@link #isAutoPlacementEnabled(UUID)} is true for that player (default {@code false} until toggled).
 * Auto runs on this stack's
 * {@link #inventoryTick} only (no inventory/Curio scanning). Toggle is in-memory per player UUID, not saved.
 * Manual use ignores light; auto only when {@link LightLayer#BLOCK} at the feet cell is {@code < 8} (mob-spawn style dim for artificial light).
 */
public class BurningBrazierItem extends Item {

    public static final int MAX_DURABILITY = 512;
    private static final String CURSE_FLAME_STAGE = "iska_utils_internal-curse_flame";

    /** In-memory only; not persisted. Absent UUID defaults to {@code false} (manual only until toggled on). */
    private static final ConcurrentHashMap<UUID, Boolean> AUTO_PLACEMENT_BY_PLAYER = new ConcurrentHashMap<>();

    public static boolean isAutoPlacementEnabled(UUID playerId) {
        return AUTO_PLACEMENT_BY_PLAYER.getOrDefault(playerId, false);
    }

    public static void setAutoPlacementEnabled(UUID playerId, boolean enabled) {
        AUTO_PLACEMENT_BY_PLAYER.put(playerId, enabled);
    }

    /** Toggles auto-placement and returns the new value. */
    public static boolean toggleAutoPlacement(ServerPlayer player) {
        UUID id = player.getUUID();
        boolean next = !isAutoPlacementEnabled(id);
        AUTO_PLACEMENT_BY_PLAYER.put(id, next);
        return next;
    }

    public static void clearAutoPlacementState(UUID playerId) {
        AUTO_PLACEMENT_BY_PLAYER.remove(playerId);
    }

    private static boolean hasCurseFlame(ServerPlayer player) {
        return StageRegistry.playerHasStage(player, CURSE_FLAME_STAGE)
                || StageRegistry.playerTeamHasStage(player, CURSE_FLAME_STAGE)
                || StageRegistry.worldHasStage(player.level(), CURSE_FLAME_STAGE);
    }

    public BurningBrazierItem(Properties properties) {
        super(properties.durability(MAX_DURABILITY));
    }

    /** Auto: only when artificial (block-emitted) light at {@code pos} is in mob-spawn range ({@code < 8}, i.e. 0–7). */
    private static boolean blockLightAllowsAutoFlame(Level level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return false;
        }
        return level.getBrightness(LightLayer.BLOCK, pos) < 8;
    }

    /** No placement when worn out ({@code damage >= max}); if max is unknown ({@code <= 0}), do not block. */
    private static boolean isDepletedForPlacement(ItemStack stack) {
        int max = stack.getMaxDamage();
        if (max <= 0) {
            return false;
        }
        return stack.getDamageValue() >= max;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        var player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockPos clickedPos = context.getClickedPos();

        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }

        BlockState clickedState = level.getBlockState(clickedPos);

        if (clickedState.getBlock() == ModBlocks.BURNING_FLAME.get()) {
            level.destroyBlock(clickedPos, false);
            stack.setDamageValue(Math.max(0, stack.getDamageValue() - 1));
            return InteractionResult.SUCCESS;
        }

        if (isDepletedForPlacement(stack)) {
            return InteractionResult.FAIL;
        }

        BlockPos placePos = clickedPos.above();
        BlockState targetState = level.getBlockState(placePos);
        if (!targetState.isAir()) {
            return InteractionResult.FAIL;
        }

        BlockState flameState = ModBlocks.BURNING_FLAME.get().defaultBlockState();
        level.setBlock(placePos, flameState, 3);

        boolean shouldBurn = Config.burningBrazierSuperHot || hasCurseFlame(serverPlayer);
        if (shouldBurn) {
            serverPlayer.setRemainingFireTicks(5 * 20);
        }

        stack.setDamageValue(stack.getDamageValue() + 1);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        if (!isAutoPlacementEnabled(player.getUUID())) {
            return;
        }
        if (isDepletedForPlacement(stack)) {
            return;
        }
        if (level.getGameTime() % 40 != 0) {
            return;
        }

        BlockPos pos = player.blockPosition();
        if (!blockLightAllowsAutoFlame(level, pos)) {
            return;
        }

        BlockState existing = level.getBlockState(pos);
        if (!existing.isAir()) {
            return;
        }

        BlockState flameState = ModBlocks.BURNING_FLAME.get().defaultBlockState();
        if (!level.setBlock(pos, flameState, 3)) {
            return;
        }

        boolean shouldBurn = Config.burningBrazierSuperHot || hasCurseFlame(player);
        if (shouldBurn) {
            player.setRemainingFireTicks(5 * 20);
        }
        stack.setDamageValue(stack.getDamageValue() + 1);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltip, flag);
        String keybindName = KeyBindings.BURNING_BRAZIER_TOGGLE_KEY.getTranslatedKeyMessage().getString();
        tooltip.accept(Component.translatable("tooltip.iska_utils.burning_brazier.desc0"));
        tooltip.accept(Component.translatable("tooltip.iska_utils.burning_brazier.desc1", keybindName));
        tooltip.accept(Component.translatable("tooltip.iska_utils.burning_brazier.desc2"));
    }
}

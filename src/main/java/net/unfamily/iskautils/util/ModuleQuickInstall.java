package net.unfamily.iskautils.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.entity.BlazingAltarBlockEntity;
import net.unfamily.iskautils.block.entity.CollectingCrateBlockEntity;
import net.unfamily.iskautils.block.entity.ClimbingNullifierBlockEntity;
import net.unfamily.iskautils.block.entity.EnderNullifierBlockEntity;
import net.unfamily.iskautils.block.entity.FlightNullifierBlockEntity;
import net.unfamily.iskautils.block.entity.EntropicSpawnerBlockEntity;
import net.unfamily.iskautils.block.entity.FanBlockEntity;
import net.unfamily.iskautils.block.entity.ImprovedPatternCrafterBlockEntity;
import net.unfamily.iskautils.block.entity.MobReaperBlockEntity;
import net.unfamily.iskautils.block.entity.SoulNullifierBlockEntity;
import net.unfamily.iskautils.block.entity.TemporalOverclockerBlockEntity;
import net.unfamily.iskautils.block.entity.WanderNullifierBlockEntity;
import net.unfamily.iskautils.item.ModItems;

/**
 * Shift+right-click install of upgrade modules into machine module slots without opening the GUI.
 * Inserts up to each slot's limit while respecting slot validation.
 */
public final class ModuleQuickInstall {

    private ModuleQuickInstall() {}

    /** True for iska_utils upgrade modules and entropic clock (overclocker / spawner upgrade). */
    public static boolean isKnownModuleItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null || !IskaUtils.MOD_ID.equals(id.getNamespace())) {
            return false;
        }
        String path = id.getPath();
        return path.endsWith("_module") || path.equals("entropic_clock");
    }

    /**
     * Attempts to install from the held stack into the block entity at {@code pos}.
     * Consumes from the hand on the server (unless creative). Returns true when insertion is possible
     * (client) or when any items were inserted (server).
     */
    public static boolean tryInstall(Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (level == null || pos == null || player == null || hand == null) {
            return false;
        }
        if (!player.isShiftKeyDown()) {
            return false;
        }
        ItemStack held = player.getItemInHand(hand);
        if (!isKnownModuleItem(held)) {
            return false;
        }
        BlockEntity be = level.getBlockEntity(pos);
        IItemHandler modules = resolveModuleHandler(be);
        if (modules == null) {
            return false;
        }

        if (level.isClientSide) {
            return canInsertAny(modules, held, be);
        }

        ItemStack toInsert = held.copy();
        int before = toInsert.getCount();
        ItemStack remaining = insertRespectingValidation(modules, toInsert, be, false);
        int installed = before - remaining.getCount();
        if (installed <= 0) {
            return false;
        }

        if (!player.getAbilities().instabuild) {
            held.shrink(installed);
            if (held.isEmpty()) {
                player.setItemInHand(hand, ItemStack.EMPTY);
            }
        }
        level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.6f, 1.1f);
        be.setChanged();
        level.sendBlockUpdated(pos, be.getBlockState(), be.getBlockState(), 3);
        return true;
    }

    private static IItemHandler resolveModuleHandler(BlockEntity be) {
        if (be == null) {
            return null;
        }
        if (be instanceof FanBlockEntity fan) {
            return fan.getModuleHandler();
        }
        if (be instanceof MobReaperBlockEntity reaper) {
            return reaper.getModuleHandler();
        }
        if (be instanceof EnderNullifierBlockEntity nullifier) {
            return nullifier.getModuleHandler();
        }
        if (be instanceof FlightNullifierBlockEntity flightNullifier) {
            return flightNullifier.getModuleHandler();
        }
        if (be instanceof ClimbingNullifierBlockEntity climbingNullifier) {
            return climbingNullifier.getModuleHandler();
        }
        if (be instanceof WanderNullifierBlockEntity nullifier) {
            return nullifier.getModuleHandler();
        }
        if (be instanceof SoulNullifierBlockEntity nullifier) {
            return nullifier.getModuleHandler();
        }
        if (be instanceof CollectingCrateBlockEntity crate) {
            return crate.getModuleHandler();
        }
        if (be instanceof BlazingAltarBlockEntity altar) {
            return altar.getModuleHandler();
        }
        if (be instanceof ImprovedPatternCrafterBlockEntity crafter) {
            return crafter.getUpgradeHandler();
        }
        if (be instanceof EntropicSpawnerBlockEntity spawner) {
            return spawner.getMachineItems();
        }
        if (be instanceof TemporalOverclockerBlockEntity overclocker) {
            return overclocker.getItemHandler();
        }
        return null;
    }

    private static boolean canInsertAny(IItemHandler handler, ItemStack stack, BlockEntity be) {
        ItemStack remaining = insertRespectingValidation(handler, stack.copy(), be, true);
        return remaining.getCount() < stack.getCount();
    }

    /**
     * Inserts as many as possible into valid slots. Pattern Crafter upgrade slots lack
     * {@code isItemValid} overrides, so they use dedicated type checks.
     */
    private static ItemStack insertRespectingValidation(
            IItemHandler handler, ItemStack stack, BlockEntity be, boolean simulate) {
        ItemStack remaining = stack.copy();
        for (int slot = 0; slot < handler.getSlots() && !remaining.isEmpty(); slot++) {
            if (!canPlaceInSlot(handler, slot, remaining, be)) {
                continue;
            }
            remaining = handler.insertItem(slot, remaining, simulate);
        }
        return remaining;
    }

    private static boolean canPlaceInSlot(IItemHandler handler, int slot, ItemStack stack, BlockEntity be) {
        if (handler.getSlotLimit(slot) <= 0) {
            return false;
        }
        if (be instanceof ImprovedPatternCrafterBlockEntity) {
            return isValidPatternCrafterUpgrade(slot, stack);
        }
        if (be instanceof EntropicSpawnerBlockEntity) {
            return (slot == EntropicSpawnerBlockEntity.CLOCK_SLOT_INDEX
                    || slot == EntropicSpawnerBlockEntity.PRODUCTION_SLOT_INDEX)
                    && handler.isItemValid(slot, stack);
        }
        if (be instanceof TemporalOverclockerBlockEntity) {
            return slot == TemporalOverclockerBlockEntity.UPGRADE_SLOT_INDEX
                    && handler.isItemValid(slot, stack);
        }
        return handler.isItemValid(slot, stack);
    }

    /** Mirrors {@link net.unfamily.iskautils.client.gui.UpgradeSlot#mayPlace}. */
    private static boolean isValidPatternCrafterUpgrade(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return switch (slot) {
            case 0 -> stack.is(ModItems.LOGIC_MODULE.get());
            case 1 -> isSpeedModule(stack);
            case 2 -> stack.is(ModItems.PRODUCTION_MODULE.get());
            default -> false;
        };
    }

    private static boolean isSpeedModule(ItemStack stack) {
        return stack.is(ModItems.SLOW_MODULE.get())
                || stack.is(ModItems.MODERATE_MODULE.get())
                || stack.is(ModItems.FAST_MODULE.get())
                || stack.is(ModItems.EXTREME_MODULE.get())
                || stack.is(ModItems.ULTRA_MODULE.get());
    }
}

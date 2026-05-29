package net.unfamily.iskautils.events;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.unfamily.iskautils.item.custom.DurableShearsItem;

import java.util.List;

@EventBusSubscriber
public final class DurableShearsEvents {
    private DurableShearsEvents() {}

    private static boolean isDurableShears(ItemStack stack) {
        return stack.getItem() instanceof DurableShearsItem;
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (!isDurableShears(stack)) {
            return;
        }
        if (!DurableShearsItem.isShearHarvestable(event.getState())) {
            event.setNewSpeed(0.0F);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (!isDurableShears(stack)) {
            return;
        }
        if (!DurableShearsItem.isShearHarvestable(event.getState())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onBlockDrops(BlockDropsEvent event) {
        ItemStack tool = event.getTool();
        if (!isDurableShears(tool)) {
            return;
        }

        BlockState state = event.getState();
        if (!DurableShearsItem.isShearHarvestable(state)) {
            return;
        }

        ServerLevel level = (ServerLevel) event.getLevel();
        BlockEntity blockEntity = event.getBlockEntity();
        Entity breaker = event.getBreaker();

        ItemStack silkProbe = tool.copy();
        silkProbe.enchant(
                level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.SILK_TOUCH),
                1);
        List<ItemStack> silkDrops = Block.getDrops(state, level, event.getPos(), blockEntity, breaker, silkProbe);

        event.getDrops().clear();
        for (ItemStack drop : silkDrops) {
            if (drop.isEmpty()) {
                continue;
            }
            ItemEntity itemEntity = new ItemEntity(
                    level,
                    event.getPos().getX() + 0.5D,
                    event.getPos().getY() + 0.5D,
                    event.getPos().getZ() + 0.5D,
                    drop);
            event.getDrops().add(itemEntity);
        }
    }
}

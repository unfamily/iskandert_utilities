package net.unfamily.iskautils.item.custom;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.entity.vehicle.MinecartTNT;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.iskautils.entity.PrimedEntropyTnt;
import net.unfamily.iskautils.item.ModItems;

import java.util.List;

/**
 * Defuses primed TNT (vanilla or modded) and TNT minecarts.
 * Use via {@link net.unfamily.iskautils.events.SaboteurScrewdriverEvents}.
 */
public class SaboteurScrewdriverItem extends Item {

    public SaboteurScrewdriverItem(Properties properties) {
        super(properties);
    }

    public static boolean tryDefuse(ServerLevel level, Player player, PrimedTnt primed, ItemStack tool, InteractionHand hand) {
        ItemStack drop = dropFromPrimed(primed);

        double x = primed.getX();
        double y = primed.getY();
        double z = primed.getZ();
        var soundPos = primed.blockPosition();
        primed.discard();

        level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(level, x, y, z, drop));
        level.playSound(null, soundPos, SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.8F, 1.4F);

        if (!player.getAbilities().instabuild) {
            tool.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        }
        return true;
    }

    public static boolean tryDefuseMinecart(ServerLevel level, Player player, MinecartTNT cart, ItemStack tool, InteractionHand hand) {
        double x = cart.getX();
        double y = cart.getY();
        double z = cart.getZ();
        var soundPos = cart.blockPosition();

        Minecart empty = new Minecart(level, x, y, z);
        empty.setYRot(cart.getYRot());
        empty.setXRot(cart.getXRot());
        empty.setDeltaMovement(cart.getDeltaMovement());
        if (cart.hasCustomName()) {
            empty.setCustomName(cart.getCustomName());
            empty.setCustomNameVisible(cart.isCustomNameVisible());
        }

        cart.ejectPassengers();
        cart.discard();

        level.addFreshEntity(empty);
        level.addFreshEntity(new ItemEntity(level, x, y + 0.5D, z, new ItemStack(Items.TNT)));
        level.playSound(null, soundPos, SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.8F, 1.4F);

        if (!player.getAbilities().instabuild) {
            tool.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        }
        return true;
    }

    /**
     * Prefer non-vanilla primed block state {@code asItem()}. Vanilla TNT state falls through to
     * entity-type heuristics (e.g. AE2 Tiny TNT). Empty / AIR registry hits are treated as misses.
     */
    private static ItemStack dropFromPrimed(PrimedTnt primed) {
        if (primed instanceof PrimedEntropyTnt) {
            return new ItemStack(ModItems.ENTROPY_TNT.get());
        }

        BlockState state = primed.getBlockState();
        boolean stateIsVanillaTnt = state.is(Blocks.TNT);
        if (!stateIsVanillaTnt) {
            ItemStack fromState = stackOrEmpty(state.getBlock().asItem());
            if (!fromState.isEmpty()) {
                return fromState;
            }
        }

        ItemStack fromType = dropFromEntityType(primed);
        if (!fromType.isEmpty()) {
            return fromType;
        }

        if (!stateIsVanillaTnt) {
            ItemStack fromState = stackOrEmpty(state.getBlock().asItem());
            if (!fromState.isEmpty()) {
                return fromState;
            }
        }

        return new ItemStack(Items.TNT);
    }

    private static ItemStack dropFromEntityType(PrimedTnt primed) {
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(primed.getType());
        if (entityId == null || EntityType.TNT.equals(primed.getType())) {
            return ItemStack.EMPTY;
        }

        String path = entityId.getPath();
        String candidate = stripPrimedPath(path);
        if (candidate == null) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = lookupItem(entityId.getNamespace(), candidate);
        if (!stack.isEmpty()) {
            return stack;
        }

        // Common alternate ids when primed path does not map 1:1
        if ("tiny_tnt".equals(candidate) || path.contains("tiny_tnt")) {
            stack = lookupItem(entityId.getNamespace(), "tiny_tnt");
            if (!stack.isEmpty()) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }

    private static String stripPrimedPath(String path) {
        if (path.endsWith("_tnt_primed")) {
            return path.substring(0, path.length() - "_tnt_primed".length()) + "_tnt";
        }
        if (path.endsWith("_primed")) {
            return path.substring(0, path.length() - "_primed".length());
        }
        if (path.startsWith("primed_")) {
            return path.substring("primed_".length());
        }
        return null;
    }

    private static ItemStack lookupItem(String namespace, String path) {
        ResourceLocation itemId = ResourceLocation.fromNamespaceAndPath(namespace, path);
        return BuiltInRegistries.ITEM.getOptional(itemId)
                .map(SaboteurScrewdriverItem::stackOrEmpty)
                .orElse(ItemStack.EMPTY);
    }

    private static ItemStack stackOrEmpty(Item item) {
        if (item == null || item == Items.AIR) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(item);
        return stack.isEmpty() ? ItemStack.EMPTY : stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.iska_utils.saboteur_screwdriver.desc0"));
        tooltip.add(Component.translatable("tooltip.iska_utils.saboteur_screwdriver.desc1"));
    }
}

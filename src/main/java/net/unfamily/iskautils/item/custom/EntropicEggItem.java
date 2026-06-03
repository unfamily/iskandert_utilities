package net.unfamily.iskautils.item.custom;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.TooltipFlag;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.util.EntropicEmpowermentUtil;

import java.util.List;

public class EntropicEggItem extends Item {
    public EntropicEggItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player.level() instanceof ServerLevel server)) {
            return InteractionResult.PASS;
        }
        if (!(target instanceof Mob mob)) {
            return InteractionResult.PASS;
        }
        if (tryUseOnMob(server, player, mob, stack, hand)) {
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    private static boolean tryUseOnMob(ServerLevel level, Player player, Mob mob, ItemStack entropicEgg, InteractionHand hand) {
        SpawnEggItem spawnEgg = SpawnEggItem.byId(mob.getType());
        if (spawnEgg != null) {
            EntropicEmpowermentUtil.apply(mob, true);

            ItemStack eggReward = new ItemStack(spawnEgg);
            if (!player.addItem(eggReward)) {
                player.drop(eggReward, false);
            }

            playUseEffects(level, mob);
            consumeEntropicEgg(player, entropicEgg, hand);
            return true;
        }

        if (Config.entropicEggAlwaysConsume) {
            consumeEntropicEgg(player, entropicEgg, hand);
            return true;
        }

        return false;
    }

    private static void consumeEntropicEgg(Player player, ItemStack entropicEgg, InteractionHand hand) {
        if (player.getAbilities().instabuild) {
            return;
        }
        ItemStack inHand = player.getItemInHand(hand);
        if (inHand.is(entropicEgg.getItem())) {
            inHand.shrink(1);
        }
    }

    private static void playUseEffects(ServerLevel level, Mob mob) {
        level.playSound(null, mob.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 0.65F, 1.15F);
        double x = mob.getX();
        double y = mob.getY() + mob.getBbHeight() * 0.5D;
        double z = mob.getZ();
        level.sendParticles(ParticleTypes.WITCH, x, y, z, 10, 0.25D, 0.2D, 0.25D, 0.02D);
        level.sendParticles(ParticleTypes.PORTAL, x, y, z, 6, 0.2D, 0.15D, 0.2D, 0.4D);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.iska_utils.entropic_egg.desc0"));
        if (Config.entropicEggApplyBuff) {
            tooltip.add(Component.translatable("tooltip.iska_utils.entropic_egg.desc_buff"));
        }
    }
}

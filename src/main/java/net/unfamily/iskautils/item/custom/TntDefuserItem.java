package net.unfamily.iskautils.item.custom;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.unfamily.iskautils.entity.PrimedEntropyTnt;
import net.unfamily.iskautils.item.ModItems;

/**
 * Defuses primed vanilla or entropy TNT, dropping the corresponding block item.
 * Use via {@link net.unfamily.iskautils.events.TntDefuserEvents} (PrimedTnt is not a LivingEntity).
 */
public class TntDefuserItem extends Item {

    public TntDefuserItem(Properties properties) {
        super(properties);
    }

    public static boolean tryDefuse(ServerLevel level, Player player, PrimedTnt primed, ItemStack tool, InteractionHand hand) {
        ItemStack drop = primed instanceof PrimedEntropyTnt
                ? new ItemStack(ModItems.ENTROPY_TNT.get())
                : new ItemStack(Items.TNT);

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
}

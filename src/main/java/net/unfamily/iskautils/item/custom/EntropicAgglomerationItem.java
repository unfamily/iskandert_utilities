package net.unfamily.iskautils.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.unfamily.iskautils.entity.EntropicCreeper;
import net.unfamily.iskautils.entity.ModEntities;
import net.unfamily.iskautils.events.EntropicAgglomerationSpreadHandler;
import net.unfamily.iskautils.util.EntropicSoilUtil;

import java.util.function.Consumer;

public class EntropicAgglomerationItem extends Item {
    public EntropicAgglomerationItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState clicked = level.getBlockState(pos);
        if (!EntropicSoilUtil.isConvertible(clicked)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.PASS;
        }
        return tryConvert(server, pos, stack, context.getPlayer());
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof Creeper creeper) || target instanceof EntropicCreeper) {
            return InteractionResult.PASS;
        }
        Level level = player.level();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.PASS;
        }

        EntropicCreeper entropic = ModEntities.ENTROPIC_CREEPER.get().create(server, EntitySpawnReason.CONVERSION);
        if (entropic == null) {
            return InteractionResult.FAIL;
        }

        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, server.registryAccess());
        creeper.saveWithoutId(output);
        CompoundTag data = output.buildResult();
        data.remove("UUID");
        var input = TagValueInput.create(ProblemReporter.DISCARDING, server.registryAccess(), data);
        entropic.load(input);
        entropic.snapTo(creeper.getX(), creeper.getY(), creeper.getZ(), creeper.getYRot(), creeper.getXRot());
        entropic.setYHeadRot(creeper.getYHeadRot());
        entropic.setYBodyRot(creeper.yBodyRot);

        creeper.discard();
        server.addFreshEntity(entropic);
        server.playSound(null, entropic.getX(), entropic.getY(), entropic.getZ(),
                SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.HOSTILE, 1.0F, 1.0F);
        server.sendParticles(ParticleTypes.WITCH, entropic.getX(), entropic.getY() + 1.0D, entropic.getZ(),
                24, 0.4D, 0.5D, 0.4D, 0.02D);

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    public static InteractionResult tryConvert(ServerLevel server, BlockPos pos, ItemStack stack, Player player) {
        if (!EntropicAgglomerationSpreadHandler.enqueue(server, pos)) {
            return InteractionResult.PASS;
        }

        server.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 0.8F, 0.5F);
        server.sendParticles(ParticleTypes.WITCH, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                12, 0.35D, 0.15D, 0.35D, 0.02D);
        if (player != null && !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.translatable("tooltip.iska_utils.entropic_agglomeration.desc0"));
        tooltip.accept(Component.translatable("tooltip.iska_utils.entropic_agglomeration.desc1"));
        tooltip.accept(Component.translatable("tooltip.iska_utils.entropic_agglomeration.desc2"));
    }
}

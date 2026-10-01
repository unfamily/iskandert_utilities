package net.unfamily.iskautils.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.InfestedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.unfamily.iskautils.integration.exdeorum.ExDeorumCompat;
import net.unfamily.iskautils.item.ModItems;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class SilverfishLarvaItem extends Item {
    public SilverfishLarvaItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (InfestedBlock.isCompatibleHostBlock(state)) {
            BlockState infested = InfestedBlock.infestedStateByHost(state);
            level.setBlock(pos, infested, 3);
            consumeOne(stack, player);
            return InteractionResult.CONSUME;
        }

        if (ExDeorumCompat.isBarrel(state.getBlock())) {
            ResourceHandler<FluidResource> handler =
                    level.getCapability(Capabilities.Fluid.BLOCK, pos, context.getClickedFace());
            if (handler != null && hasWitchWater(handler)) {
                consumeOne(stack, player);
                if (player != null) {
                    player.getInventory().add(new ItemStack(ModItems.DROP_OF_ENTROPY.get()));
                }
                return InteractionResult.CONSUME;
            }
        }

        return InteractionResult.PASS;
    }

    private static boolean hasWitchWater(ResourceHandler<FluidResource> handler) {
        for (int slot = 0; slot < handler.size(); slot++) {
            if (handler.getAmountAsLong(slot) > 0L
                    && ExDeorumCompat.isWitchWater(handler.getResource(slot).getFluid())) {
                return true;
            }
        }
        return false;
    }

    private static void consumeOne(ItemStack stack, Player player) {
        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay tooltipDisplay,
            Consumer<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltip, flag);
        tooltip.accept(Component.translatable("tooltip.iska_utils.silverfish_larva.desc0"));
        tooltip.accept(Component.translatable("tooltip.iska_utils.silverfish_larva.desc1"));
    }
}

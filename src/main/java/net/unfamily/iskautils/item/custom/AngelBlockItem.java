package net.unfamily.iskautils.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * AngelBlockItem - Item per il blocco Angel Block
 * Permette di piazzare il blocco in aria con click destro
 */
public class AngelBlockItem extends BlockItem {
    
    public AngelBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    /**
     * Gestisce l'uso dell'item nel vuoto (click destro in aria)
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        
        if (level.isClientSide) {
            return InteractionResultHolder.success(itemStack);
        }
        
        // Ottiene la posizione davanti al giocatore
        Vec3 lookVec = player.getLookAngle();
        Vec3 placePos = player.getEyePosition().add(lookVec.scale(2.0)); // 2 blocchi davanti
        BlockPos blockPos = new BlockPos((int)Math.floor(placePos.x), (int)Math.floor(placePos.y), (int)Math.floor(placePos.z));
        
        // Verifica se la posizione è libera
        if (!level.getBlockState(blockPos).isAir()) {
            return InteractionResultHolder.fail(itemStack);
        }
        
        // Crea un contesto di piazzamento per il blocco
        BlockPlaceContext context = new BlockPlaceContext(level, player, hand, itemStack, 
                new BlockHitResult(placePos, Direction.UP, blockPos, false));
        
        // Piazza il blocco
        BlockState placedState = this.getBlock().getStateForPlacement(context);
        if (placedState != null && this.place(context).consumesAction()) {
            // Successo
            if (!player.getAbilities().instabuild) {
                itemStack.shrink(1);
            }
            return InteractionResultHolder.consume(itemStack);
        }
        
        return InteractionResultHolder.fail(itemStack);
    }
    
    /**
     * Mantiene anche la funzionalità standard di piazzamento su altri blocchi
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        return super.useOn(context);
    }
} 
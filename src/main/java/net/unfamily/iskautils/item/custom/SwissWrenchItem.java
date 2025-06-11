package net.unfamily.iskautils.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;

public class SwissWrenchItem extends Item {
    // Tag per i blocchi che non devono essere ruotati
    private static final TagKey<Block> WRENCH_NOT_ROTATE = BlockTags.create(
            ResourceLocation.tryParse("c:wrench_not_rotate"));

    public SwissWrenchItem(Properties properties) {
        super(properties);
    }
    
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos blockPos = context.getClickedPos();
        Player player = context.getPlayer();
        BlockState blockState = level.getBlockState(blockPos);
        Block block = blockState.getBlock();
        
        // Verifica se il player sta usando Shift + tasto destro
        if (player != null && player.isShiftKeyDown()) {
            // Verifica se il blocco appartiene al tag che esclude la rotazione
            if (blockState.is(WRENCH_NOT_ROTATE)) {
                return InteractionResult.PASS;
            }
            
            // Non facciamo nulla sul client, solo sul server
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            
            // Prova a ruotare il blocco in base alle sue proprietà
            boolean rotated = false;
            
            // Prova a ruotare blocchi con la proprietà FACING (6 direzioni complete)
            if (blockState.hasProperty(BlockStateProperties.FACING)) {
                Direction currentFacing = blockState.getValue(BlockStateProperties.FACING);
                Direction newFacing;
                
                // Determina la nuova direzione in base all'input del giocatore
                Direction lookingDirection = player.getDirection();
                
                // Se il giocatore guarda verso l'alto/basso, ruota in modo appropriato
                if (player.getXRot() > 45) { // Guardando in basso
                    newFacing = Direction.DOWN;
                } else if (player.getXRot() < -45) { // Guardando in alto
                    newFacing = Direction.UP;
                } else {
                    // Ruota orizzontalmente
                    if (currentFacing == Direction.UP || currentFacing == Direction.DOWN) {
                        // Se attualmente è verticale, orienta verso la direzione del giocatore
                        newFacing = lookingDirection;
                    } else {
                        // Altrimenti ruota orizzontalmente
                        if (player.isCrouching() && player.isShiftKeyDown()) {
                            // Se è premuto anche Ctrl (crouch), ruota in verticale
                            newFacing = (currentFacing == Direction.UP) ? Direction.DOWN : Direction.UP;
                        } else {
                            // Ruota orizzontalmente in senso orario
                            newFacing = currentFacing.getClockWise();
                        }
                    }
                }
                
                level.setBlock(blockPos, blockState.setValue(BlockStateProperties.FACING, newFacing), 3);
                rotated = true;
            } 
            // Prova a ruotare blocchi con la proprietà HORIZONTAL_FACING
            else if (blockState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                Direction currentFacing = blockState.getValue(BlockStateProperties.HORIZONTAL_FACING);
                Direction newFacing = currentFacing.getClockWise();
                level.setBlock(blockPos, blockState.setValue(BlockStateProperties.HORIZONTAL_FACING, newFacing), 3);
                rotated = true;
            }
            // Prova a ruotare blocchi con la proprietà FACING (HorizontalDirectionalBlock)
            else if (blockState.hasProperty(HorizontalDirectionalBlock.FACING)) {
                Direction currentFacing = blockState.getValue(HorizontalDirectionalBlock.FACING);
                Direction newFacing = currentFacing.getClockWise();
                level.setBlock(blockPos, blockState.setValue(HorizontalDirectionalBlock.FACING, newFacing), 3);
                rotated = true;
            }
            // Prova a ruotare blocchi con proprietà AXIS (come i tronchi)
            else if (blockState.hasProperty(BlockStateProperties.AXIS)) {
                rotated = rotateBlockOnAxis(level, blockPos, blockState);
            }
            
            // Se il blocco è stato ruotato, riproduci un suono
            if (rotated) {
                level.playSound(null, blockPos, SoundEvents.LANTERN_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                return InteractionResult.SUCCESS;
            }
        }
        
        return InteractionResult.PASS;
    }
    
    /**
     * Ruota un blocco basato sulla proprietà AXIS (come i tronchi)
     */
    private boolean rotateBlockOnAxis(Level level, BlockPos pos, BlockState state) {
        if (state.hasProperty(BlockStateProperties.AXIS)) {
            Direction.Axis currentAxis = state.getValue(BlockStateProperties.AXIS);
            Direction.Axis newAxis;
            
            // Ruota in sequenza X -> Z -> Y -> X
            switch (currentAxis) {
                case X -> newAxis = Direction.Axis.Z;
                case Z -> newAxis = Direction.Axis.Y;
                case Y -> newAxis = Direction.Axis.X;
                default -> newAxis = currentAxis;
            }
            
            level.setBlock(pos, state.setValue(BlockStateProperties.AXIS, newAxis), 3);
            return true;
        }
        
        return false;
    }
}
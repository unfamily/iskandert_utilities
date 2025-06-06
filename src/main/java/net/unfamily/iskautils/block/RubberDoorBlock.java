package net.unfamily.iskautils.block;

import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.core.BlockPos;
import net.unfamily.iskautils.util.ModWoodTypes;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class RubberDoorBlock extends DoorBlock {
    
    public RubberDoorBlock(Properties properties) {
        super(ModWoodTypes.RUBBER_SET_TYPE, properties);
    }
    
    @Override
    public int getLightBlock(BlockState state, BlockGetter world, BlockPos pos) {
        return 0;
    }
    
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter getter, BlockPos pos, CollisionContext context) {
        // Usa la forma di collisione normale della porta invece di ereditare il comportamento "noCollision"
        return this.getShape(state, getter, pos, context);
    }
} 
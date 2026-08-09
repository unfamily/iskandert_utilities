package net.unfamily.iskautils.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.unfamily.iskautils.item.custom.CrystalCageItem;

/**
 * Crystal Cage Trap Plate – a special (non-JSON) plate block.
 *
 * When a living entity (not a player) walks through it:
 *  1. The entity is captured into a filled Crystal Cage item.
 *  2. The cage is dropped at the plate's position.
 *  3. The plate reverts to a plain Plate Base.
 */
public class CrystalCageTrapPlateBlock extends VectorBlock {

    public static final MapCodec<CrystalCageTrapPlateBlock> CODEC =
            simpleCodec(CrystalCageTrapPlateBlock::new);

    public CrystalCageTrapPlateBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends CrystalCageTrapPlateBlock> codec() {
        return CODEC;
    }

    @Override
    protected void entityInside(BlockState state,
                                Level level,
                                BlockPos pos,
                                Entity entity,
                                InsideBlockEffectApplier effectApplier,
                                boolean isPrecise) {
        if (level.isClientSide()) return;
        if (!(entity instanceof LivingEntity living)) return;
        if (living instanceof Player) return;
        if (living.isRemoved()) return;

        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;

        if (CrystalCageItem.isCaptureBlacklisted(living)) return;

        // Capture mob into a filled Crystal Cage
        ItemStack cage = CrystalCageItem.createFilledCage(living, serverLevel);
        if (cage.isEmpty()) return;

        CrystalCageItem.spawnPurpleFireBurst(serverLevel,
                living.getX(), living.getY() + living.getBbHeight() * 0.5D, living.getZ());

        // Discard entity before dropping so death logic doesn't interfere
        living.discard();

        // Drop cage at the plate center
        net.minecraft.world.level.block.Block.popResource(level, pos, cage);

        // Revert to plain Plate Base, keeping facing & vertical state
        BlockState baseState = ModBlocks.PLATE_BASE_BLOCK.get().defaultBlockState()
                .setValue(FACING,   state.getValue(FACING))
                .setValue(VERTICAL, state.getValue(VERTICAL));
        level.setBlock(pos, baseState, 3);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state,
                                        net.minecraft.world.level.BlockGetter level,
                                        BlockPos pos,
                                        CollisionContext context) {
        return Shapes.empty();
    }
}

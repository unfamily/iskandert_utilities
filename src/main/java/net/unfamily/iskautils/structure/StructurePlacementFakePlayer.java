package net.unfamily.iskautils.structure;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Isolated fake player for structure placement ({@code place_like_player}).
 * Prevents {@link BlockItem#place(BlockPlaceContext)} from consuming items from a real player's hand.
 */
public final class StructurePlacementFakePlayer {

    private StructurePlacementFakePlayer() {
    }

    public static FakePlayer get(ServerLevel level, UUID ownerId, BlockPos anchor) {
        GameProfile profile = new GameProfile(
                UUID.nameUUIDFromBytes(("iska_structure_placer:" + ownerId).getBytes(StandardCharsets.UTF_8)),
                "[StructurePlacer]"
        );
        FakePlayer fakePlayer = FakePlayerFactory.get(level, profile);
        refreshPosition(fakePlayer, anchor);
        fakePlayer.getAbilities().instabuild = true;
        return fakePlayer;
    }

    public static void refreshPosition(FakePlayer fakePlayer, BlockPos anchor) {
        fakePlayer.setPos(anchor.getX() + 0.5, anchor.getY(), anchor.getZ() + 0.5);
    }

    /**
     * Places a block using player-like {@link BlockItem} logic without consuming from a real player inventory.
     * Caller is responsible for material consumption (machine inventory or allocated player slot).
     */
    public static void placeBlockAsPlayer(ServerLevel level, BlockPos pos, BlockState state, ItemStack stackForHand, FakePlayer fakePlayer) {
        refreshPosition(fakePlayer, pos);

        Item item = stackForHand.isEmpty() ? Items.AIR : stackForHand.getItem();
        if (item instanceof BlockItem blockItem && item != Items.AIR) {
            fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, stackForHand.copy());
            UseOnContext context = new UseOnContext(
                    fakePlayer,
                    InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)
            );
            try {
                InteractionResult result = blockItem.place(new BlockPlaceContext(context));
                // Do not fall back when placement succeeded but the block is gone (e.g. Wither spawn
                // clears skull/soul sand immediately after the last piece is placed).
                if (result.consumesAction()) {
                    return;
                }
            } catch (Exception ignored) {
                // Fall through to direct placement
            }
        }

        level.setBlock(pos, state, 3);
    }
}

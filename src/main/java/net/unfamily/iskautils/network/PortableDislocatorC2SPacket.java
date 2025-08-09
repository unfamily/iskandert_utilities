package net.unfamily.iskautils.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.network.NetworkEvent;
import net.unfamily.iskautils.IskaUtils;

import java.util.function.Supplier;

public class PortableDislocatorC2SPacket {
    private final int targetX;
    private final int targetY;
    private final int targetZ;
    
    public PortableDislocatorC2SPacket(int targetX, int targetY, int targetZ) {
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
    }
    
    public PortableDislocatorC2SPacket(FriendlyByteBuf buf) {
        this.targetX = buf.readInt();
        this.targetY = buf.readInt();
        this.targetZ = buf.readInt();
    }
    
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(targetX);
        buf.writeInt(targetY);
        buf.writeInt(targetZ);
    }
    
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            
            // Verify player has portable dislocator in inventory
            boolean hasPortableDislocator = false;
            for (ItemStack stack : player.getInventory().items) {
                if (stack.getItem() == IskaUtils.PORTABLE_DISLOCATOR.get()) {
                    hasPortableDislocator = true;
                    break;
                }
            }
            
            if (!hasPortableDislocator) {
                return; // Player doesn't have portable dislocator
            }
            
            // Calculate random offset (200-300 blocks away as mentioned in tooltips)
            double angle = Math.random() * 2 * Math.PI;
            double distance = 200 + Math.random() * 100; // 200-300 blocks
            
            double offsetX = Math.cos(angle) * distance;
            double offsetZ = Math.sin(angle) * distance;
            
            double finalX = targetX + offsetX;
            double finalZ = targetZ + offsetZ;
            
            // Find safe Y position with multiple attempts
            Level level = player.level();
            // Usa Math.floor invece di (int) per gestire correttamente i numeri negativi
            BlockPos targetPos = new BlockPos((int)Math.floor(finalX), targetY, (int)Math.floor(finalZ));
            
            double safeY = findSafeYPosition(level, targetPos, player);
            
            // Teleport player alle stesse coordinate del blocco + offset per centrarlo
            double playerX = targetPos.getX() + 0.5;
            double playerZ = targetPos.getZ() + 0.5;
            player.teleportTo(playerX, safeY, playerZ);
            
            // Send success message to action bar
            player.displayClientMessage(Component.translatable("item.iska_utils.portable_dislocator.message.teleporting"), true);
        });
        return true;
    }
    
    private static double findSafeYPosition(Level level, BlockPos basePos, ServerPlayer player) {
        // 5 tentativi normali con condizioni complete
        for (int attempt = 1; attempt <= 5; attempt++) {
            Double safeY = findSafeYNormalAttempt(level, basePos, player, attempt);
            if (safeY != null) {
                return safeY + 0.3; // Incrementa Y di 0.3
            }
        }
        
        // 5 tentativi void (solo angel block e 2 spazi aria)
        for (int attempt = 1; attempt <= 5; attempt++) {
            Double safeY = findSafeYVoidAttempt(level, basePos, player, attempt);
            if (safeY != null) {
                return safeY + 0.3; // Incrementa Y di 0.3
            }
        }
        
        // Fallback: Y 100.3 costante (void dimension)
        return 100.3;
    }
    
    private static Double findSafeYNormalAttempt(Level level, BlockPos basePos, ServerPlayer player, int attempt) {
        // Y minima 40, cerca verso l'alto fino a 200
        for (int y = 40; y <= 200; y++) {
            BlockPos checkPos = new BlockPos(basePos.getX(), y, basePos.getZ());
            Double result = checkSafePosition(level, checkPos, player, false);
            if (result != null) {
                return result;
            }
        }
        
        // Se fallisce, ritorna a 40 andando verso il basso minimo -64
        for (int y = 40; y >= -64; y--) {
            BlockPos checkPos = new BlockPos(basePos.getX(), y, basePos.getZ());
            Double result = checkSafePosition(level, checkPos, player, false);
            if (result != null) {
                return result;
            }
        }
        
        return null;
    }
    
    private static Double findSafeYVoidAttempt(Level level, BlockPos basePos, ServerPlayer player, int attempt) {
        // Spawna a Y 100 costante per void
        BlockPos checkPos = new BlockPos(basePos.getX(), 100, basePos.getZ());
        return checkSafePosition(level, checkPos, player, true);
    }
    
    private static Double checkSafePosition(Level level, BlockPos pos, ServerPlayer player, boolean isVoidAttempt) {
        // Controlla 2 spazi d'aria (pos e pos+1)
        BlockState block1 = level.getBlockState(pos);
        BlockState block2 = level.getBlockState(pos.above());
        
        if (!isAirOrPlant(block1) || !isAirOrPlant(block2)) {
            return null; // Condizione 1 non rispettata
        }
        
        if (isVoidAttempt) {
            // Per void: piazza angel block no drop sotto e basta
            level.setBlock(pos.below(), getAngelBlockNoDrop(), 3);
            return (double) pos.getY();
        }
        
        // Controlli normali
        BlockPos groundPos = pos.below();
        BlockState groundBlock = level.getBlockState(groundPos);
        
        // Condizione 2: blocco sotto deve essere solido o liquido
        if (!groundBlock.canOcclude() && !(groundBlock.getBlock() instanceof LiquidBlock)) {
            return null;
        }
        
        // Condizione 3: se è acqua
        if (groundBlock.getFluidState().getType() == Fluids.WATER) {
            // Piazza raft bridge che non droppa sopra l'acqua
            level.setBlock(groundPos.above(), getNoDropRaftBridge(), 3);
            return (double) pos.getY();
        }
        
        // Condizione 4: altri liquidi
        if (groundBlock.getBlock() instanceof LiquidBlock) {
            // Controlla un blocco più in alto per angel block
            BlockPos angelPos = pos.above();
            BlockState angelCheck = level.getBlockState(angelPos.above());
            
            if (!isAirOrPlant(angelCheck)) {
                return null; // Non abbastanza spazio sopra
            }
            
            // Piazza angel block no drop e posiziona giocatore sopra
            level.setBlock(angelPos, getAngelBlockNoDrop(), 3);
            return (double) angelPos.getY();
        }
        
        // Superficie solida normale
        return (double) pos.getY();
    }
    
    private static boolean isAirOrPlant(BlockState state) {
        if (state.isAir()) return true;
        
        // Controlla tag forge:plants se disponibile
        try {
            TagKey<Block> plantsTag = TagKey.create(ForgeRegistries.BLOCKS.getRegistryKey(), 
                    ResourceLocation.fromNamespaceAndPath("forge", "plants"));
            if (state.is(plantsTag)) return true;
        } catch (Exception e) {
            // Fallback ai tag vanilla se forge:plants non è disponibile
        }
        
        // Fallback ai tag vanilla
        return state.is(BlockTags.FLOWERS) || state.is(BlockTags.CROPS) || 
               state.is(BlockTags.SAPLINGS) || state.is(BlockTags.LEAVES) ||
               state.is(BlockTags.SMALL_FLOWERS) || state.is(BlockTags.TALL_FLOWERS);
    }
    
    private static BlockState getAngelBlockNoDrop() {
        var block = ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("iska_utils", "angel_block_no_drop"));
        return block != null ? block.defaultBlockState() : Blocks.BEDROCK.defaultBlockState();
    }
    
    private static BlockState getNoDropRaftBridge() {
        var block = ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("iska_utils", "raft_no_drop"));
        return block != null ? block.defaultBlockState() : Blocks.OAK_PLANKS.defaultBlockState();
    }
}

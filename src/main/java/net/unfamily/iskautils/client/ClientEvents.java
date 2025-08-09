package net.unfamily.iskautils.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.network.PortableDislocatorC2SPacket;
import net.unfamily.iskautils.network.NetworkHandler;

public class ClientEvents {
    
    @Mod.EventBusSubscriber(modid = IskaUtils.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void onKeyRegister(RegisterKeyMappingsEvent event) {
            event.register(KeyBindings.PORTABLE_DISLOCATOR_KEY);
        }
    }
    
    @Mod.EventBusSubscriber(modid = IskaUtils.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ForgeBusEvents {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                handleKeyInput();
            }
        }
        
        private static void handleKeyInput() {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            
            if (KeyBindings.PORTABLE_DISLOCATOR_KEY.consumeClick()) {
                handlePortableDislocatorKey(mc.player);
            }
        }
        
        private static void handlePortableDislocatorKey(Player player) {
            // Check if player has portable dislocator in inventory
            boolean hasPortableDislocator = player.getInventory().contains(new ItemStack(IskaUtils.PORTABLE_DISLOCATOR.get()));
            
            if (!hasPortableDislocator) {
                return; // No message, just silently ignore
            }
            
            // Check held item for valid compass
            ItemStack heldItem = player.getMainHandItem();
            if (heldItem.isEmpty()) {
                heldItem = player.getOffhandItem();
            }
            
            if (heldItem.isEmpty()) {
                return; // No compass in hand
            }
            
            // Check for valid compass items
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(heldItem.getItem());
            if (itemId == null) return;
            
            String itemIdString = itemId.toString();
            if (!itemIdString.equals("naturescompass:naturescompass") && !itemIdString.equals("explorerscompass:explorerscompass")) {
                return; // Not a valid compass
            }
            
            // Extract coordinates from NBT
            CompoundTag nbt = heldItem.getTag();
            if (nbt == null || !nbt.contains("FoundX") || !nbt.contains("FoundZ")) {
                return; // No valid coordinates found
            }
            
            int foundX = nbt.getInt("FoundX");
            int foundZ = nbt.getInt("FoundZ");
            int foundY = 100; // Default Y as specified
            
            // Send teleportation message to action bar
            player.displayClientMessage(Component.translatable("item.iska_utils.portable_dislocator.message.teleporting"), true);
            
            // Send packet to server for teleportation
            NetworkHandler.sendToServer(new PortableDislocatorC2SPacket(foundX, foundY, foundZ));
        }
    }
}
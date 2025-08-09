package net.unfamily.iskautils.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.unfamily.iskautils.IskaUtils;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );
    
    public static void register() {
        int id = 0;
        
        INSTANCE.registerMessage(id++, PortableDislocatorC2SPacket.class,
                PortableDislocatorC2SPacket::toBytes,
                PortableDislocatorC2SPacket::new,
                PortableDislocatorC2SPacket::handle);
    }
    
    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }
}

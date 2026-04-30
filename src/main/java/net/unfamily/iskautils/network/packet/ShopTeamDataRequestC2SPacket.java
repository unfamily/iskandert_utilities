package net.unfamily.iskautils.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskalib.team.ShopTeamManager;
import net.unfamily.iskautils.shop.ShopCurrency;
import net.unfamily.iskautils.shop.ShopLoader;
import net.unfamily.iskautils.network.ModMessages;

import java.util.Map;

/**
 * Packet for requesting team data from client to server
 */
public record ShopTeamDataRequestC2SPacket() implements CustomPacketPayload {
    
    public static final Type<ShopTeamDataRequestC2SPacket> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "shop_team_data_request")
    );
    
    public static final StreamCodec<FriendlyByteBuf, ShopTeamDataRequestC2SPacket> STREAM_CODEC = 
        StreamCodec.unit(new ShopTeamDataRequestC2SPacket());
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
    /**
     * Handles the packet on the server side
     */
    public void handle(ServerPlayer player) {
        if (player == null) return;
        
        ShopTeamManager teamManager = ShopTeamManager.getInstance((net.minecraft.server.level.ServerLevel) player.level());
        String teamKey = teamManager.getPlayerTeamKey(player);
        
        // Prepara i dati del team
        Map<String, Double> teamBalances = null;
        if (teamKey != null) {
            teamBalances = new java.util.HashMap<>();
            Map<String, ShopCurrency> currencies = ShopLoader.getCurrencies();
            
            // Ottieni il balance per ogni valuta
            for (String currencyId : currencies.keySet()) {
                double balance = teamManager.getTeamValuteBalance(teamKey, currencyId);
                teamBalances.put(currencyId, balance);
            }
        }
        
        // Invia i dati al client
        String displayName = teamKey != null ? teamManager.getTeamDisplayName(teamKey) : null;
        ModMessages.sendShopTeamDataToClient(player, displayName != null ? displayName : teamKey, teamBalances);
    }
    
    public static void handle(ShopTeamDataRequestC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                packet.handle(serverPlayer);
            }
        });
    }
} 
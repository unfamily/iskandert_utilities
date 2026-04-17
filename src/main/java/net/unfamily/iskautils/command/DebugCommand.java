package net.unfamily.iskautils.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.data.load.IskaUtilsDataReload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Debug command for dumping item data (ID + NBT)
 */
@EventBusSubscriber(modid = IskaUtils.MOD_ID)
public class DebugCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(DebugCommand.class);
    
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        
        dispatcher.register(Commands.literal("iska_utils_debug")
                .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.byId(0))))
                .then(Commands.literal("reload")
                        .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.byId(2))))
                        .executes(DebugCommand::executeReload))
                .then(Commands.argument("action", StringArgumentType.word())
                        .suggests((context, builder) ->
                            SharedSuggestionProvider.suggest(new String[]{"hand"}, builder))
                        .executes(context -> executeDebug(context))
                )
        );
    }
    
    private static int executeReload(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal("Reloading IskaUtils load JSON (data/*/load/)")
                .withStyle(ChatFormatting.GRAY), false);

        try {
            IskaUtilsDataReload.reloadAllFromServer();
            ShopCommand.notifyClientGUIReload();
            var server = source.getServer();
            if (server != null && !server.isSingleplayer()) {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    net.unfamily.iskautils.network.ModMessages.sendStructureSyncPacket(player);
                }
                source.sendSuccess(() -> Component.literal("Reload complete (structures synced to clients)")
                        .withStyle(ChatFormatting.GREEN), false);
            } else {
                source.sendSuccess(() -> Component.literal("Reload complete").withStyle(ChatFormatting.GREEN), false);
            }
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error during IskaUtils reload: {}", e.getMessage());
            source.sendFailure(Component.literal("Reload failed: " + e.getMessage()).withStyle(ChatFormatting.RED));
            return 0;
        }
    }

    private static int executeDebug(CommandContext<CommandSourceStack> context) {
        String action = StringArgumentType.getString(context, "action");
        
        if ("hand".equals(action)) {
            return dumpItem(context);
        } else {
            context.getSource().sendFailure(Component.translatable("command.iska_utils.debug.unknown_action", action));
            return 0;
        }
    }
    
    private static int dumpItem(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("command.iska_utils.debug.only_player"));
            return 0;
        }
        
        ItemStack itemInHand = player.getMainHandItem();
        
        if (itemInHand.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("command.iska_utils.debug.no_item"), false);
            return 0;
        }
        
        // Complete item dump
        String itemId = itemInHand.getItem().toString();
        
        // Serialize ItemStack data for debugging output
        CompoundTag nbtTag = new CompoundTag();
        nbtTag.putString("components", itemInHand.getComponentsPatch().toString());
        
        // Check if item is a block
        boolean isBlock = itemInHand.getItem() instanceof BlockItem;
        
        // Send to chat
        source.sendSuccess(() -> Component.translatable("command.iska_utils.debug.header"), false);
        
        // Item ID (copyable, green, with hover text)
        Component itemIdLabel = Component.literal("Item ID: ").withStyle(ChatFormatting.WHITE);
        Component itemIdComponent = Component.literal(itemId)
                .withStyle(Style.EMPTY
                        .withColor(ChatFormatting.GREEN)
                        .withClickEvent(new ClickEvent.CopyToClipboard(itemId))
                        .withHoverEvent(new HoverEvent.ShowText(
                                Component.translatable("command.iska_utils.debug.click_to_copy"))));
        Component itemIdFull = itemIdLabel.copy().append(itemIdComponent);
        source.sendSuccess(() -> itemIdFull, false);
        
        // Split tags into blocks and items
        CompoundTag blocksTag = new CompoundTag();
        CompoundTag itemsTag = new CompoundTag();
        
        for (String key : nbtTag.keySet()) {
            Tag value = nbtTag.get(key);
            if (value != null) {
                // Common block-related keys
                if (key.equals("BlockEntityTag") || key.equals("BlockStateTag") || 
                    key.equals("BlockEntity") || key.startsWith("block_") ||
                    key.equals("palette") || key.equals("blocks") || key.equals("entities") ||
                    key.equals("size") || key.equals("dataVersion")) {
                    blocksTag.put(key, value);
                } else {
                    itemsTag.put(key, value);
                }
            }
        }
        
        // Show blocks section only if item is a block
        if (isBlock && !blocksTag.isEmpty()) {
            Component blocksLabel = Component.literal("Blocks:").withStyle(ChatFormatting.WHITE);
            source.sendSuccess(() -> blocksLabel, false);
            String blocksString = blocksTag.toString();
            sendCopyableNbt(source, blocksString, ChatFormatting.YELLOW);
        }
        
        // Show items section (general item tags)
        if (!itemsTag.isEmpty()) {
            Component itemsLabel = Component.literal("Item:").withStyle(ChatFormatting.WHITE);
            source.sendSuccess(() -> itemsLabel, false);
            String itemsString = itemsTag.toString();
            sendCopyableNbt(source, itemsString, ChatFormatting.YELLOW);
        }
        
        // Show item tags (like #c:ingots)
        if (player.level() != null) {
            Item item = itemInHand.getItem();
            var itemTags = item.builtInRegistryHolder().tags()
                    .map(TagKey::location)
                    .map(Identifier::toString)
                    .sorted()
                    .toList();
            
            if (!itemTags.isEmpty()) {
                Component tagsLabel = Component.literal("Tags:").withStyle(ChatFormatting.WHITE);
                source.sendSuccess(() -> tagsLabel, false);
                String tagsString = String.join(", ", itemTags.stream().map(tag -> "#" + tag).toList());
                sendCopyableNbt(source, tagsString, ChatFormatting.YELLOW);
            }
        }
        
        // If both are empty, show full tag
        if (blocksTag.isEmpty() && itemsTag.isEmpty() && !nbtTag.isEmpty()) {
            String nbtString = nbtTag.toString();
            sendCopyableNbt(source, nbtString, ChatFormatting.YELLOW);
        }
        
        // Also log to server log for safety
        LOGGER.info("=== DUMP ITEM ===");
        LOGGER.info("Item ID: {}", itemId);
        LOGGER.info("Complete CompoundTag:\n{}", nbtTag.toString());
        
        return 1;
    }
    
    /**
     * Sends NBT data as copyable chat messages
     */
    private static void sendCopyableNbt(CommandSourceStack source, String nbtString, ChatFormatting color) {
        int maxLength = 30000; // Maximum length per chat message
        Component copyFeedback = Component.translatable("command.iska_utils.debug.copied");
        
        if (nbtString.length() > maxLength) {
            int chunks = (nbtString.length() + maxLength - 1) / maxLength;
            for (int i = 0; i < chunks; i++) {
                int start = i * maxLength;
                int end = Math.min(start + maxLength, nbtString.length());
                String chunk = nbtString.substring(start, end);
                final int chunkNum = i + 1;
                final int totalChunks = chunks;
                
                Component chunkLabel = Component.literal(String.format("[Part %s/%s] ", chunkNum, totalChunks))
                        .withStyle(ChatFormatting.GRAY);
                Component chunkComponent = Component.literal(chunk)
                        .withStyle(Style.EMPTY
                                .withColor(color)
                                .withClickEvent(new ClickEvent.CopyToClipboard(chunk))
                                .withHoverEvent(new HoverEvent.ShowText(copyFeedback)));
                Component chunkFull = chunkLabel.copy().append(chunkComponent);
                source.sendSuccess(() -> chunkFull, false);
            }
        } else {
            Component nbtComponent = Component.literal(nbtString)
                    .withStyle(Style.EMPTY
                            .withColor(color)
                            .withClickEvent(new ClickEvent.CopyToClipboard(nbtString))
                            .withHoverEvent(new HoverEvent.ShowText(copyFeedback)));
            source.sendSuccess(() -> nbtComponent, false);
        }
    }
}

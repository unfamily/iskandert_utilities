package net.unfamily.iskautils.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.unfamily.iskautils.IskaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Debug command for dumping item data (ID + NBT)
 */
@EventBusSubscriber(modid = IskaUtils.MOD_ID)
public class DumpItemCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(DumpItemCommand.class);
    
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        
        dispatcher.register(Commands.literal("iska_utils_debug")
                .requires(source -> source.hasPermission(2)) // OP only
                .then(Commands.argument("action", StringArgumentType.word())
                        .executes(context -> executeDebug(context))
                )
        );
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
        
        // Serialize complete ItemStack to get full CompoundTag
        CompoundTag nbtTag = new CompoundTag();
        try {
            if (player.level() != null) {
                var tag = itemInHand.save(player.level().registryAccess());
                // itemInHand.save() returns a Tag, which for ItemStack should be a CompoundTag
                if (tag instanceof CompoundTag compoundTag) {
                    nbtTag = compoundTag;
                } else {
                    // If it's not a CompoundTag, create one and put the Tag inside
                    nbtTag = new CompoundTag();
                    nbtTag.put("data", tag);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error serializing CompoundTag", e);
            source.sendFailure(Component.translatable("command.iska_utils.debug.serialization_error", e.getMessage()));
            return 0;
        }
        
        final String nbtString = nbtTag.toString(); // Final for lambda usage
        
        // Send to chat
        source.sendSuccess(() -> Component.translatable("command.iska_utils.debug.header"), false);
        source.sendSuccess(() -> Component.translatable("command.iska_utils.debug.item_id", itemId), false);
        source.sendSuccess(() -> Component.translatable("command.iska_utils.debug.compoundtag_label"), false);
        
        // Split CompoundTag into multiple messages if too long
        int maxLength = 30000; // Maximum length per chat message
        if (nbtString.length() > maxLength) {
            int chunks = (nbtString.length() + maxLength - 1) / maxLength;
            for (int i = 0; i < chunks; i++) {
                int start = i * maxLength;
                int end = Math.min(start + maxLength, nbtString.length());
                String chunk = nbtString.substring(start, end);
                final int chunkNum = i + 1;
                final int totalChunks = chunks;
                source.sendSuccess(() -> Component.translatable("command.iska_utils.debug.chunk", chunkNum, totalChunks, chunk), false);
            }
        } else {
            final String finalNbtString = nbtString;
            source.sendSuccess(() -> Component.translatable("command.iska_utils.debug.compoundtag_content", finalNbtString), false);
        }
        
        // Also log to server log for safety
        LOGGER.info("=== DUMP ITEM ===");
        LOGGER.info("Item ID: {}", itemId);
        LOGGER.info("Complete CompoundTag:\n{}", nbtString);
        
        return 1;
    }
}

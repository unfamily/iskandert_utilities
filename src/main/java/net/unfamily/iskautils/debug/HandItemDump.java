package net.unfamily.iskautils.debug;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.unfamily.iskautils.shop.ItemConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server-side dump of held items (aligned with {@code iskandert_utilities_26} / Iskandert Library):
 * item argument line, Stack JSON, then tags / component detail.
 */
public final class HandItemDump {
    private static final Logger LOGGER = LoggerFactory.getLogger(HandItemDump.class);
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final int MAX_CHAT_LENGTH = 30000;

    private HandItemDump() {}

    private static String toChatSingleLine(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        return s.replace("\r\n", "").replace("\n", "").replace("\r", "");
    }

    /**
     * Dumps main hand and off hand for a player.
     */
    public static int dumpHands(ServerPlayer player, CommandSourceStack source) {
        dumpHandSlot(player, source, EquipmentSlot.MAINHAND);
        return 1;
    }

    private static void dumpHandSlot(ServerPlayer player, CommandSourceStack source, EquipmentSlot slot) {
        ItemStack stack = player.getItemBySlot(slot);
        String slotLabel = slot == EquipmentSlot.MAINHAND ? "main" : "off";
        source.sendSuccess(
                () -> Component.literal("=== Hand (" + slotLabel + ") ===").withStyle(ChatFormatting.GOLD),
                false);

        if (stack.isEmpty()) {
            source.sendSuccess(() -> Component.literal("(empty)").withStyle(ChatFormatting.GRAY), false);
            return;
        }

        appendItemArgumentLine(source, player, stack);
        appendItemArgumentJsonLine(source, player, stack);
        appendStackJsonLine(source, player, stack);
        appendDetailedDump(source, player, stack);
    }

    private static void appendItemArgumentLine(CommandSourceStack source, ServerPlayer player, ItemStack stack) {
        String itemArg = ItemConverter.formatAsKubeJsItemString(stack, player.registryAccess());
        source.sendSuccess(() -> copyableLine("Item", itemArg, ChatFormatting.AQUA), false);
    }

    private static void appendItemArgumentJsonLine(CommandSourceStack source, ServerPlayer player, ItemStack stack) {
        String itemArgJson = ItemConverter.formatAsKubeJsItemStringJson(stack, player.registryAccess());
        source.sendSuccess(() -> copyableLine("Item JSON", itemArgJson, ChatFormatting.AQUA), false);
    }

    private static void appendStackJsonLine(CommandSourceStack source, ServerPlayer player, ItemStack stack) {
        var ops = player.registryAccess().createSerializationContext(JsonOps.INSTANCE);
        JsonElement encoded = ItemStack.CODEC.encodeStart(ops, stack).getOrThrow();
        String json = toChatSingleLine(GSON.toJson(encoded));
        source.sendSuccess(() -> copyableLine("Stack JSON", json, ChatFormatting.YELLOW), false);
    }

    private static void appendDetailedDump(CommandSourceStack source, ServerPlayer player, ItemStack stack) {
        String itemIdStr = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();

        CompoundTag nbtTag = new CompoundTag();
        nbtTag.putString("components", stack.getComponentsPatch().toString());

        MutableComponent itemIdLabel = Component.literal("Item ID: ").withStyle(ChatFormatting.WHITE);
        MutableComponent itemIdComponent = Component.literal(itemIdStr)
                .withStyle(Style.EMPTY
                        .withColor(ChatFormatting.GREEN)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, itemIdStr))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.translatable("command.iska_utils.debug.click_to_copy"))));
        source.sendSuccess(() -> itemIdLabel.append(itemIdComponent), false);

        boolean isBlock = stack.getItem() instanceof BlockItem;
        CompoundTag blocksTag = new CompoundTag();
        CompoundTag itemsTag = new CompoundTag();
        for (String key : nbtTag.getAllKeys()) {
            Tag value = nbtTag.get(key);
            if (value == null) {
                continue;
            }
            if (key.equals("BlockEntityTag") || key.equals("BlockStateTag")
                    || key.equals("BlockEntity") || key.startsWith("block_")
                    || key.equals("palette") || key.equals("blocks") || key.equals("entities")
                    || key.equals("size") || key.equals("dataVersion")) {
                blocksTag.put(key, value);
            } else {
                itemsTag.put(key, value);
            }
        }

        if (isBlock && !blocksTag.isEmpty()) {
            source.sendSuccess(() -> Component.literal("Blocks:").withStyle(ChatFormatting.WHITE), false);
            sendCopyableNbt(source, blocksTag.toString(), ChatFormatting.YELLOW);
        }
        if (!itemsTag.isEmpty()) {
            source.sendSuccess(() -> Component.literal("NBT:").withStyle(ChatFormatting.WHITE), false);
            sendCopyableNbt(source, itemsTag.toString(), ChatFormatting.YELLOW);
        }

        Item item = stack.getItem();
        var itemTags = item.builtInRegistryHolder().tags()
                .map(TagKey::location)
                .map(ResourceLocation::toString)
                .sorted()
                .toList();
        if (!itemTags.isEmpty()) {
            source.sendSuccess(() -> Component.literal("Tags:").withStyle(ChatFormatting.WHITE), false);
            for (String tag : itemTags) {
                String tagWithHash = "#" + tag;
                source.sendSuccess(
                        () -> Component.literal("  ")
                                .withStyle(ChatFormatting.DARK_GRAY)
                                .append(clickableCopyOnly(tagWithHash, ChatFormatting.YELLOW)),
                        false);
            }
        }

        if (blocksTag.isEmpty() && itemsTag.isEmpty() && !nbtTag.isEmpty()) {
            sendCopyableNbt(source, nbtTag.toString(), ChatFormatting.YELLOW);
        }

        LOGGER.info("[HandItemDump] Item ID: {} | {}", itemIdStr, toChatSingleLine(nbtTag.toString()));
    }

    private static MutableComponent copyableLine(String label, String text, ChatFormatting color) {
        MutableComponent prefix = Component.literal(label + ": ").withStyle(ChatFormatting.WHITE);
        MutableComponent body = clickableCopyOnly(text, color);
        return prefix.append(body);
    }

    private static MutableComponent clickableCopyOnly(String text, ChatFormatting color) {
        return Component.literal(text)
                .withStyle(Style.EMPTY
                        .withColor(color)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, text))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.translatable("command.iska_utils.debug.click_to_copy"))));
    }

    private static void sendCopyableNbt(CommandSourceStack source, String nbtString, ChatFormatting color) {
        nbtString = toChatSingleLine(nbtString);
        Component copyFeedback = Component.translatable("command.iska_utils.debug.click_to_copy");
        if (nbtString.length() > MAX_CHAT_LENGTH) {
            int chunks = (nbtString.length() + MAX_CHAT_LENGTH - 1) / MAX_CHAT_LENGTH;
            for (int i = 0; i < chunks; i++) {
                int start = i * MAX_CHAT_LENGTH;
                int end = Math.min(start + MAX_CHAT_LENGTH, nbtString.length());
                String chunk = nbtString.substring(start, end);
                final int chunkNum = i + 1;
                final int totalChunks = chunks;
                MutableComponent chunkLabel = Component.literal(String.format("[Part %s/%s] ", chunkNum, totalChunks))
                        .withStyle(ChatFormatting.GRAY);
                MutableComponent chunkComponent = Component.literal(chunk)
                        .withStyle(Style.EMPTY
                                .withColor(color)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, chunk))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, copyFeedback)));
                source.sendSuccess(() -> chunkLabel.append(chunkComponent), false);
            }
        } else {
            MutableComponent nbtComponent = Component.literal(nbtString)
                    .withStyle(Style.EMPTY
                            .withColor(color)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, nbtString))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, copyFeedback)));
            source.sendSuccess(() -> nbtComponent, false);
        }
    }
}

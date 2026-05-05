package net.unfamily.iskautils.debug;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.DataResult;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Server-side dump of held items:
 * - KubeJS-like item string (no quotes, no count)
 * - JSON compatible KubeJS-like item string (wrapped in quotes, escaped)
 * - Stack JSON
 * - tags / component detail
 */
public final class HandItemDump {
    private static final Logger LOGGER = LoggerFactory.getLogger(HandItemDump.class);
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private HandItemDump() {}

    /** Chat / copy payload: no line breaks (NBT SNBT and JSON pretty dumps otherwise span multiple lines). */
    private static String toChatSingleLine(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        return s.replace("\r\n", "").replace("\n", "").replace("\r", "");
    }

    /** Dumps main hand only. Always returns {@code 1} when the player exists. */
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
        appendDetailedDump(source, stack);
    }

    private static void appendItemArgumentLine(CommandSourceStack source, ServerPlayer player, ItemStack stack) {
        String itemArg = formatAsKubeJsItemString(stack, player.registryAccess());
        source.sendSuccess(() -> copyableLine("Item", itemArg, ChatFormatting.AQUA), false);
    }

    private static void appendItemArgumentJsonLine(CommandSourceStack source, ServerPlayer player, ItemStack stack) {
        String itemArg = formatAsKubeJsItemString(stack, player.registryAccess());
        String json = toJsonCompatibleString(itemArg);
        source.sendSuccess(() -> copyableLine("Item JSON", json, ChatFormatting.AQUA), false);
    }

    private static void appendStackJsonLine(CommandSourceStack source, ServerPlayer player, ItemStack stack) {
        var ops = player.registryAccess().createSerializationContext(JsonOps.INSTANCE);
        JsonElement encoded = ItemStack.CODEC.encodeStart(ops, stack).getOrThrow();
        String json = toChatSingleLine(GSON.toJson(encoded));
        source.sendSuccess(() -> copyableLine("Stack JSON", json, ChatFormatting.YELLOW), false);
    }

    private static void appendDetailedDump(CommandSourceStack source, ItemStack stack) {
        String itemIdStr = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();

        MutableComponent itemIdLabel = Component.literal("Item ID: ").withStyle(ChatFormatting.WHITE);
        MutableComponent itemIdComponent = Component.literal(itemIdStr)
                .withStyle(Style.EMPTY
                        .withColor(ChatFormatting.GREEN)
                        .withClickEvent(new ClickEvent.CopyToClipboard(itemIdStr))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to copy"))));
        source.sendSuccess(() -> itemIdLabel.append(itemIdComponent), false);

        Item item = stack.getItem();
        boolean isBlock = item instanceof BlockItem;

        // Keep a minimal detailed dump aligned with previous behavior: show components patch SNBT
        if (!stack.getComponentsPatch().isEmpty()) {
            String comp = toChatSingleLine(stack.getComponentsPatch().toString());
            source.sendSuccess(() -> Component.literal((isBlock ? "Blocks/NBT: " : "NBT: "))
                    .withStyle(ChatFormatting.WHITE)
                    .append(clickableCopyOnly(comp, ChatFormatting.YELLOW)), false);
        }

        var itemTags = item.builtInRegistryHolder().tags()
                .map(TagKey::location)
                .map(Identifier::toString)
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

        LOGGER.info("[HandItemDump] Item ID: {} | {}", itemIdStr, toChatSingleLine(stack.getComponentsPatch().toString()));
    }

    private static MutableComponent copyableLine(String label, String text, ChatFormatting color) {
        MutableComponent prefix = Component.literal(label + ": ").withStyle(ChatFormatting.WHITE);
        MutableComponent body = clickableCopyOnly(text, color);
        return prefix.append(body);
    }

    /** One click copies exactly {@code text}. */
    private static MutableComponent clickableCopyOnly(String text, ChatFormatting color) {
        return Component.literal(text)
                .withStyle(Style.EMPTY
                        .withColor(color)
                        .withClickEvent(new ClickEvent.CopyToClipboard(text))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to copy"))));
    }

    private static String toJsonCompatibleString(String raw) {
        if (raw == null) {
            raw = "";
        }
        raw = raw.replace("\\", "\\\\").replace("\"", "\\\"");
        return "\"" + raw + "\"";
    }

    private static String formatAsKubeJsItemString(ItemStack stack, HolderLookup.Provider registries) {
        if (stack.isEmpty()) {
            return "";
        }
        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        DataComponentPatch patch = stack.getComponentsPatch();
        if (patch.isEmpty()) {
            return itemId.toString();
        }

        RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        List<Map.Entry<DataComponentType<?>, Optional<?>>> entries = new ArrayList<>(patch.entrySet());
        entries.sort(Comparator.comparing(e -> {
            Identifier id = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(e.getKey());
            return id != null ? id.toString() : "";
        }));

        StringBuilder bracket = new StringBuilder();
        boolean first = true;
        for (Map.Entry<DataComponentType<?>, Optional<?>> entry : entries) {
            DataComponentType<?> type = entry.getKey();
            Optional<?> opt = entry.getValue();
            Identifier compId = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type);
            if (compId == null) {
                continue;
            }

            if (!first) {
                bracket.append(',');
            }
            first = false;

            String compKey = "minecraft".equals(compId.getNamespace()) ? compId.getPath() : compId.toString();
            if (opt.isEmpty()) {
                bracket.append('!').append(compKey);
            } else {
                @SuppressWarnings("unchecked")
                DataComponentType<Object> typed = (DataComponentType<Object>) type;
                Object value = opt.get();
                DataResult<Tag> encoded = typed.codecOrThrow().encodeStart(ops, value);
                Tag tag = encoded.getOrThrow();
                bracket.append(compKey).append('=').append(tag);
            }
        }

        return itemId + "[" + bracket + "]";
    }
}


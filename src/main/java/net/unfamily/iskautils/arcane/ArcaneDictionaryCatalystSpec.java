package net.unfamily.iskautils.arcane;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Parsed {@code cat} entry: {@code #c:gems/lapis:12} or {@code minecraft:item:8}. */
public record ArcaneDictionaryCatalystSpec(String raw, String matchSpec, int weightBoost) {

    public static ArcaneDictionaryCatalystSpec parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        int lastColon = raw.lastIndexOf(':');
        if (lastColon > 0) {
            String suffix = raw.substring(lastColon + 1);
            if (!suffix.isEmpty() && suffix.chars().allMatch(Character::isDigit)) {
                return new ArcaneDictionaryCatalystSpec(
                        raw,
                        raw.substring(0, lastColon),
                        Integer.parseInt(suffix));
            }
        }
        return new ArcaneDictionaryCatalystSpec(raw, raw, 0);
    }

    public static List<ArcaneDictionaryCatalystSpec> parseAll(List<String> rawSpecs) {
        if (rawSpecs == null || rawSpecs.isEmpty()) {
            return List.of();
        }
        List<ArcaneDictionaryCatalystSpec> out = new ArrayList<>();
        for (String raw : rawSpecs) {
            ArcaneDictionaryCatalystSpec parsed = parse(raw);
            if (parsed != null) {
                out.add(parsed);
            }
        }
        return List.copyOf(out);
    }

    public boolean matches(ItemStack stack) {
        if (stack.isEmpty() || matchSpec.isBlank()) {
            return false;
        }
        if (matchSpec.startsWith("#")) {
            Identifier tagId = Identifier.tryParse(matchSpec.substring(1));
            if (tagId == null) {
                return false;
            }
            TagKey<Item> tag = TagKey.create(BuiltInRegistries.ITEM.key(), tagId);
            return stack.is(tag);
        }
        Identifier itemId = Identifier.tryParse(matchSpec);
        if (itemId == null) {
            return false;
        }
        return BuiltInRegistries.ITEM.getOptional(itemId).map(stack::is).orElse(false);
    }

    public List<ItemStack> exampleStacks() {
        if (matchSpec.startsWith("#")) {
            Identifier tagId = Identifier.tryParse(matchSpec.substring(1));
            if (tagId == null) {
                return List.of();
            }
            TagKey<Item> tag = TagKey.create(BuiltInRegistries.ITEM.key(), tagId);
            List<ItemStack> stacks = new ArrayList<>();
            for (Item item : BuiltInRegistries.ITEM) {
                if (item.builtInRegistryHolder().is(tag)) {
                    stacks.add(new ItemStack(item));
                }
            }
            return stacks;
        }
        Identifier itemId = Identifier.tryParse(matchSpec);
        if (itemId == null) {
            return List.of();
        }
        return BuiltInRegistries.ITEM.getOptional(itemId)
                .map(item -> List.of(new ItemStack(item)))
                .orElse(List.of());
    }
}

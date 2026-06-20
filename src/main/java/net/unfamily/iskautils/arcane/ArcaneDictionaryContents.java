package net.unfamily.iskautils.arcane;

import net.unfamily.iskautils.util.ArtifactTooltipUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.unfamily.iskautils.Config;

import java.util.ArrayList;
import java.util.List;

public final class ArcaneDictionaryContents {
    public static final String NBT_STORED = "stored_entropy";
    public static final String NBT_TRAITS = "arcane_traits";
    public static final String NBT_TRAIT_ID = "id";
    public static final String NBT_TRAIT_LEVEL = "level";
    public static final String NBT_INITIAL_ROLL_DONE = "initial_roll_done";

    private ArcaneDictionaryContents() {}

    public record TraitSlot(ResourceLocation id, int level) {}

    public static boolean hasTraits(ItemStack stack) {
        return !getTraits(stack).isEmpty();
    }

    public static boolean isTraitless(ItemStack stack) {
        return !hasTraits(stack);
    }

    public static boolean needsInitialTraitRoll(ItemStack stack) {
        return isTraitless(stack) && !isInitialRollAttempted(stack);
    }

    public static boolean isInitialRollAttempted(ItemStack stack) {
        return root(stack).getBoolean(NBT_INITIAL_ROLL_DONE);
    }

    public static void markInitialRollAttempted(ItemStack stack) {
        CompoundTag tag = root(stack);
        tag.putBoolean(NBT_INITIAL_ROLL_DONE, true);
        write(stack, tag);
    }

    public static int getStoredEntropy(ItemStack stack) {
        CompoundTag tag = root(stack);
        return Math.max(0, tag.getInt(NBT_STORED));
    }

    public static void setStoredEntropy(ItemStack stack, int stored) {
        setStoredEntropy(stack, stored, Config.arcaneDictionaryMaxStored);
    }

    public static void setStoredEntropy(ItemStack stack, int stored, int max) {
        int clamped = Math.max(0, Math.min(Math.max(1, max), stored));
        CompoundTag tag = root(stack);
        tag.putInt(NBT_STORED, clamped);
        write(stack, tag);
    }

    public static List<TraitSlot> getTraits(ItemStack stack) {
        CompoundTag tag = root(stack);
        if (!tag.contains(NBT_TRAITS, Tag.TAG_LIST)) {
            return List.of();
        }
        List<TraitSlot> out = new ArrayList<>();
        ListTag list = tag.getList(NBT_TRAITS, Tag.TAG_COMPOUND);
        for (Tag el : list) {
            if (!(el instanceof CompoundTag compound)) {
                continue;
            }
            ResourceLocation id = ResourceLocation.tryParse(compound.getString(NBT_TRAIT_ID));
            if (id == null) {
                continue;
            }
            int level = Math.max(1, compound.getInt(NBT_TRAIT_LEVEL));
            out.add(new TraitSlot(id, level));
        }
        return List.copyOf(out);
    }

    public static void setTraits(ItemStack stack, List<TraitSlot> traits) {
        CompoundTag tag = root(stack);
        ListTag list = new ListTag();
        for (TraitSlot trait : traits) {
            CompoundTag entry = new CompoundTag();
            entry.putString(NBT_TRAIT_ID, trait.id().toString());
            entry.putInt(NBT_TRAIT_LEVEL, Math.max(1, Math.min(Config.arcaneDictionaryMaxLevel, trait.level())));
            list.add(entry);
        }
        tag.put(NBT_TRAITS, list);
        write(stack, tag);
    }

    public static int computeTotalConsume(ItemStack stack) {
        ArcaneDictionaryShiftingState.MimicState mimic = ArcaneDictionaryShiftingState.read(stack);
        long gameTime = mimic != null ? mimic.untilTick() - 1 : 0L;
        return ArcaneDictionaryConsume.computeTotalConsume(stack, gameTime);
    }

    public static void appendTooltip(ItemStack stack, TooltipFlag flag, java.util.function.Consumer<Component> add) {
        if (flag != null && flag.hasShiftDown() && hasTraits(stack)) {
            appendBriefTooltip(stack, add);
            return;
        }
        appendEntropySummary(stack, add);
        for (TraitSlot trait : getTraits(stack)) {
            add.accept(traitLine(trait));
        }
        add.accept(ArtifactTooltipUtil.techLine("tooltip.iska_utils.arcane_dictionary.entropy_refill_hint"));
    }

    public static void appendBriefTooltip(ItemStack stack, java.util.function.Consumer<Component> add) {
        appendEntropySummary(stack, add);
        List<TraitSlot> traits = getTraits(stack);
        for (int i = 0; i < traits.size(); i++) {
            if (i > 0) {
                add.accept(Component.empty());
            }
            appendBriefTraitTooltip(traits.get(i), add);
        }
    }

    private static void appendEntropySummary(ItemStack stack, java.util.function.Consumer<Component> add) {
        int stored = getStoredEntropy(stack);
        add.accept(ArtifactTooltipUtil.techLine(
                "tooltip.iska_utils.arcane_dictionary.entropy",
                stored,
                Config.arcaneDictionaryMaxStored));
        if (hasTraits(stack)) {
            add.accept(ArtifactTooltipUtil.techLine(
                    "tooltip.iska_utils.arcane_dictionary.entropy_consume",
                    computeTotalConsume(stack)));
        }
    }

    private static void appendBriefTraitTooltip(TraitSlot trait, java.util.function.Consumer<Component> add) {
        add.accept(traitLine(trait));
        String brief = Component.translatable(briefTraitKey(trait)).getString().trim();
        if (!brief.isEmpty()) {
            add.accept(Component.literal(brief).withStyle(ArtifactTooltipUtil.loreStyle()));
        }
        ArcaneDictionaryDefinition.Entry poolEntry = ArcaneDictionaryLoader.findEntry(trait.id());
        if (poolEntry != null && poolEntry.catalysts() != null && !poolEntry.catalysts().isEmpty()) {
            List<ArcaneDictionaryDefinition.Entry> visible = ArcaneDictionaryPools.visibleForJei(
                    new ArrayList<>(ArcaneDictionaryPools.uniqueEntries(ArcaneDictionaryLoader.getEntries()).values()));
            int total = ArcaneDictionaryPools.poolTotalWeight(visible);
            double chance = ArcaneDictionaryCatalystBoost.maxBoostedChancePercent(poolEntry, total);
            add.accept(ArtifactTooltipUtil.techLine(
                    "tooltip.iska_utils.arcane_trait.catalyst_chance",
                    formatChancePercent(chance)));
        }
    }

    private static String formatChancePercent(double percent) {
        if (Math.rint(percent) == percent) {
            return String.format(java.util.Locale.ROOT, "%.0f%%", percent);
        }
        return String.format(java.util.Locale.ROOT, "%.1f%%", percent);
    }

    private static String briefTraitKey(TraitSlot trait) {
        return "tooltip.iska_utils.arcane_trait."
                + trait.id().getNamespace()
                + "."
                + trait.id().getPath()
                + ".brief";
    }

    public static Component traitLine(TraitSlot trait) {
        return ArcaneDictionaryTraitStyle.traitNameAndLevel(trait.id(), trait.level());
    }

    private static CompoundTag root(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static void write(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}

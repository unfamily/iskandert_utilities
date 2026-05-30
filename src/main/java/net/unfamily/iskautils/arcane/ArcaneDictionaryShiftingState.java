package net.unfamily.iskautils.arcane;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.unfamily.iskautils.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class ArcaneDictionaryShiftingState {

    private static final String NBT_MIMIC_ID = "shifting_mimic_id";
    private static final String NBT_MIMIC_UNTIL = "shifting_until_tick";

    private static final Set<ResourceLocation> EXCLUDED_MIMIC = Set.of(
            ArcaneDictionaryTraitIds.SHIFTING_POWER,
            ArcaneDictionaryTraitIds.ENTROPY_OVERFLOW,
            ArcaneDictionaryTraitIds.UNLUCKY,
            ArcaneDictionaryTraitIds.MARTYR_SCRIPT,
            ArcaneDictionaryTraitIds.GLASS_SKIN);

    private ArcaneDictionaryShiftingState() {}

    public record MimicState(ResourceLocation traitId, long untilTick) {}

    public static int mimicDurationTicks() {
        return Config.arcaneShiftingPowerMimicDurationSeconds * 20;
    }

    public static MimicState read(ItemStack dictionary) {
        CompoundTag tag = root(dictionary);
        if (!tag.contains(NBT_MIMIC_ID)) {
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(tag.getString(NBT_MIMIC_ID));
        if (id == null) {
            return null;
        }
        return new MimicState(id, tag.getLong(NBT_MIMIC_UNTIL));
    }

    public static boolean isActive(ItemStack dictionary, long gameTime) {
        MimicState state = read(dictionary);
        return state != null && gameTime < state.untilTick();
    }

    public static MimicState tick(ItemStack dictionary, RandomSource random, long gameTime) {
        MimicState current = read(dictionary);
        if (current != null && gameTime < current.untilTick()) {
            return current;
        }
        ResourceLocation picked = pickPositiveTrait(random);
        if (picked == null) {
            clear(dictionary);
            return null;
        }
        MimicState next = new MimicState(picked, gameTime + mimicDurationTicks());
        write(dictionary, next);
        return next;
    }

    public static void clear(ItemStack dictionary) {
        CompoundTag tag = root(dictionary);
        tag.remove(NBT_MIMIC_ID);
        tag.remove(NBT_MIMIC_UNTIL);
        write(dictionary, tag);
    }

    public static int shiftingLevel(ItemStack dictionary) {
        for (ArcaneDictionaryContents.TraitSlot trait : ArcaneDictionaryContents.getTraits(dictionary)) {
            if (ArcaneDictionaryTraitIds.SHIFTING_POWER.equals(trait.id())) {
                return trait.level();
            }
        }
        return 0;
    }

    private static ResourceLocation pickPositiveTrait(RandomSource random) {
        List<ArcaneDictionaryDefinition.Entry> candidates = new ArrayList<>();
        for (ArcaneDictionaryDefinition.Entry entry : ArcaneDictionaryLoader.getEntries()) {
            if (entry.luck() < 0 || EXCLUDED_MIMIC.contains(entry.enchant())) {
                continue;
            }
            if (ArcaneDictionaryEffectRegistry.get(entry.enchant()) == null) {
                continue;
            }
            candidates.add(entry);
        }
        if (candidates.isEmpty()) {
            return null;
        }
        return ArcaneDictionaryLoot.weightedPick(candidates, random).enchant();
    }

    private static void write(ItemStack dictionary, MimicState state) {
        CompoundTag tag = root(dictionary);
        tag.putString(NBT_MIMIC_ID, state.traitId().toString());
        tag.putLong(NBT_MIMIC_UNTIL, state.untilTick());
        write(dictionary, tag);
    }

    private static CompoundTag root(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static void write(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}

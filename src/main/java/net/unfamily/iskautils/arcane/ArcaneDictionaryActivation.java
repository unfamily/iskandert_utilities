package net.unfamily.iskautils.arcane;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.util.CurioEquipUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Resolves which dictionary stack is active and whether traits require a Curio slot. */
public final class ArcaneDictionaryActivation {
    public enum Scope {
        /** Exactly one dictionary in Curios; all traits apply. */
        CURIO,
        /** Dictionary carried outside Curios; only traits with {@code only_curio: false}. */
        OFF_CURIO
    }

    public record Context(ItemStack dictionary, Scope scope) {
        public boolean curioFull() {
            return scope == Scope.CURIO;
        }
    }

    private static final Predicate<Item> DICTIONARY_ITEM = item -> item == ModItems.ARCANE_DICTIONARY.get();

    private ArcaneDictionaryActivation() {}

    /** Exactly one dictionary in Curios, or {@code null}. */
    public static ItemStack singleCurioDictionary(ServerPlayer player) {
        return resolveCurioDictionary(player);
    }

    public static Context resolve(ServerPlayer player) {
        ItemStack curio = resolveCurioDictionary(player);
        if (curio != null) {
            return new Context(curio, Scope.CURIO);
        }
        ItemStack carried = resolveCarriedDictionary(player);
        if (carried != null && hasOffCurioTrait(carried)) {
            return new Context(carried, Scope.OFF_CURIO);
        }
        return null;
    }

    public static boolean traitApplies(Identifier traitId, Scope scope) {
        if (scope == Scope.CURIO) {
            return true;
        }
        ArcaneDictionaryDefinition.Entry entry = ArcaneDictionaryLoader.findEntry(traitId);
        return entry != null && !entry.onlyCurio();
    }

    private static ItemStack resolveCurioDictionary(ServerPlayer player) {
        if (CurioEquipUtil.countDistinctEquippedCurioItems(player, DICTIONARY_ITEM) != 1) {
            return null;
        }
        ItemStack stack = CurioEquipUtil.findEquippedCurioStack(player, ModItems.ARCANE_DICTIONARY.get());
        return stack.isEmpty() ? null : stack;
    }

    /** Single dictionary in vanilla inventory/hands, not counting Curio slots. */
    private static ItemStack resolveCarriedDictionary(ServerPlayer player) {
        List<ItemStack> stacks = new ArrayList<>();
        ItemStack main = player.getMainHandItem();
        if (!main.isEmpty() && main.is(ModItems.ARCANE_DICTIONARY.get())) {
            stacks.add(main);
        }
        ItemStack off = player.getOffhandItem();
        if (!off.isEmpty() && off.is(ModItems.ARCANE_DICTIONARY.get()) && off != main) {
            stacks.add(off);
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(ModItems.ARCANE_DICTIONARY.get()) && !stacks.contains(stack)) {
                stacks.add(stack);
            }
        }
        if (stacks.size() != 1) {
            return null;
        }
        ItemStack only = stacks.get(0);
        return ArcaneDictionaryContents.hasTraits(only) ? only : null;
    }

    private static boolean hasOffCurioTrait(ItemStack dictionary) {
        for (ArcaneDictionaryContents.TraitSlot trait : ArcaneDictionaryContents.getTraits(dictionary)) {
            if (traitApplies(trait.id(), Scope.OFF_CURIO)) {
                return true;
            }
        }
        return false;
    }
}

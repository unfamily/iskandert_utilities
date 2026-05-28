package net.unfamily.iskautils.item.component;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AncientTabletContents {
    public static final int MAX_SLOTS = 64;
    private static final String NBT_SLOTS = "ancient_tablet_slots";

    private AncientTabletContents() {}

    /** One 1-count virtual slot for matching (maps back to a real slot index). */
    public record SlotView(int slotIndex, ItemStack stack1) {}

    /**
     * Best-effort preview stacks for tooltips. Uses the synthetic {@link DataComponents#BUNDLE_CONTENTS} list when present.
     */
    public static List<ItemStack> peekSlotsForTooltip(ItemStack tablet, HolderLookup.Provider providerOrNull) {
        BundleContents contents = tablet.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null) {
            return List.of();
        }
        List<ItemStack> out = new ArrayList<>();
        for (ItemStackTemplate t : contents.items()) {
            ItemStack s = t.create();
            if (!s.isEmpty()) {
                out.add(s.copyWithCount(1));
            }
        }
        return Collections.unmodifiableList(out);
    }

    public static int occupiedCount(ItemStack tablet) {
        var custom = tablet.get(DataComponents.CUSTOM_DATA);
        if (custom == null) {
            return 0;
        }
        CompoundTag tag = custom.copyTag();
        Tag slots = tag.get(NBT_SLOTS);
        if (!(slots instanceof ListTag list)) {
            return 0;
        }
        return list.size();
    }

    public static List<ItemStack> getSlots(ItemStack tablet, HolderLookup.Provider provider) {
        var custom = tablet.get(DataComponents.CUSTOM_DATA);
        if (custom == null) {
            return List.of();
        }
        CompoundTag tag = custom.copyTag();
        Tag slots = tag.get(NBT_SLOTS);
        if (!(slots instanceof ListTag list)) {
            return List.of();
        }
        List<ItemStack> out = new ArrayList<>(list.size());
        for (Tag entry : list) {
            if (entry instanceof CompoundTag compound) {
                ItemStack s = loadStack(provider, compound);
                if (!s.isEmpty()) {
                    out.add(s.copy());
                }
            }
        }
        return Collections.unmodifiableList(out);
    }

    private static ItemStack loadStack(HolderLookup.Provider provider, CompoundTag compound) {
        if (compound.contains("stack")) {
            return ItemStack.CODEC.parse(provider.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), compound.get("stack"))
                    .result()
                    .orElse(ItemStack.EMPTY)
                    .copy();
        }
        if (compound.contains("id")) {
            Identifier id = Identifier.tryParse(compound.getString("id").orElse(""));
            if (id != null && BuiltInRegistries.ITEM.containsKey(id)) {
                return new ItemStack(BuiltInRegistries.ITEM.getValue(id));
            }
        }
        return ItemStack.EMPTY;
    }

    private static CompoundTag saveStack(HolderLookup.Provider provider, ItemStack stack) {
        CompoundTag wrapper = new CompoundTag();
        Tag encoded = ItemStack.CODEC.encodeStart(provider.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), stack.copy())
                .result()
                .orElse(new CompoundTag());
        wrapper.put("stack", encoded);
        return wrapper;
    }

    public static void setSlots(ItemStack tablet, HolderLookup.Provider provider, List<ItemStack> slots) {
        List<ItemStack> normalized = new ArrayList<>(Math.min(slots.size(), MAX_SLOTS));
        for (ItemStack s : slots) {
            if (!s.isEmpty()) {
                normalized.add(s.copy());
            }
            if (normalized.size() >= MAX_SLOTS) {
                break;
            }
        }
        ListTag list = new ListTag();
        for (ItemStack s : normalized) {
            list.add(saveStack(provider, s));
        }
        CompoundTag root = tablet.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        root.put(NBT_SLOTS, list);
        tablet.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        syncBundlePreview(tablet, normalized);
    }

    /** Insert a whole stack as a slot (merge if compatible). */
    public static boolean tryInsert(ItemStack tablet, HolderLookup.Provider provider, ItemStack toInsert) {
        if (toInsert.isEmpty()) {
            return false;
        }
        List<ItemStack> slots = new ArrayList<>(getSlots(tablet, provider));
        for (int i = 0; i < slots.size(); i++) {
            ItemStack existing = slots.get(i);
            if (!ItemStack.isSameItemSameComponents(existing, toInsert)) {
                continue;
            }
            int max = existing.getMaxStackSize();
            int space = max - existing.getCount();
            if (space <= 0) {
                continue;
            }
            int move = Math.min(space, toInsert.getCount());
            existing.grow(move);
            toInsert.shrink(move);
            slots.set(i, existing);
            if (toInsert.isEmpty()) {
                setSlots(tablet, provider, slots);
                return true;
            }
        }
        if (slots.size() >= MAX_SLOTS) {
            return false;
        }
        slots.add(toInsert.copy());
        toInsert.setCount(0);
        setSlots(tablet, provider, slots);
        return true;
    }

    /**
     * Removes and returns a single item from the last inserted stack (LIFO). Returns empty if none.
     */
    public static ItemStack popLast(ItemStack tablet, HolderLookup.Provider provider) {
        List<ItemStack> slots = new ArrayList<>(getSlots(tablet, provider));
        if (slots.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int last = slots.size() - 1;
        ItemStack stack = slots.get(last);
        if (stack.isEmpty()) {
            slots.remove(last);
            setSlots(tablet, provider, slots);
            return ItemStack.EMPTY;
        }
        ItemStack out = stack.copyWithCount(1);
        stack.shrink(1);
        if (stack.isEmpty()) {
            slots.remove(last);
        } else {
            slots.set(last, stack);
        }
        setSlots(tablet, provider, slots);
        return out;
    }

    public static void clear(ItemStack tablet) {
        tablet.remove(DataComponents.BUNDLE_CONTENTS);
        tablet.remove(DataComponents.CUSTOM_DATA);
    }

    public static void dropAll(Level level, Player player, ItemStack tablet) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        HolderLookup.Provider provider = serverLevel.registryAccess();
        List<ItemStack> slots = new ArrayList<>(getSlots(tablet, provider));
        if (slots.isEmpty()) {
            return;
        }
        clear(tablet);
        Vec3 pos = player.position().add(0, player.getEyeHeight() * 0.5, 0);
        for (ItemStack stack : slots) {
            if (stack.isEmpty()) {
                continue;
            }
            ItemEntity entity = new ItemEntity(serverLevel, pos.x, pos.y, pos.z, stack.copy());
            entity.setDefaultPickUpDelay();
            entity.setDeltaMovement(
                    (serverLevel.getRandom().nextDouble() - 0.5) * 0.08,
                    0.15,
                    (serverLevel.getRandom().nextDouble() - 0.5) * 0.08);
            serverLevel.addFreshEntity(entity);
        }
        if (player instanceof ServerPlayer sp) {
            sp.containerMenu.broadcastChanges();
        }
    }

    public static void consumeSlotsAtIndices(ItemStack tablet, HolderLookup.Provider provider, List<Integer> indices) {
        List<ItemStack> slots = new ArrayList<>(getSlots(tablet, provider));
        if (indices.isEmpty() || slots.isEmpty()) {
            return;
        }
        Map<Integer, Integer> toConsume = new HashMap<>();
        for (int idx : indices) {
            toConsume.merge(idx, 1, Integer::sum);
        }
        for (Map.Entry<Integer, Integer> e : toConsume.entrySet()) {
            int idx = e.getKey();
            int count = e.getValue();
            if (idx < 0 || idx >= slots.size()) {
                continue;
            }
            ItemStack s = slots.get(idx);
            if (s.isEmpty()) {
                continue;
            }
            s.shrink(count);
            if (s.isEmpty()) {
                slots.set(idx, ItemStack.EMPTY);
            } else {
                slots.set(idx, s);
            }
        }
        slots.removeIf(ItemStack::isEmpty);
        setSlots(tablet, provider, slots);
    }

    /** Expand real stacks to a 1-count view list for matching (preserves insertion order). */
    public static List<SlotView> expandForMatching(ItemStack tablet, HolderLookup.Provider provider) {
        List<ItemStack> slots = getSlots(tablet, provider);
        if (slots.isEmpty()) {
            return List.of();
        }
        List<SlotView> out = new ArrayList<>();
        for (int i = 0; i < slots.size(); i++) {
            ItemStack s = slots.get(i);
            if (s.isEmpty()) continue;
            for (int k = 0; k < s.getCount(); k++) {
                out.add(new SlotView(i, s.copyWithCount(1)));
            }
        }
        return Collections.unmodifiableList(out);
    }

    private static void syncBundlePreview(ItemStack tablet, List<ItemStack> slots) {
        List<ItemStackTemplate> out = new ArrayList<>(Math.min(slots.size(), MAX_SLOTS));
        for (ItemStack s : slots) {
            if (!s.isEmpty()) {
                out.add(ItemStackTemplate.fromNonEmptyStack(s.copyWithCount(1)));
            }
        }
        tablet.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(out));
    }
}

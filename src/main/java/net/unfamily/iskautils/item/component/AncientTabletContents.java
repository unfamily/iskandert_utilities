package net.unfamily.iskautils.item.component;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AncientTabletContents {
    public static final int MAX_SLOTS = 64;
    private static final String NBT_SLOTS = "ancient_tablet_slots";

    private AncientTabletContents() {}

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
                    out.add(s);
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
                    .copyWithCount(1);
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
        Tag encoded = ItemStack.CODEC.encodeStart(provider.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), stack.copyWithCount(1))
                .result()
                .orElse(new CompoundTag());
        wrapper.put("stack", encoded);
        return wrapper;
    }

    public static void setSlots(ItemStack tablet, HolderLookup.Provider provider, List<ItemStack> slots) {
        ListTag list = new ListTag();
        int n = Math.min(slots.size(), MAX_SLOTS);
        for (int i = 0; i < n; i++) {
            ItemStack one = slots.get(i);
            if (!one.isEmpty()) {
                list.add(saveStack(provider, one));
            }
        }
        CompoundTag tag = new CompoundTag();
        tag.put(NBT_SLOTS, list);
        tablet.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
    }

    public static boolean tryInsert(ItemStack tablet, HolderLookup.Provider provider, ItemStack toInsert) {
        if (toInsert.isEmpty()) {
            return false;
        }
        List<ItemStack> slots = new ArrayList<>(getSlots(tablet, provider));
        if (slots.size() >= MAX_SLOTS) {
            return false;
        }
        slots.add(toInsert.copyWithCount(1));
        setSlots(tablet, provider, slots);
        return true;
    }

    public static void clear(ItemStack tablet) {
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
            ItemEntity entity = new ItemEntity(serverLevel, pos.x, pos.y, pos.z, stack.copyWithCount(1));
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
        indices.stream().sorted(Collections.reverseOrder()).forEach(i -> {
            if (i >= 0 && i < slots.size()) {
                slots.remove((int) i);
            }
        });
        setSlots(tablet, provider, slots);
    }
}

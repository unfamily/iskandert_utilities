package net.unfamily.iskautils.data.load.ancienttablet;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * One tablet slot requirement (always count 1 per slot; repeated requirements are separate instances).
 */
public sealed interface AncientTabletRequirement {

    boolean matches(ItemStack stack);

    /** For JEI grouping / display. */
    AncientTabletRequirement displayKey();

    record ItemRequirement(Item item, int minDamage) implements AncientTabletRequirement {
        public ItemRequirement(Item item) {
            this(item, -1);
        }

        @Override
        public boolean matches(ItemStack stack) {
            if (!stack.is(item)) {
                return false;
            }
            if (minDamage < 0) {
                return true;
            }
            return stack.getOrDefault(DataComponents.DAMAGE, 0) >= minDamage;
        }

        @Override
        public AncientTabletRequirement displayKey() {
            return this;
        }
    }

    record TagRequirement(TagKey<Item> tag) implements AncientTabletRequirement {
        @Override
        public boolean matches(ItemStack stack) {
            return stack.is(tag);
        }

        @Override
        public AncientTabletRequirement displayKey() {
            return this;
        }
    }

    static AncientTabletRequirement itemId(Identifier id, int minDamage) {
        return BuiltInRegistries.ITEM.getOptional(id)
                .map(item -> new ItemRequirement(item, minDamage))
                .orElse(null);
    }
}

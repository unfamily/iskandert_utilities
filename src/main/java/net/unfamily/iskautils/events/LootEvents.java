package net.unfamily.iskautils.events;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.TagEntry;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.unfamily.iskautils.IskaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * Event handler for loot table modifications
 */
@EventBusSubscriber
public class LootEvents {
    private static final Logger LOGGER = LoggerFactory.getLogger(LootEvents.class);

    /** Tag containing curio items that can drop from Artifacts' Mimic. See data/iska_utils/tags/item/curio_compat/mimic.json */
    private static final ResourceLocation MIMIC_CURIO_TAG = ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "curio_compat/mimic");

    // Possible paths for the mimic loot table of Artifacts
    // Since the paths can vary between versions, we check all possible variants
    private static final List<ResourceLocation> MIMIC_LOOT_TABLES = Arrays.asList(
        ResourceLocation.fromNamespaceAndPath("artifacts", "entities/mimic"),
        ResourceLocation.fromNamespaceAndPath("artifacts", "entity/mimic"),
        ResourceLocation.fromNamespaceAndPath("artifacts", "mimic")
    );

    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event) {
        ResourceLocation table = event.getName();

        LOGGER.debug("Loot table loaded: {}", table.toString());

        // Check if this is the mimic loot table
        if (MIMIC_LOOT_TABLES.contains(table)) {
            LOGGER.info("Found mimic loot table: {}. Adding curio items from tag {}", table, MIMIC_CURIO_TAG);

            // Create a new loot pool using items from the curio_compat/mimic tag
            // 5% chance to drop, then picks 1 item from the tag with equal weight per item
            LootPool.Builder poolBuilder = LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .add(TagEntry.expandTag(ItemTags.create(MIMIC_CURIO_TAG))
                            .setWeight(1)
                            .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1))))
                    .when(LootItemRandomChanceCondition.randomChance(0.05f));

            // Add our pool to the loot table
            event.getTable().addPool(poolBuilder.build());

            LOGGER.info("Successfully added curio items from tag {} to the mimic loot table", MIMIC_CURIO_TAG);
        }
    }
} 
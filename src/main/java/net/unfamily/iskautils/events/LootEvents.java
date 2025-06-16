package net.unfamily.iskautils.events;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.item.ModItems;
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
            LOGGER.info("Found mimic loot table: {}. Adding Necrotic Crystal Heart", table);
            
            // Create a new loot pool for our item
            LootPool.Builder poolBuilder = LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .add(LootItem.lootTableItem(ModItems.NECROTIC_CRYSTAL_HEART.get())
                            .setWeight(1)
                            .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1))))
                    .add(LootItem.lootTableItem(ModItems.MINING_EQUITIZER.get())
                            .setWeight(1)
                            .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1))))
                    .when(LootItemRandomChanceCondition.randomChance(0.05f));
                    
            // Add our pool to the loot table
            event.getTable().addPool(poolBuilder.build());
            
            LOGGER.info("Successfully added Necrotic Crystal Heart to the mimic loot table");
        }
    }
} 
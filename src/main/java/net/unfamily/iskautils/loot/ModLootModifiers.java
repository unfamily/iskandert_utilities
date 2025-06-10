package net.unfamily.iskautils.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.item.ModItems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

public class ModLootModifiers {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModLootModifiers.class);
    
    private static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIER_SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.GLOBAL_LOOT_MODIFIER_SERIALIZERS, IskaUtils.MOD_ID);

    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<MimicAdditionModifier>> MIMIC_ADDITION =
            LOOT_MODIFIER_SERIALIZERS.register("mimic_addition", 
                () -> MimicAdditionModifier.CODEC);

    public static void register(IEventBus eventBus) {
        LOOT_MODIFIER_SERIALIZERS.register(eventBus);
        LOGGER.info("Registered loot modifiers for IskaUtils");
    }

    /**
     * A loot modifier that adds the Necrotic Crystal Heart to Mimic drops
     */
    public static class MimicAdditionModifier extends LootModifier {
        private final float chance;

        public static final MapCodec<MimicAdditionModifier> CODEC = RecordCodecBuilder.mapCodec(inst ->
                LootModifier.codecStart(inst).and(
                        Codec.FLOAT.fieldOf("chance").orElse(0.0f).forGetter(m -> m.chance)
                ).apply(inst, MimicAdditionModifier::new));

        public MimicAdditionModifier(LootItemCondition[] conditionsIn, float chance) {
            super(conditionsIn);
            this.chance = chance;
        }

        @Override
        protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
            // Log for debugging
            Entity entity = context.getParamOrNull(LootContextParams.THIS_ENTITY);
            LOGGER.debug("MimicAdditionModifier applied. Entity type: {}", 
                    entity != null ? entity.getType().toString() : "unknown");
            
            // Check if we should add the item based on chance
            if (context.getRandom().nextFloat() <= this.chance) {
                // Add the necrotic crystal heart to the loot
                generatedLoot.add(new ItemStack(ModItems.NECROTIC_CRYSTAL_HEART.get()));
                LOGGER.debug("Added Necrotic Crystal Heart to loot");
            }
            
            return generatedLoot;
        }

        @Override
        public MapCodec<? extends IGlobalLootModifier> codec() {
            return MIMIC_ADDITION.get();
        }
    }
}

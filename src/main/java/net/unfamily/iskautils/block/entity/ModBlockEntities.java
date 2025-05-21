package net.unfamily.iskautils.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.block.ModBlocks;

import java.util.function.Supplier;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, IskaUtils.MOD_ID);

    // Registra l'entità blocco per Hellfire Igniter
    public static final Supplier<BlockEntityType<HellfireIgniterBlockEntity>> HELLFIRE_IGNITER =
            BLOCK_ENTITIES.register("hellfire_igniter",
                    () -> BlockEntityType.Builder.of(
                            ModBlockEntities::createHellfireIgniter,
                            ModBlocks.HELLFIRE_IGNITER.get()
                    ).build(null));

    // Constructor helper
    private static HellfireIgniterBlockEntity createHellfireIgniter(BlockPos pos, BlockState state) {
        return new HellfireIgniterBlockEntity(pos, state);
    }

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
        
        // Registra l'evento per le capabilities
        modEventBus.addListener(ModBlockEntities::registerCapabilities);
    }
    
    // Registrazione delle capabilities
    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        // Registra la capability per l'energy storage dell'HellfireIgniter
        event.registerBlockEntity(
            Capabilities.EnergyStorage.BLOCK, 
            HELLFIRE_IGNITER.get(),
            (blockEntity, context) -> {
                // Non registrare la capability se il blocco non può ricevere energia
                if (!blockEntity.canReceiveEnergy()) {
                    return null;
                }
                // Altrimenti, restituisci l'energy storage
                return blockEntity.getEnergyStorage();
            }
        );
    }
} 
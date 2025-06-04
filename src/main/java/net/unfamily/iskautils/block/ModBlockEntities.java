package net.unfamily.iskautils.block;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.entity.RubberLogEmptyBlockEntity;

/**
 * Registra tutte le BlockEntity utilizzate nella mod.
 */
public class ModBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, IskaUtils.MOD_ID);
            
    // BlockEntity per il blocco di legno di gomma vuoto
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RubberLogEmptyBlockEntity>> RUBBER_LOG_EMPTY =
            BLOCK_ENTITIES.register("rubber_log_empty",
                    () -> BlockEntityType.Builder.of(RubberLogEmptyBlockEntity::new,
                            ModBlocks.RUBBER_LOG_EMPTY.get()).build(null));
                    
    /**
     * Registra tutte le BlockEntity con l'event bus.
     */
    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
} 
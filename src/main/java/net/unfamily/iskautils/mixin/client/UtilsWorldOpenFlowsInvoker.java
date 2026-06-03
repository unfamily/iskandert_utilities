package net.unfamily.iskautils.mixin.client;

import com.mojang.serialization.Dynamic;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(WorldOpenFlows.class)
public interface UtilsWorldOpenFlowsInvoker {

    @Invoker(value = "openWorldLoadLevelStem", remap = false)
    void invokeOpenWorldLoadLevelStem(
            LevelStorageSource.LevelStorageAccess access,
            Dynamic<?> dynamic,
            boolean safeMode,
            Runnable onFail);
}

package net.unfamily.iskautils.item.custom;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.unfamily.iskautils.util.ModUtils;
import net.unfamily.iskautils.stage.StageRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.server.level.ServerPlayer;

public class MiningEquitizerCurioHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(MiningEquitizerCurioHandler.class);

    public static void register() {
        if (!ModUtils.isCuriosLoaded()) return;
    }
} 
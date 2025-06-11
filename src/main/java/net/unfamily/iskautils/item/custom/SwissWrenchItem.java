package net.unfamily.iskautils.item.custom;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.util.ModUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Special item for the Vector Charm that can be worn as a Curio when available.
 * When worn or held in hand, provides increased speed to vector plates.
 * Now includes energy storage and consumption system.
 */
public class SwissWrenchItem extends Item {
    private static final Logger LOGGER = LoggerFactory.getLogger(SwissWrenchItem.class);


    public SwissWrenchItem(Properties properties) {
        super(properties);
    }
}
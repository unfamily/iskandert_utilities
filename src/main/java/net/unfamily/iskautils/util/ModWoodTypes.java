package net.unfamily.iskautils.util;

import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.unfamily.iskautils.IskaUtils;

public class ModWoodTypes {
    public static final BlockSetType RUBBER_SET_TYPE = BlockSetType.register(new BlockSetType(IskaUtils.MOD_ID + ":rubber"));
    public static final WoodType RUBBER = WoodType.register(new WoodType(IskaUtils.MOD_ID + ":rubber", RUBBER_SET_TYPE));

    public static void register() {
        // Già registrati tramite i metodi register sopra
    }
} 
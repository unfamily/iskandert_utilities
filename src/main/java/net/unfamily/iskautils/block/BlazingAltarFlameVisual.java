package net.unfamily.iskautils.block;

import net.minecraft.util.StringRepresentable;

public enum BlazingAltarFlameVisual implements StringRepresentable {
    GLOW("glow"),
    BURNING("burning"),
    CURSED("cursed"),
    HIDDEN("hidden");

    private final String name;

    BlazingAltarFlameVisual(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}

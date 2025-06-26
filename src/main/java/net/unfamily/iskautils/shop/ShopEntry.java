package net.unfamily.iskautils.shop;

/**
 * Represents an entry in the shop system
 */
public class ShopEntry {
    public String id; // Identificatore univoco per l'entry
    public String inCategory;
    public String item; // Supporta anche data components: "minecraft:diamond_sword[enchantments={...}]"
    public int itemCount;
    public String valute;
    public double buy;
    public double sell;
    public ShopStage[] stages;
} 
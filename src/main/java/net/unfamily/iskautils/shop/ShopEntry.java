package net.unfamily.iskautils.shop;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an entry in the shop system.
 * Within a category, entries are ordered by {@link #priority} (higher first), then by resource id.
 */
public class ShopEntry {
    public String id;
    public String inCategory;
    /** Namespaced type id (e.g. {@code iska_utils:item}). Default item. */
    public ResourceLocation typeId = ShopEntryTypes.ITEM;
    /** Item selector (id, components, or {@code #tag}). Used when type is item. */
    public String item;
    /** Fluid selector (id or {@code #tag}). Used when type is fluid. */
    public String fluid;
    /** Gas/chemical id (no tags). Used when type is gas. */
    public String gas;
    /** Lang key for command/stage entry label. */
    public String display;
    /**
     * Optional GUI icon stem under {@code textures/gui/icons/} (e.g. {@code command_icon}).
     * Null/blank = type default icon.
     */
    public String icon;
    /** Commands run server-side on buy ({@code iska_utils:command}). */
    public List<String> commands = new ArrayList<>();
    /**
     * Stage rewards on buy ({@code iska_utils:stage}, JSON key {@code stage}).
     * Same fields as gate {@link #stages}: stage / stage_type / is.
     */
    public ShopStage[] stageRewards;
    /**
     * Generic quantity: item count, fluid/gas mB, or RF.
     * {@link #itemCount} mirrors this for legacy callers.
     */
    public int amount = 1;
    /** Legacy mirror of {@link #amount}. */
    public int itemCount;
    public String currency;
    public String valute;
    public double buy;
    public double sell;
    /** Display order within category: higher value = shown first. Default 0. */
    public int priority = 0;
    /** If true, item can be bought even when buy is 0; no currency is charged. */
    public boolean free = false;
    /** Gate requirements (JSON {@code stages}). */
    public ShopStage[] stages;
    /** Buy-side trade limits; null means defaults (team + always). */
    public ShopRepeatableRule repeatableBuy;
    /** Sell-side trade limits; null means defaults (team + always). */
    public ShopRepeatableRule repeatableSell;
}

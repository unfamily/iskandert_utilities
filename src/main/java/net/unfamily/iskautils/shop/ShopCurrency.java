package net.unfamily.iskautils.shop;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Represents a currency in the shop system
 */
public class ShopCurrency {
    /** Default display symbol. Avoid § — Minecraft treats it as a formatting code. */
    public static final String DEFAULT_SYMBOL = "¤";

    public String id;
    public String name;
    public String charSymbol;
    /** Display order: higher value = shown first. Default 0. */
    public int priority = 0;

    /** Higher priority first, then id. */
    public static Comparator<ShopCurrency> displayOrder() {
        return Comparator.comparingInt((ShopCurrency c) -> -c.priority)
                .thenComparing(c -> c.id != null ? c.id : "");
    }

    public static List<ShopCurrency> sorted(Collection<ShopCurrency> currencies) {
        return currencies.stream().sorted(displayOrder()).toList();
    }

    public static List<String> sortedIds(Collection<ShopCurrency> currencies) {
        return currencies.stream().sorted(displayOrder()).map(c -> c.id).toList();
    }
}

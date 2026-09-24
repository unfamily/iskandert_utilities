package net.unfamily.iskautils.obtaining;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.item.ModItems;

/**
 * Wandering trader trade helpers for Suspicious Delivery.
 * Cost comes from config {@code tweaks.002_suspicious_delivery_trade_cost} ({@code item_id;min-max}).
 */
public final class SuspiciousDeliveryTradeUtil {
    private static final int MAX_TRADE_USES = 1;
    /** Roll when the rare trade pool misses; keeps the trade common without allowing duplicates. */
    private static final float INJECT_IF_MISSING_CHANCE = 1.0f;
    /** Wandering traders offer nine trades; replace a common slot when injecting. */
    private static final int REPLACE_INDEX = 8;

    private static final String DEFAULT_COST_SPEC = "minecraft:emerald;12-16";
    private static final int DEFAULT_MIN = 12;
    private static final int DEFAULT_MAX = 16;

    private static Item tradeCostItem = Items.EMERALD;
    private static int tradeCostMin = DEFAULT_MIN;
    private static int tradeCostMax = DEFAULT_MAX;

    private SuspiciousDeliveryTradeUtil() {}

    /** Re-parse {@link Config#suspiciousDeliveryTradeCostSpec} after config bake/reload. */
    public static void reloadTradeCostFromConfig() {
        applyTradeCostSpec(Config.suspiciousDeliveryTradeCostSpec);
    }

    /**
     * Parses {@code item_id;min-max}. Invalid parts fall back to emerald / 12-16.
     */
    public static void applyTradeCostSpec(String raw) {
        Item item = Items.EMERALD;
        int min = DEFAULT_MIN;
        int max = DEFAULT_MAX;
        if (raw != null && !raw.isBlank()) {
            String trimmed = raw.trim();
            int sep = trimmed.lastIndexOf(';');
            String itemPart = sep >= 0 ? trimmed.substring(0, sep).trim() : trimmed;
            String rangePart = sep >= 0 ? trimmed.substring(sep + 1).trim() : "";
            Identifier id = Identifier.tryParse(itemPart);
            if (id != null) {
                Item resolved = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
                if (resolved != null && resolved != Items.AIR) {
                    item = resolved;
                }
            }
            if (!rangePart.isEmpty()) {
                int dash = rangePart.indexOf('-');
                try {
                    if (dash >= 0) {
                        int a = Integer.parseInt(rangePart.substring(0, dash).trim());
                        int b = Integer.parseInt(rangePart.substring(dash + 1).trim());
                        min = Math.min(a, b);
                        max = Math.max(a, b);
                    } else {
                        min = max = Integer.parseInt(rangePart.trim());
                    }
                } catch (NumberFormatException ignored) {
                    min = DEFAULT_MIN;
                    max = DEFAULT_MAX;
                }
            }
        }
        min = clampCount(min);
        max = clampCount(max);
        if (min > max) {
            int tmp = min;
            min = max;
            max = tmp;
        }
        tradeCostItem = item;
        tradeCostMin = min;
        tradeCostMax = max;
    }

    private static int clampCount(int count) {
        return Math.max(1, Math.min(64, count));
    }

    public static Item tradeCostItem() {
        return tradeCostItem;
    }

    public static int tradeCostMin() {
        return tradeCostMin;
    }

    public static int tradeCostMax() {
        return tradeCostMax;
    }

    public static ItemStack rollCostStack(RandomSource random) {
        int span = tradeCostMax - tradeCostMin;
        int count = tradeCostMin + (span > 0 ? random.nextInt(span + 1) : 0);
        return new ItemStack(tradeCostItem, count);
    }

    public static boolean isSuspiciousDeliveryOffer(MerchantOffer offer) {
        return offer.getResult().is(ModItems.SUSPICIOUS_DELIVERY.get());
    }

    public static boolean hasSuspiciousDeliveryTrade(MerchantOffers offers) {
        for (int i = 0; i < offers.size(); i++) {
            if (isSuspiciousDeliveryOffer(offers.get(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Builds a delivery offer using the configured cost item and count range.
     */
    public static MerchantOffer createOffer(RandomSource random) {
        ItemStack costItem = rollCostStack(random);
        return new MerchantOffer(
                new ItemCost(costItem.getItem(), costItem.getCount()),
                new ItemStack(ModItems.SUSPICIOUS_DELIVERY.get(), 1),
                MAX_TRADE_USES,
                1,
                0.05f);
    }

    /**
     * @deprecated Prefer {@link #createOffer(RandomSource)}; {@code existingOffers} is ignored.
     */
    public static MerchantOffer createOffer(RandomSource random, MerchantOffers existingOffers) {
        return createOffer(random);
    }

    private static MerchantOffer toSingleUseOffer(MerchantOffer source) {
        if (source.getMaxUses() == MAX_TRADE_USES) {
            return source;
        }
        return new MerchantOffer(
                source.getItemCostA(),
                source.getItemCostB(),
                source.getResult(),
                source.getUses(),
                MAX_TRADE_USES,
                source.getXp(),
                source.getPriceMultiplier());
    }

    /**
     * Keeps at most one suspicious delivery trade in the offer list.
     */
    public static void capToSingleTrade(MerchantOffers offers) {
        int keepIndex = -1;
        for (int i = 0; i < offers.size(); i++) {
            if (isSuspiciousDeliveryOffer(offers.get(i))) {
                if (keepIndex < 0) {
                    keepIndex = i;
                } else {
                    offers.remove(i);
                    i--;
                }
            }
        }
        if (keepIndex >= 0) {
            offers.set(keepIndex, toSingleUseOffer(offers.get(keepIndex)));
        }
    }

    /**
     * Caps duplicate delivery trades, then injects one if the rare pool roll missed.
     * If a delivery offer already exists, only caps — does not replace it.
     */
    public static void applyTraderTradeLimit(WanderingTrader trader) {
        MerchantOffers offers = trader.getOffers();
        capToSingleTrade(offers);
        if (hasSuspiciousDeliveryTrade(offers)) {
            return;
        }
        if (trader.getRandom().nextFloat() >= INJECT_IF_MISSING_CHANCE) {
            return;
        }
        MerchantOffer offer = createOffer(trader.getRandom());
        if (offers.isEmpty()) {
            offers.add(offer);
            return;
        }
        offers.set(Math.min(REPLACE_INDEX, offers.size() - 1), offer);
    }

    /** Default spec when config is missing (tests / early boot). */
    public static String defaultCostSpec() {
        return DEFAULT_COST_SPEC;
    }
}

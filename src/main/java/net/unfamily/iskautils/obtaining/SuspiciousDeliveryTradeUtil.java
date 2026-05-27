package net.unfamily.iskautils.obtaining;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.unfamily.iskautils.item.ModItems;

/**
 * Wandering trader trade helpers for Suspicious Delivery.
 */
public final class SuspiciousDeliveryTradeUtil {
    /** Roll when datapack pools miss; keeps the trade common without allowing duplicates. */
    private static final float INJECT_IF_MISSING_CHANCE = 1.0f;
    /** Wandering traders offer nine trades; replace a common slot when injecting. */
    private static final int REPLACE_INDEX = 8;

    private SuspiciousDeliveryTradeUtil() {}

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

    public static MerchantOffer createOffer(RandomSource random) {
        int emeraldCost = 12 + random.nextInt(5);
        return new MerchantOffer(
                new ItemCost(Items.EMERALD, emeraldCost),
                new ItemStack(ModItems.SUSPICIOUS_DELIVERY.get(), 1),
                8,
                1,
                0.05f);
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
    }

    /**
     * Caps duplicate delivery trades, then injects one if the datapack roll missed.
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
}

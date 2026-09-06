package net.unfamily.iskautils.shop;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Extensible shop entry type. Addons register implementations via {@link ShopEntryTypeRegistry}.
 */
public interface ShopEntryTypeHandler {

    ResourceLocation id();

    /** Short label for the shop editor type button (e.g. {@code ITEM}, {@code RF/FE}). */
    String editorLabel();

    /** Whether this type appears in the editor type cycle / is loadable on this loader. */
    default boolean isAvailable() {
        return true;
    }

    default boolean usesAmount() {
        return true;
    }

    default boolean usesResourceSelector() {
        return true;
    }

    default boolean usesSell() {
        return true;
    }

    /** When false, entry has no buy price (purchase runs the whole payload once per click). */
    default boolean usesBuy() {
        return true;
    }

    /** Whether the delivered payload and price scale with the requested buy quantity. */
    default boolean scalesWithBuyQuantity() {
        return usesBuy();
    }

    default boolean isBuyOnly() {
        return false;
    }

    default boolean isPlayerShopBrowsable(ShopEntry entry) {
        return ShopEntryHelper.hasTradeOffer(entry);
    }

    default boolean isPlayerShopTradable(ShopEntry entry) {
        return false;
    }

    default boolean isAutoShopSelectable(ShopEntry entry) {
        return ShopEntryHelper.hasTradeOffer(entry);
    }

    /** Read type-specific JSON fields into {@code entry} (already has common fields). */
    default void readExtras(com.google.gson.JsonObject json, ShopEntry entry) {}

    /** Write type-specific JSON fields. */
    default void writeExtras(com.google.gson.JsonObject json, ShopEntry entry) {}

    /**
     * Validate after load. Return false to skip the entry.
     */
    boolean validate(ShopEntry entry, String fileName);

    @Nullable
    default String resourceSelector(ShopEntry entry) {
        return null;
    }

    default Component displayName(ShopEntry entry) {
        String selector = resourceSelector(entry);
        return Component.literal(selector != null ? selector : id().toString());
    }

    default String displayLabel(ShopEntry entry) {
        return displayName(entry).getString();
    }

    /** Fixed GUI icon path under assets, or null to use item/fluid rendering. */
    @Nullable
    default ResourceLocation guiIcon() {
        return null;
    }

    /** Resolved GUI icon for an entry (custom {@link ShopEntry#icon} or type default). */
    @Nullable
    default ResourceLocation guiIcon(ShopEntry entry) {
        return ShopGuiIcons.resolve(entry != null ? entry.icon : null, guiIcon());
    }

    default ItemStack displayItemStack(ShopEntry entry) {
        return ItemStack.EMPTY;
    }

    /**
     * Execute a successful buy after currency is charged (and stages checked).
     * Return false if the purchase effect failed (caller should refund if needed).
     */
    default boolean onBuy(ServerPlayer player, ShopEntry entry, int quantity) {
        return false;
    }

    default boolean onSell(ServerPlayer player, ShopEntry entry, int quantity) {
        return false;
    }

    /** Editor: show display lang-key field + string-list submenu. */
    default boolean usesDisplayAndStringList() {
        return false;
    }

    /** Stage reward list ({@code iska_utils:stage}) using {@link ShopStage} rows. */
    default boolean usesStageRewards() {
        return false;
    }

    default boolean usesResultButton() {
        return usesDisplayAndStringList() || usesStageRewards();
    }

    /**
     * Editor: show optional/required Display field + icon cycle button
     * (command/stage required display; RF optional).
     */
    default boolean usesDisplayAndIcon() {
        return usesResultButton();
    }

    default int resultCount(ShopEntry entry) {
        if (usesDisplayAndStringList()) {
            return stringList(entry).size();
        }
        if (usesStageRewards() && entry.stageRewards != null) {
            return entry.stageRewards.length;
        }
        return 0;
    }

    /** JSON array key for string-list types ({@code commands} / {@code stage}). */
    @Nullable
    default String stringListJsonKey() {
        return null;
    }

    default List<String> stringList(ShopEntry entry) {
        return List.of();
    }

    default void setStringList(ShopEntry entry, List<String> values) {}
}

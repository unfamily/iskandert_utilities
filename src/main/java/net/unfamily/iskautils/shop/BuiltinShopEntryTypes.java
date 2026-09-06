package net.unfamily.iskautils.shop;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.unfamily.iskalib.stage.StageRegistry;
import net.unfamily.iskalib.team.ShopTeamManager;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.command.PlayerCommandSources;
import net.unfamily.iskautils.integration.mekanism.MekChemicalHelper;
import net.unfamily.iskautils.util.ModLogger;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Built-in {@link ShopEntryTypeHandler} registrations.
 */
public final class BuiltinShopEntryTypes {
    private static final ModLogger LOGGER = ModLogger.of(BuiltinShopEntryTypes.class);

    public static final Identifier RF_ICON = ShopGuiIcons.texture("rf_icon");
    public static final Identifier COMMAND_ICON = ShopGuiIcons.texture(ShopGuiIcons.COMMAND_DEFAULT);
    public static final Identifier STAGE_ICON = ShopGuiIcons.texture(ShopGuiIcons.STAGE_DEFAULT);
    public static final Identifier CURRENCY_ICON = ShopGuiIcons.texture(ShopGuiIcons.CURRENCY_DEFAULT);

    private BuiltinShopEntryTypes() {}

    public static void registerAll() {
        ShopEntryTypeRegistry.register(new ItemHandler());
        ShopEntryTypeRegistry.register(new FluidHandler());
        ShopEntryTypeRegistry.register(new GasHandler());
        ShopEntryTypeRegistry.register(new RfHandler());
        ShopEntryTypeRegistry.register(new CommandHandler());
        ShopEntryTypeRegistry.register(new StageGrantHandler());
        ShopEntryTypeRegistry.register(new CurrencyHandler());
    }

    private static abstract class BaseHandler implements ShopEntryTypeHandler {
        private final Identifier id;
        private final String editorLabel;

        protected BaseHandler(Identifier id, String editorLabel) {
            this.id = id;
            this.editorLabel = editorLabel;
        }

        @Override
        public Identifier id() {
            return id;
        }

        @Override
        public String editorLabel() {
            return editorLabel;
        }
    }

    private static final class ItemHandler extends BaseHandler {
        ItemHandler() {
            super(ShopEntryTypes.ITEM, "ITEM");
        }

        @Override
        public void readExtras(JsonObject json, ShopEntry entry) {
            if (json.has("item")) {
                entry.item = json.get("item").getAsString();
            }
        }

        @Override
        public void writeExtras(JsonObject json, ShopEntry entry) {
            if (entry.item != null) {
                json.addProperty("item", entry.item);
            }
        }

        @Override
        public boolean validate(ShopEntry entry, String fileName) {
            if (entry.item == null || entry.item.isBlank()) {
                LOGGER.warn("Skipping shop entry {} in {}: missing item", entry.id, fileName);
                return false;
            }
            return true;
        }

        @Override
        public String resourceSelector(ShopEntry entry) {
            return entry.item;
        }

        @Override
        public boolean isPlayerShopTradable(ShopEntry entry) {
            return true;
        }

        @Override
        public ItemStack displayItemStack(ShopEntry entry) {
            return ShopEntryHelper.displayStackForItemSelector(entry.item, entry.amount);
        }

        @Override
        public Component displayName(ShopEntry entry) {
            ItemStack stack = displayItemStack(entry);
            return !stack.isEmpty() ? stack.getHoverName() : Component.literal(entry.item != null ? entry.item : "");
        }
    }

    private static final class FluidHandler extends BaseHandler {
        FluidHandler() {
            super(ShopEntryTypes.FLUID, "FLUID");
        }

        @Override
        public void readExtras(JsonObject json, ShopEntry entry) {
            if (json.has("fluid")) {
                entry.fluid = json.get("fluid").getAsString();
            }
        }

        @Override
        public void writeExtras(JsonObject json, ShopEntry entry) {
            if (entry.fluid != null) {
                json.addProperty("fluid", entry.fluid);
            }
        }

        @Override
        public boolean validate(ShopEntry entry, String fileName) {
            if (entry.fluid == null || entry.fluid.isBlank()) {
                LOGGER.warn("Skipping shop entry {} in {}: missing fluid", entry.id, fileName);
                return false;
            }
            return true;
        }

        @Override
        public String resourceSelector(ShopEntry entry) {
            return entry.fluid;
        }

        @Override
        public Component displayName(ShopEntry entry) {
            FluidStack fluid = ShopEntryHelper.displayFluidForEntry(entry);
            return !fluid.isEmpty() ? fluid.getHoverName() : Component.literal(entry.fluid != null ? entry.fluid : "");
        }
    }

    private static final class GasHandler extends BaseHandler {
        GasHandler() {
            super(ShopEntryTypes.GAS, "GAS");
        }

        @Override
        public boolean isAvailable() {
            return MekChemicalHelper.isGasSupportEnabled();
        }

        @Override
        public void readExtras(JsonObject json, ShopEntry entry) {
            if (json.has("gas")) {
                entry.gas = json.get("gas").getAsString();
            }
        }

        @Override
        public void writeExtras(JsonObject json, ShopEntry entry) {
            if (entry.gas != null) {
                json.addProperty("gas", entry.gas);
            }
        }

        @Override
        public boolean validate(ShopEntry entry, String fileName) {
            if (!MekChemicalHelper.isGasSupportEnabled()) {
                LOGGER.warn("Skipping gas shop entry {} in {}: gas support disabled on this loader", entry.id, fileName);
                return false;
            }
            if (entry.gas == null || entry.gas.isBlank()) {
                LOGGER.warn("Skipping shop entry {} in {}: missing gas", entry.id, fileName);
                return false;
            }
            if (ShopEntryHelper.isTagSelector(entry.gas)) {
                LOGGER.warn("Skipping gas shop entry {} in {}: gas entries cannot use tags", entry.id, fileName);
                return false;
            }
            return true;
        }

        @Override
        public String resourceSelector(ShopEntry entry) {
            return entry.gas;
        }

        @Override
        public boolean isPlayerShopBrowsable(ShopEntry entry) {
            return MekChemicalHelper.isLoaded() && ShopEntryHelper.hasTradeOffer(entry);
        }

        @Override
        public boolean isAutoShopSelectable(ShopEntry entry) {
            return MekChemicalHelper.isLoaded() && ShopEntryHelper.hasTradeOffer(entry);
        }

        @Override
        public Component displayName(ShopEntry entry) {
            Object chemical = ShopEntryHelper.displayGasForEntry(entry);
            Component name = MekChemicalHelper.getDisplayName(chemical);
            if (name != null && !name.getString().isEmpty()) {
                return name;
            }
            return Component.literal(entry.gas != null ? entry.gas : "");
        }
    }

    private static final class RfHandler extends BaseHandler {
        RfHandler() {
            super(ShopEntryTypes.RF, "RF/FE");
        }

        @Override
        public boolean usesResourceSelector() {
            return false;
        }

        @Override
        public boolean usesDisplayAndIcon() {
            return true;
        }

        @Override
        public boolean validate(ShopEntry entry, String fileName) {
            return true;
        }

        @Override
        public void readExtras(JsonObject json, ShopEntry entry) {
            if (json.has("display")) {
                entry.display = json.get("display").getAsString();
            }
            if (json.has("icon")) {
                entry.icon = json.get("icon").getAsString();
            }
        }

        @Override
        public void writeExtras(JsonObject json, ShopEntry entry) {
            if (entry.display != null && !entry.display.isBlank()) {
                json.addProperty("display", entry.display);
            }
            if (entry.icon != null && !entry.icon.isBlank()
                    && !ShopGuiIcons.isDefaultIcon(entry.icon, RF_ICON)) {
                json.addProperty("icon", ShopGuiIcons.sanitizeStem(entry.icon));
            }
        }

        @Override
        @Nullable
        public String resourceSelector(ShopEntry entry) {
            return ShopEntryTypes.RF.toString();
        }

        @Override
        public Identifier guiIcon() {
            return RF_ICON;
        }

        @Override
        public Component displayName(ShopEntry entry) {
            if (entry.display != null && !entry.display.isBlank()) {
                return Component.translatable(entry.display);
            }
            return Component.translatable("gui.iska_utils.shop.other.rf");
        }
    }

    private static final class CurrencyHandler extends BaseHandler {
        CurrencyHandler() {
            super(ShopEntryTypes.CURRENCY, "CURRENCY");
        }

        @Override
        public boolean usesResourceSelector() {
            return false;
        }

        @Override
        public boolean usesDisplayAndIcon() {
            return true;
        }

        @Override
        public boolean usesCurrencyConvert() {
            return true;
        }

        @Override
        public boolean usesSell() {
            return false;
        }

        @Override
        public boolean usesBuy() {
            return true;
        }

        @Override
        public boolean isBuyOnly() {
            return true;
        }

        @Override
        public boolean scalesWithBuyQuantity() {
            return true;
        }

        @Override
        public void readExtras(JsonObject json, ShopEntry entry) {
            if (json.has("display")) {
                entry.display = json.get("display").getAsString();
            }
            if (json.has("icon")) {
                entry.icon = json.get("icon").getAsString();
            }
            if (json.has("target_currency")) {
                entry.targetCurrency = json.get("target_currency").getAsString();
            }
        }

        @Override
        public void writeExtras(JsonObject json, ShopEntry entry) {
            if (entry.display != null && !entry.display.isBlank()) {
                json.addProperty("display", entry.display);
            }
            if (entry.icon != null && !entry.icon.isBlank()
                    && !ShopGuiIcons.isDefaultIcon(entry.icon, CURRENCY_ICON)) {
                json.addProperty("icon", ShopGuiIcons.sanitizeStem(entry.icon));
            }
            if (entry.targetCurrency != null && !entry.targetCurrency.isBlank()) {
                json.addProperty("target_currency", entry.targetCurrency);
            }
        }

        @Override
        public boolean validate(ShopEntry entry, String fileName) {
            String source = entry.currency != null ? entry.currency : entry.valute;
            if (source == null || source.isBlank() || ShopLoader.getCurrency(source) == null) {
                LOGGER.warn("Skipping currency convert entry {} in {}: unknown source currency {}",
                        entry.id, fileName, source);
                return false;
            }
            if (entry.targetCurrency == null || entry.targetCurrency.isBlank()
                    || ShopLoader.getCurrency(entry.targetCurrency) == null) {
                LOGGER.warn("Skipping currency convert entry {} in {}: unknown target currency {}",
                        entry.id, fileName, entry.targetCurrency);
                return false;
            }
            if (source.equals(entry.targetCurrency)) {
                LOGGER.warn("Skipping currency convert entry {} in {}: source and target are the same",
                        entry.id, fileName);
                return false;
            }
            if (!entry.free && entry.buy <= 0) {
                LOGGER.warn("Skipping currency convert entry {} in {}: buy must be > 0 unless free",
                        entry.id, fileName);
                return false;
            }
            if (entry.amount < 1) {
                LOGGER.warn("Skipping currency convert entry {} in {}: amount must be >= 1",
                        entry.id, fileName);
                return false;
            }
            entry.sell = 0;
            entry.valute = source;
            entry.currency = source;
            return true;
        }

        @Override
        @Nullable
        public String resourceSelector(ShopEntry entry) {
            return ShopEntryTypes.CURRENCY.toString();
        }

        @Override
        public boolean isPlayerShopTradable(ShopEntry entry) {
            return true;
        }

        @Override
        public boolean isAutoShopSelectable(ShopEntry entry) {
            return false;
        }

        @Override
        public Identifier guiIcon() {
            return CURRENCY_ICON;
        }

        @Override
        public Component displayName(ShopEntry entry) {
            if (entry.display != null && !entry.display.isBlank()) {
                return Component.translatable(entry.display);
            }
            return Component.translatable("gui.iska_utils.shop.currency.convert",
                    currencyDisplayName(entry.currency != null ? entry.currency : entry.valute),
                    currencyDisplayName(entry.targetCurrency));
        }

        @Override
        public boolean onBuy(ServerPlayer player, ShopEntry entry, int quantity) {
            if (entry.targetCurrency == null || entry.targetCurrency.isBlank() || entry.amount < 1) {
                return false;
            }
            ShopTeamManager teamManager = ShopTeamManager.getInstance(
                    (net.minecraft.server.level.ServerLevel) player.level());
            String teamName = teamManager.getPlayerTeam(player);
            if (teamName == null) {
                return false;
            }
            double credit = (double) entry.amount * quantity;
            return teamManager.addTeamValutes(teamName, entry.targetCurrency, credit);
        }
    }

    private static Component currencyDisplayName(@Nullable String currencyId) {
        if (currencyId == null || currencyId.isBlank()) {
            return Component.literal("?");
        }
        ShopCurrency currency = ShopLoader.getCurrency(currencyId);
        if (currency != null && currency.name != null && !currency.name.isBlank()) {
            return Component.translatable(currency.name);
        }
        return Component.literal(currencyId);
    }

    private static final class CommandHandler extends BaseHandler {
        CommandHandler() {
            super(ShopEntryTypes.COMMAND, "COMMAND");
        }

        @Override
        public boolean usesAmount() {
            return false;
        }

        @Override
        public boolean usesResourceSelector() {
            return false;
        }

        @Override
        public boolean usesSell() {
            return false;
        }

        @Override
        public boolean usesBuy() {
            return true;
        }

        @Override
        public boolean scalesWithBuyQuantity() {
            return false;
        }

        @Override
        public boolean isBuyOnly() {
            return true;
        }

        @Override
        public boolean usesDisplayAndStringList() {
            return true;
        }

        @Override
        public String stringListJsonKey() {
            return "commands";
        }

        @Override
        public List<String> stringList(ShopEntry entry) {
            return entry.commands != null ? entry.commands : List.of();
        }

        @Override
        public void setStringList(ShopEntry entry, List<String> values) {
            entry.commands = values != null ? new ArrayList<>(values) : new ArrayList<>();
        }

        @Override
        public void readExtras(JsonObject json, ShopEntry entry) {
            if (json.has("display")) {
                entry.display = json.get("display").getAsString();
            }
            if (json.has("icon")) {
                entry.icon = json.get("icon").getAsString();
            }
            entry.commands = readStringArray(json, "commands");
        }

        @Override
        public void writeExtras(JsonObject json, ShopEntry entry) {
            if (entry.display != null) {
                json.addProperty("display", entry.display);
            }
            if (entry.icon != null && !entry.icon.isBlank()
                    && !ShopGuiIcons.isDefaultIcon(entry.icon, COMMAND_ICON)) {
                json.addProperty("icon", ShopGuiIcons.sanitizeStem(entry.icon));
            }
            json.add("commands", toStringArray(entry.commands));
        }

        @Override
        public boolean validate(ShopEntry entry, String fileName) {
            if (entry.display == null || entry.display.isBlank()) {
                LOGGER.warn("Skipping command shop entry {} in {}: missing display lang key", entry.id, fileName);
                return false;
            }
            if (entry.commands == null || entry.commands.isEmpty()) {
                LOGGER.warn("Skipping command shop entry {} in {}: empty commands", entry.id, fileName);
                return false;
            }
            entry.sell = 0;
            return true;
        }

        @Override
        public boolean isPlayerShopTradable(ShopEntry entry) {
            return entry.commands != null && !entry.commands.isEmpty();
        }

        @Override
        public boolean isAutoShopSelectable(ShopEntry entry) {
            return false;
        }

        @Override
        public Identifier guiIcon() {
            return COMMAND_ICON;
        }

        @Override
        public Component displayName(ShopEntry entry) {
            return entry.display != null && !entry.display.isBlank()
                    ? Component.translatable(entry.display)
                    : Component.literal("command");
        }

        @Override
        public boolean onBuy(ServerPlayer player, ShopEntry entry, int quantity) {
            if (entry.commands == null || entry.commands.isEmpty()) {
                return false;
            }
            var server = player.level().getServer();
            if (server == null) {
                return false;
            }
            for (int q = 0; q < quantity; q++) {
                for (String command : entry.commands) {
                    if (command == null || command.isBlank()) {
                        continue;
                    }
                    try {
                        server.getCommands().performPrefixedCommand(PlayerCommandSources.at(player), command.trim());
                    } catch (Exception e) {
                        LOGGER.error("Shop command entry {} failed: {}", entry.id, e.getMessage());
                        return false;
                    }
                }
            }
            return true;
        }
    }

    private static final class StageGrantHandler extends BaseHandler {
        StageGrantHandler() {
            super(ShopEntryTypes.STAGE, "STAGE");
        }

        @Override
        public boolean usesAmount() {
            return false;
        }

        @Override
        public boolean usesResourceSelector() {
            return false;
        }

        @Override
        public boolean usesSell() {
            return false;
        }

        @Override
        public boolean usesBuy() {
            return true;
        }

        @Override
        public boolean scalesWithBuyQuantity() {
            return false;
        }

        @Override
        public boolean isBuyOnly() {
            return true;
        }

        @Override
        public boolean usesStageRewards() {
            return true;
        }

        @Override
        public void readExtras(JsonObject json, ShopEntry entry) {
            if (json.has("display")) {
                entry.display = json.get("display").getAsString();
            }
            if (json.has("icon")) {
                entry.icon = json.get("icon").getAsString();
            }
            entry.stageRewards = readStageRewardArray(json, "stage");
        }

        @Override
        public void writeExtras(JsonObject json, ShopEntry entry) {
            if (entry.display != null) {
                json.addProperty("display", entry.display);
            }
            if (entry.icon != null && !entry.icon.isBlank()
                    && !ShopGuiIcons.isDefaultIcon(entry.icon, STAGE_ICON)) {
                json.addProperty("icon", ShopGuiIcons.sanitizeStem(entry.icon));
            }
            json.add("stage", writeStageRewardArray(entry.stageRewards));
        }

        @Override
        public boolean validate(ShopEntry entry, String fileName) {
            if (entry.display == null || entry.display.isBlank()) {
                LOGGER.warn("Skipping stage shop entry {} in {}: missing display lang key", entry.id, fileName);
                return false;
            }
            if (entry.stageRewards == null || entry.stageRewards.length == 0) {
                LOGGER.warn("Skipping stage shop entry {} in {}: empty stage list", entry.id, fileName);
                return false;
            }
            entry.sell = 0;
            return true;
        }

        @Override
        public boolean isPlayerShopTradable(ShopEntry entry) {
            return entry.stageRewards != null && entry.stageRewards.length > 0;
        }

        @Override
        public boolean isAutoShopSelectable(ShopEntry entry) {
            return false;
        }

        @Override
        public Identifier guiIcon() {
            return STAGE_ICON;
        }

        @Override
        public Component displayName(ShopEntry entry) {
            return entry.display != null && !entry.display.isBlank()
                    ? Component.translatable(entry.display)
                    : Component.literal("stage");
        }

        @Override
        public boolean onBuy(ServerPlayer player, ShopEntry entry, int quantity) {
            if (entry.stageRewards == null || entry.stageRewards.length == 0) {
                return false;
            }
            var level = ((net.minecraft.server.level.ServerLevel) player.level());
            for (int q = 0; q < quantity; q++) {
                for (ShopStage grant : entry.stageRewards) {
                    if (grant == null || grant.stage == null || grant.stage.isBlank()) {
                        continue;
                    }
                    applyStageReward(player, level, grant);
                }
            }
            return true;
        }
    }

    private static void applyStageReward(ServerPlayer player, net.minecraft.server.level.ServerLevel level, ShopStage grant) {
        String type = grant.stageType != null ? grant.stageType.toLowerCase() : "player";
        boolean set = grant.is;
        String id = grant.stage.trim();
        switch (type) {
            case "world" -> {
                if (set) {
                    StageRegistry.addWorldStage(level, id);
                } else {
                    StageRegistry.removeWorldStage(level, id);
                }
            }
            case "team" -> {
                String team = net.unfamily.iskalib.team.ShopTeamManager.getInstance(level).getPlayerTeam(player);
                if (team == null) {
                    LOGGER.warn("Stage reward team apply skipped: player {} has no team", player.getName().getString());
                    return;
                }
                if (set) {
                    StageRegistry.addTeamStage(level, team, id);
                } else {
                    StageRegistry.removeTeamStage(level, team, id);
                }
            }
            default -> {
                if (set) {
                    StageRegistry.addPlayerStage(player, id, true);
                } else {
                    StageRegistry.removePlayerStage(player, id, true);
                }
            }
        }
    }

    private static ShopStage[] readStageRewardArray(JsonObject json, String key) {
        if (!json.has(key)) {
            return new ShopStage[0];
        }
        if (json.get(key).isJsonArray()) {
            JsonArray arr = json.getAsJsonArray(key);
            List<ShopStage> list = new ArrayList<>();
            for (JsonElement el : arr) {
                if (el == null) {
                    continue;
                }
                if (el.isJsonPrimitive()) {
                    ShopStage st = new ShopStage();
                    st.stage = el.getAsString();
                    st.stageType = "player";
                    st.is = true;
                    if (st.stage != null && !st.stage.isBlank()) {
                        list.add(st);
                    }
                } else if (el.isJsonObject()) {
                    JsonObject o = el.getAsJsonObject();
                    ShopStage st = new ShopStage();
                    st.stage = o.has("stage") ? o.get("stage").getAsString() : "";
                    st.stageType = o.has("stage_type") ? o.get("stage_type").getAsString() : "player";
                    st.is = !o.has("is") || o.get("is").getAsBoolean();
                    if (st.stage != null && !st.stage.isBlank()) {
                        list.add(st);
                    }
                }
            }
            return list.toArray(new ShopStage[0]);
        }
        return new ShopStage[0];
    }

    private static JsonArray writeStageRewardArray(@Nullable ShopStage[] rewards) {
        JsonArray array = new JsonArray();
        if (rewards != null) {
            for (ShopStage st : rewards) {
                if (st == null || st.stage == null || st.stage.isBlank()) {
                    continue;
                }
                JsonObject o = new JsonObject();
                o.addProperty("stage", st.stage);
                o.addProperty("stage_type", st.stageType != null ? st.stageType : "player");
                o.addProperty("is", st.is);
                array.add(o);
            }
        }
        return array;
    }

    private static List<String> readStringArray(JsonObject json, String key) {
        List<String> out = new ArrayList<>();
        if (!json.has(key) || !json.get(key).isJsonArray()) {
            return out;
        }
        for (JsonElement el : json.getAsJsonArray(key)) {
            if (el != null && el.isJsonPrimitive()) {
                String s = el.getAsString();
                if (s != null && !s.isBlank()) {
                    out.add(s.trim());
                }
            }
        }
        return out;
    }

    private static JsonArray toStringArray(@Nullable List<String> values) {
        JsonArray array = new JsonArray();
        if (values != null) {
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    array.add(value);
                }
            }
        }
        return array;
    }
}

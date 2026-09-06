package net.unfamily.iskautils.shop;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

/**
 * Optional purchase-limit rule on a shop entry. Defaults: scope=team, when=always.
 * Omitted from JSON when {@link #isDefault()}.
 */
public class ShopRepeatableRule {
    public static final String SCOPE_TEAM = "team";
    public static final String SCOPE_PLAYER = "player";
    public static final String SCOPE_ALL = "all";

    public static final String WHEN_ALWAYS = "always";
    public static final String WHEN_DAILY = "daily";
    public static final String WHEN_WEEKLY = "weekly";
    public static final String WHEN_MONTHLY = "monthly";
    public static final String WHEN_YEARLY = "yearly";
    public static final String WHEN_ONLY = "only";

    /** {@code team} | {@code player} | {@code all} */
    public String scope = SCOPE_TEAM;
    /** {@code always} | {@code daily} | {@code weekly} | {@code monthly} | {@code yearly} | {@code only} */
    public String when = WHEN_ALWAYS;
    /** {@code HH:mm} server-local reset time for period modes. Default midnight. */
    public String resetTime = "00:00";
    /** Weekly: Mon=1..Sun=7. Monthly/yearly: day of month 1–31. */
    public int resetDay = 1;
    /** Yearly: month 1–12. Unused otherwise. */
    public int resetMonth = 1;
    /** Max purchases per period, or lifetime for {@code only}. Default 1. */
    public int count = 1;

    public static ShopRepeatableRule defaults() {
        return new ShopRepeatableRule();
    }

    public static void readEntryRules(JsonObject entryJson, ShopEntry entry) {
        if (!entryJson.has("repeatable") || !entryJson.get("repeatable").isJsonObject()) {
            return;
        }
        JsonObject repeatable = entryJson.getAsJsonObject("repeatable");
        if (repeatable.has("buy") || repeatable.has("sell")) {
            entry.repeatableBuy = repeatable.has("buy") && repeatable.get("buy").isJsonObject()
                    ? fromJson(repeatable.getAsJsonObject("buy")) : null;
            entry.repeatableSell = repeatable.has("sell") && repeatable.get("sell").isJsonObject()
                    ? fromJson(repeatable.getAsJsonObject("sell")) : null;
        } else {
            // Legacy flat repeatable rules limited purchases only.
            entry.repeatableBuy = fromJson(repeatable);
        }
    }

    public static void writeEntryRules(JsonObject entryJson, ShopEntry entry) {
        JsonObject buy = entry.repeatableBuy != null ? entry.repeatableBuy.toJsonOptional() : null;
        JsonObject sell = entry.repeatableSell != null ? entry.repeatableSell.toJsonOptional() : null;
        if (buy == null && sell == null) {
            return;
        }
        JsonObject repeatable = new JsonObject();
        if (buy != null) {
            repeatable.add("buy", buy);
        }
        if (sell != null) {
            repeatable.add("sell", sell);
        }
        entryJson.add("repeatable", repeatable);
    }

    public boolean isDefault() {
        return SCOPE_TEAM.equalsIgnoreCase(nullTo(scope, SCOPE_TEAM))
                && WHEN_ALWAYS.equalsIgnoreCase(nullTo(when, WHEN_ALWAYS))
                && "00:00".equals(normalizeTime(resetTime))
                && resetDay == 1
                && resetMonth == 1
                && count == 1;
    }

    public ShopRepeatableRule copy() {
        ShopRepeatableRule c = new ShopRepeatableRule();
        c.scope = scope;
        c.when = when;
        c.resetTime = resetTime;
        c.resetDay = resetDay;
        c.resetMonth = resetMonth;
        c.count = count;
        return c;
    }

    public static ShopRepeatableRule fromJson(@Nullable JsonObject o) {
        if (o == null) {
            return null;
        }
        ShopRepeatableRule r = new ShopRepeatableRule();
        if (o.has("scope")) {
            r.scope = o.get("scope").getAsString();
        }
        if (o.has("when")) {
            r.when = o.get("when").getAsString();
        }
        if (o.has("reset_time")) {
            r.resetTime = o.get("reset_time").getAsString();
        }
        if (o.has("reset_day")) {
            r.resetDay = o.get("reset_day").getAsInt();
        }
        if (o.has("reset_month")) {
            r.resetMonth = o.get("reset_month").getAsInt();
        }
        if (o.has("count")) {
            r.count = Math.max(1, o.get("count").getAsInt());
        }
        return r.isDefault() ? null : r;
    }

    @Nullable
    public JsonObject toJsonOptional() {
        if (isDefault()) {
            return null;
        }
        JsonObject o = new JsonObject();
        String sc = nullTo(scope, SCOPE_TEAM);
        String wh = nullTo(when, WHEN_ALWAYS);
        if (!SCOPE_TEAM.equalsIgnoreCase(sc)) {
            o.addProperty("scope", sc.toLowerCase());
        }
        if (!WHEN_ALWAYS.equalsIgnoreCase(wh)) {
            o.addProperty("when", wh.toLowerCase());
        }
        if (!WHEN_ALWAYS.equalsIgnoreCase(wh) && !WHEN_ONLY.equalsIgnoreCase(wh)) {
            String time = normalizeTime(resetTime);
            if (!"00:00".equals(time)) {
                o.addProperty("reset_time", time);
            }
            if (WHEN_WEEKLY.equalsIgnoreCase(wh) || WHEN_MONTHLY.equalsIgnoreCase(wh) || WHEN_YEARLY.equalsIgnoreCase(wh)) {
                if (resetDay != 1) {
                    o.addProperty("reset_day", resetDay);
                }
            }
            if (WHEN_YEARLY.equalsIgnoreCase(wh) && resetMonth != 1) {
                o.addProperty("reset_month", resetMonth);
            }
        }
        if (count != 1) {
            o.addProperty("count", count);
        }
        // If when is non-always but all other fields default, still need when in JSON
        if (o.size() == 0 && !WHEN_ALWAYS.equalsIgnoreCase(wh)) {
            o.addProperty("when", wh.toLowerCase());
        }
        if (!SCOPE_TEAM.equalsIgnoreCase(sc) && !o.has("scope")) {
            o.addProperty("scope", sc.toLowerCase());
        }
        return o.size() == 0 ? null : o;
    }

    public static String normalizeTime(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return "00:00";
        }
        String t = raw.trim();
        if (t.matches("\\d{1,2}:\\d{2}")) {
            String[] p = t.split(":");
            int h = Math.max(0, Math.min(23, Integer.parseInt(p[0])));
            int m = Math.max(0, Math.min(59, Integer.parseInt(p[1])));
            return String.format("%02d:%02d", h, m);
        }
        return "00:00";
    }

    private static String nullTo(@Nullable String v, String def) {
        return v == null || v.isBlank() ? def : v.trim();
    }
}

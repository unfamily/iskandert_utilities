package net.unfamily.iskautils.shop;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.unfamily.iskalib.team.ShopTeamManager;
import org.jetbrains.annotations.Nullable;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.Map;

/**
 * Tracks shop purchase counts for {@link ShopRepeatableRule} limits (server timezone).
 */
public class ShopPurchaseLimitsData extends SavedData {
    public enum TradeSide {
        BUY("buy"), SELL("sell");

        private final String id;

        TradeSide(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }

    private static final String DATA_NAME = "iska_utils_shop_purchase_limits";

    private final Map<String, EntryCounter> counters = new HashMap<>();

    public static ShopPurchaseLimitsData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(ShopPurchaseLimitsData::new, ShopPurchaseLimitsData::load),
                DATA_NAME);
    }

    public static ShopPurchaseLimitsData load(CompoundTag tag, HolderLookup.Provider registries) {
        ShopPurchaseLimitsData data = new ShopPurchaseLimitsData();
        ListTag list = tag.getList("counters", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            String key = c.getString("key");
            EntryCounter ec = new EntryCounter();
            ec.periodId = c.getString("period");
            ec.count = c.getInt("count");
            data.counters.put(key, ec);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<String, EntryCounter> e : counters.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.putString("key", e.getKey());
            c.putString("period", e.getValue().periodId != null ? e.getValue().periodId : "");
            c.putInt("count", e.getValue().count);
            list.add(c);
        }
        tag.put("counters", list);
        return tag;
    }

    public boolean canTrade(ServerPlayer player, ShopEntry entry, TradeSide side, int units) {
        ShopTeamManager tm = ShopTeamManager.getInstance(player.serverLevel());
        return canTrade(player.serverLevel(), player.getUUID(), tm.getPlayerTeam(player), entry, side, units);
    }

    public boolean canTrade(ServerLevel level, @Nullable java.util.UUID playerId, @Nullable String teamKey,
                            ShopEntry entry, TradeSide side, int units) {
        ShopRepeatableRule rule = effective(entry, side);
        String scopeKey = resolveScope(playerId, teamKey, rule);
        if (scopeKey == null) {
            return false;
        }
        if (ShopRepeatableRule.WHEN_ALWAYS.equalsIgnoreCase(rule.when)) {
            return true;
        }
        String key = counterKey(entry.id, side, scopeKey);
        String periodId = currentPeriodId(rule);
        EntryCounter ec = counters.get(key);
        if (ec == null || !periodId.equals(ec.periodId)) {
            return units <= Math.max(1, rule.count);
        }
        return ec.count + Math.max(1, units) <= Math.max(1, rule.count);
    }

    public void recordTrade(ServerPlayer player, ShopEntry entry, TradeSide side, int units) {
        ShopTeamManager tm = ShopTeamManager.getInstance(player.serverLevel());
        recordTrade(player.serverLevel(), player.getUUID(), tm.getPlayerTeam(player), entry, side, units);
    }

    public void recordTrade(ServerLevel level, @Nullable java.util.UUID playerId, @Nullable String teamKey,
                            ShopEntry entry, TradeSide side, int units) {
        ShopRepeatableRule rule = effective(entry, side);
        if (units <= 0) {
            return;
        }
        String scopeKey = resolveScope(playerId, teamKey, rule);
        if (scopeKey == null) {
            return;
        }
        if (ShopRepeatableRule.WHEN_ALWAYS.equalsIgnoreCase(rule.when)) {
            return;
        }
        String key = counterKey(entry.id, side, scopeKey);
        String periodId = currentPeriodId(rule);
        EntryCounter ec = counters.computeIfAbsent(key, k -> new EntryCounter());
        if (!periodId.equals(ec.periodId)) {
            ec.periodId = periodId;
            ec.count = 0;
        }
        ec.count += units;
        setDirty();
    }

    public static ShopRepeatableRule effective(@Nullable ShopEntry entry, TradeSide side) {
        ShopRepeatableRule rule = entry == null ? null
                : (side == TradeSide.BUY ? entry.repeatableBuy : entry.repeatableSell);
        if (rule == null) {
            return ShopRepeatableRule.defaults();
        }
        return rule;
    }

    @Nullable
    public static String resolveScope(@Nullable java.util.UUID playerId, @Nullable String teamKey,
                                      ShopRepeatableRule rule) {
        String scope = rule.scope != null ? rule.scope : ShopRepeatableRule.SCOPE_TEAM;
        return switch (scope.toLowerCase()) {
            case ShopRepeatableRule.SCOPE_PLAYER -> playerId != null ? playerId.toString() : null;
            case ShopRepeatableRule.SCOPE_ALL -> "all";
            default -> teamKey;
        };
    }

    private static String counterKey(String entryId, TradeSide side, String scopeKey) {
        return entryId + "|" + side.id() + "|" + scopeKey;
    }

    public static String currentPeriodId(ShopRepeatableRule rule) {
        String when = rule.when != null ? rule.when.toLowerCase() : ShopRepeatableRule.WHEN_ALWAYS;
        if (ShopRepeatableRule.WHEN_ALWAYS.equals(when)) {
            return "always";
        }
        if (ShopRepeatableRule.WHEN_ONLY.equals(when)) {
            return "only";
        }
        ZoneId zone = ZoneId.systemDefault();
        ZonedDateTime now = ZonedDateTime.now(zone);
        LocalTime reset = parseResetTime(rule.resetTime);
        return switch (when) {
            case ShopRepeatableRule.WHEN_DAILY -> periodStartDaily(now, reset).toLocalDate().toString();
            case ShopRepeatableRule.WHEN_WEEKLY -> {
                DayOfWeek dow = DayOfWeek.of(Math.max(1, Math.min(7, rule.resetDay)));
                yield periodStartWeekly(now, dow, reset).toLocalDate().toString();
            }
            case ShopRepeatableRule.WHEN_MONTHLY -> {
                int day = Math.max(1, Math.min(31, rule.resetDay));
                yield periodStartMonthly(now, day, reset).toLocalDate().withDayOfMonth(1).toString();
            }
            case ShopRepeatableRule.WHEN_YEARLY -> {
                int month = Math.max(1, Math.min(12, rule.resetMonth));
                int day = Math.max(1, Math.min(31, rule.resetDay));
                yield String.valueOf(periodStartYearly(now, month, day, reset).getYear());
            }
            default -> "always";
        };
    }

    @Nullable
    public static Instant nextResetInstant(ShopRepeatableRule rule) {
        String when = rule.when != null ? rule.when.toLowerCase() : ShopRepeatableRule.WHEN_ALWAYS;
        if (ShopRepeatableRule.WHEN_ALWAYS.equals(when) || ShopRepeatableRule.WHEN_ONLY.equals(when)) {
            return null;
        }
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        LocalTime reset = parseResetTime(rule.resetTime);
        ZonedDateTime start = switch (when) {
            case ShopRepeatableRule.WHEN_DAILY -> periodStartDaily(now, reset);
            case ShopRepeatableRule.WHEN_WEEKLY -> periodStartWeekly(
                    now, DayOfWeek.of(Math.max(1, Math.min(7, rule.resetDay))), reset);
            case ShopRepeatableRule.WHEN_MONTHLY -> periodStartMonthly(
                    now, Math.max(1, Math.min(31, rule.resetDay)), reset);
            case ShopRepeatableRule.WHEN_YEARLY -> periodStartYearly(
                    now, Math.max(1, Math.min(12, rule.resetMonth)),
                    Math.max(1, Math.min(31, rule.resetDay)), reset);
            default -> null;
        };
        if (start == null) {
            return null;
        }
        return switch (when) {
            case ShopRepeatableRule.WHEN_DAILY -> start.plusDays(1).toInstant();
            case ShopRepeatableRule.WHEN_WEEKLY -> start.plusWeeks(1).toInstant();
            case ShopRepeatableRule.WHEN_MONTHLY -> {
                LocalDate next = start.toLocalDate().plusMonths(1);
                int day = Math.min(Math.max(1, rule.resetDay), next.lengthOfMonth());
                yield next.withDayOfMonth(day).atTime(reset).atZone(start.getZone()).toInstant();
            }
            case ShopRepeatableRule.WHEN_YEARLY -> {
                int year = start.getYear() + 1;
                int month = Math.max(1, Math.min(12, rule.resetMonth));
                int day = Math.min(Math.max(1, rule.resetDay), LocalDate.of(year, month, 1).lengthOfMonth());
                yield LocalDate.of(year, month, day).atTime(reset).atZone(start.getZone()).toInstant();
            }
            default -> null;
        };
    }

    private static LocalTime parseResetTime(@Nullable String raw) {
        String t = ShopRepeatableRule.normalizeTime(raw);
        String[] p = t.split(":");
        return LocalTime.of(Integer.parseInt(p[0]), Integer.parseInt(p[1]));
    }

    private static ZonedDateTime periodStartDaily(ZonedDateTime now, LocalTime reset) {
        ZonedDateTime todayReset = now.toLocalDate().atTime(reset).atZone(now.getZone());
        if (now.isBefore(todayReset)) {
            return todayReset.minusDays(1);
        }
        return todayReset;
    }

    private static ZonedDateTime periodStartWeekly(ZonedDateTime now, DayOfWeek dow, LocalTime reset) {
        LocalDate thisWeek = now.toLocalDate().with(TemporalAdjusters.previousOrSame(dow));
        ZonedDateTime boundary = thisWeek.atTime(reset).atZone(now.getZone());
        if (now.isBefore(boundary)) {
            return boundary.minusWeeks(1);
        }
        return boundary;
    }

    private static ZonedDateTime periodStartMonthly(ZonedDateTime now, int day, LocalTime reset) {
        LocalDate date = now.toLocalDate().withDayOfMonth(Math.min(day, now.toLocalDate().lengthOfMonth()));
        ZonedDateTime boundary = date.atTime(reset).atZone(now.getZone());
        if (now.isBefore(boundary)) {
            LocalDate prev = now.toLocalDate().minusMonths(1);
            int d = Math.min(day, prev.lengthOfMonth());
            return prev.withDayOfMonth(d).atTime(reset).atZone(now.getZone());
        }
        return boundary;
    }

    private static ZonedDateTime periodStartYearly(ZonedDateTime now, int month, int day, LocalTime reset) {
        int d = Math.min(day, LocalDate.of(now.getYear(), month, 1).lengthOfMonth());
        ZonedDateTime boundary = LocalDate.of(now.getYear(), month, d).atTime(reset).atZone(now.getZone());
        if (now.isBefore(boundary)) {
            int y = now.getYear() - 1;
            int d2 = Math.min(day, LocalDate.of(y, month, 1).lengthOfMonth());
            return LocalDate.of(y, month, d2).atTime(reset).atZone(now.getZone());
        }
        return boundary;
    }

    private static final class EntryCounter {
        String periodId = "";
        int count;
    }
}

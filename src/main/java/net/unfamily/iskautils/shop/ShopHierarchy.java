package net.unfamily.iskautils.shop;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Helpers for shop category nesting. {@code null}/blank {@code in_category} means root.
 */
public final class ShopHierarchy {

    private ShopHierarchy() {}

    /** True when parent id is null or blank (root level). */
    public static boolean isRoot(@Nullable String parentId) {
        return parentId == null || parentId.isBlank();
    }

    /** Returns null for root (null/blank), otherwise the trimmed id. */
    @Nullable
    public static String normalizeParent(@Nullable String parentId) {
        if (parentId == null) {
            return null;
        }
        String trimmed = parentId.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** True when both ids refer to the same parent (including both root). */
    public static boolean sameParent(@Nullable String a, @Nullable String b) {
        return Objects.equals(normalizeParent(a), normalizeParent(b));
    }

    /**
     * Reads JSON {@code in_category}; missing/null/blank → root ({@code null}).
     */
    @Nullable
    public static String readInCategory(JsonObject o) {
        if (o == null || !o.has("in_category") || o.get("in_category").isJsonNull()) {
            return null;
        }
        if (!o.get("in_category").isJsonPrimitive()) {
            return null;
        }
        return normalizeParent(o.get("in_category").getAsString());
    }

    /**
     * Writes {@code in_category} only when non-null (root = omit key).
     */
    public static void writeInCategory(JsonObject o, @Nullable String inCategory) {
        String normalized = normalizeParent(inCategory);
        if (normalized != null) {
            o.addProperty("in_category", normalized);
        }
    }

    /**
     * If {@code parentId} points at a missing category, returns {@code null} (root).
     * Caller should log when the result differs from a non-root input.
     */
    @Nullable
    public static String resolveParentOrRoot(
            @Nullable String parentId,
            Map<String, ShopCategory> categories) {
        String normalized = normalizeParent(parentId);
        if (normalized == null) {
            return null;
        }
        if (categories == null || !categories.containsKey(normalized)) {
            return null;
        }
        return normalized;
    }

    /**
     * True if {@code categoryId} is nested under {@code ancestorId} (direct or indirect).
     * Self is not a descendant. Cycles return false.
     */
    public static boolean isDescendantOf(
            Map<String, ShopCategory> categories,
            @Nullable String categoryId,
            @Nullable String ancestorId) {
        String start = normalizeParent(categoryId);
        String ancestor = normalizeParent(ancestorId);
        if (start == null || ancestor == null || start.equals(ancestor) || categories == null) {
            return false;
        }
        Set<String> seen = new HashSet<>();
        String current = start;
        while (!isRoot(current)) {
            if (!seen.add(current)) {
                return false;
            }
            ShopCategory cat = categories.get(current);
            if (cat == null) {
                return false;
            }
            String parent = normalizeParent(cat.inCategory);
            if (ancestor.equals(parent)) {
                return true;
            }
            current = parent;
        }
        return false;
    }

    /** Categories whose {@code inCategory} matches {@code parentId} (null = root children). */
    public static List<ShopCategory> childCategories(
            Collection<ShopCategory> categories,
            @Nullable String parentId) {
        List<ShopCategory> out = new ArrayList<>();
        if (categories == null) {
            return out;
        }
        for (ShopCategory c : categories) {
            if (c != null && sameParent(c.inCategory, parentId)) {
                out.add(c);
            }
        }
        return out;
    }

    /** Entries whose {@code inCategory} matches {@code categoryId} (null = root entries). */
    public static List<ShopEntry> childEntries(
            Collection<ShopEntry> entries,
            @Nullable String categoryId) {
        List<ShopEntry> out = new ArrayList<>();
        if (entries == null) {
            return out;
        }
        for (ShopEntry e : entries) {
            if (e != null && sameParent(e.inCategory, categoryId)) {
                out.add(e);
            }
        }
        return out;
    }
}

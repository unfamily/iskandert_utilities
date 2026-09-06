package net.unfamily.iskautils.shop;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.unfamily.iskautils.IskaUtils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Shop GUI icons under {@code assets/iska_utils/textures/gui/icons/}.
 * <p>
 * KubeJS (or other packs) may add more PNGs under the same {@code iska_utils} path;
 * {@link #listAvailable} returns every PNG found there. JSON stores the file stem
 * (e.g. {@code command_icon}); omitted means type default.
 */
public final class ShopGuiIcons {
    public static final String FOLDER = "textures/gui/icons";

    public static final String COMMAND_DEFAULT = "command_icon";
    public static final String STAGE_DEFAULT = "stage_icon";
    public static final String RF_DEFAULT = "rf_icon";
    public static final String CURRENCY_DEFAULT = "currency_icon";

    private ShopGuiIcons() {}

    public static ResourceLocation texture(String stem) {
        String clean = sanitizeStem(stem);
        return ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, FOLDER + "/" + clean + ".png");
    }

    public static ResourceLocation resolve(@Nullable String icon, @Nullable ResourceLocation fallback) {
        if (icon != null && !icon.isBlank()) {
            String raw = icon.trim();
            if (raw.contains(":")) {
                ResourceLocation parsed = ResourceLocation.tryParse(raw);
                if (parsed != null) {
                    return parsed;
                }
            } else {
                return texture(raw);
            }
        }
        return fallback;
    }

    public static String sanitizeStem(@Nullable String stem) {
        if (stem == null || stem.isBlank()) {
            return COMMAND_DEFAULT;
        }
        String s = stem.trim();
        int slash = Math.max(s.lastIndexOf('/'), s.lastIndexOf('\\'));
        if (slash >= 0) {
            s = s.substring(slash + 1);
        }
        if (s.endsWith(".png")) {
            s = s.substring(0, s.length() - 4);
        }
        return s.isEmpty() ? COMMAND_DEFAULT : s;
    }

    public static String stemOf(ResourceLocation texture) {
        String path = texture.getPath();
        int slash = path.lastIndexOf('/');
        String file = slash >= 0 ? path.substring(slash + 1) : path;
        if (file.endsWith(".png")) {
            file = file.substring(0, file.length() - 4);
        }
        return file;
    }

    /**
     * All PNGs under {@code iska_utils:textures/gui/icons/}, including extras from
     * resource packs / KubeJS that target the same namespace path.
     */
    public static List<ResourceLocation> listAvailable(ResourceManager manager) {
        List<ResourceLocation> out = new ArrayList<>();
        manager.listResources(FOLDER, rl ->
                IskaUtils.MOD_ID.equals(rl.getNamespace())
                        && rl.getPath().startsWith(FOLDER + "/")
                        && rl.getPath().endsWith(".png")
                        && rl.getPath().indexOf('/', FOLDER.length() + 1) < 0
        ).keySet().forEach(out::add);
        out.sort(Comparator.comparing(ShopGuiIcons::stemOf));
        return out;
    }

    public static ResourceLocation cycleNext(@Nullable String currentStem,
                                             @Nullable ResourceLocation typeDefault,
                                             ResourceManager manager) {
        List<ResourceLocation> available = listAvailable(manager);
        if (available.isEmpty()) {
            return typeDefault != null ? typeDefault : texture(COMMAND_DEFAULT);
        }
        ResourceLocation current = resolve(currentStem, typeDefault);
        int idx = 0;
        for (int i = 0; i < available.size(); i++) {
            if (available.get(i).equals(current) || stemOf(available.get(i)).equalsIgnoreCase(stemOf(current))) {
                idx = i;
                break;
            }
        }
        return available.get((idx + 1) % available.size());
    }

    /** Whether the stored icon is the type default (omit from JSON). */
    public static boolean isDefaultIcon(@Nullable String icon, ResourceLocation typeDefault) {
        if (icon == null || icon.isBlank()) {
            return true;
        }
        ResourceLocation resolved = resolve(icon, null);
        return typeDefault != null && typeDefault.equals(resolved);
    }

    /** @deprecated use {@link #isDefaultIcon} */
    public static boolean isDefaultStem(@Nullable String stem, String typeDefaultStem) {
        return isDefaultIcon(stem, texture(typeDefaultStem));
    }

    /** Compact JSON value: file stem for icons in this folder. */
    public static String toStoredValue(ResourceLocation texture) {
        return stemOf(texture);
    }
}

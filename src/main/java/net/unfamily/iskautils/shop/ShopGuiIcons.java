package net.unfamily.iskautils.shop;

import net.minecraft.resources.Identifier;
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

    private ShopGuiIcons() {}

    public static Identifier texture(String stem) {
        String clean = sanitizeStem(stem);
        return Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, FOLDER + "/" + clean + ".png");
    }

    public static Identifier resolve(@Nullable String icon, @Nullable Identifier fallback) {
        if (icon != null && !icon.isBlank()) {
            String raw = icon.trim();
            if (raw.contains(":")) {
                Identifier parsed = Identifier.tryParse(raw);
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

    public static String stemOf(Identifier texture) {
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
    public static List<Identifier> listAvailable(ResourceManager manager) {
        List<Identifier> out = new ArrayList<>();
        manager.listResources(FOLDER, id ->
                IskaUtils.MOD_ID.equals(id.getNamespace())
                        && id.getPath().startsWith(FOLDER + "/")
                        && id.getPath().endsWith(".png")
                        && id.getPath().indexOf('/', FOLDER.length() + 1) < 0
        ).keySet().forEach(out::add);
        out.sort(Comparator.comparing(ShopGuiIcons::stemOf));
        return out;
    }

    public static Identifier cycleNext(@Nullable String currentStem,
                                       @Nullable Identifier typeDefault,
                                       ResourceManager manager) {
        List<Identifier> available = listAvailable(manager);
        if (available.isEmpty()) {
            return typeDefault != null ? typeDefault : texture(COMMAND_DEFAULT);
        }
        Identifier current = resolve(currentStem, typeDefault);
        int idx = 0;
        for (int i = 0; i < available.size(); i++) {
            if (available.get(i).equals(current) || stemOf(available.get(i)).equalsIgnoreCase(stemOf(current))) {
                idx = i;
                break;
            }
        }
        return available.get((idx + 1) % available.size());
    }

    public static boolean isDefaultIcon(@Nullable String icon, Identifier typeDefault) {
        if (icon == null || icon.isBlank()) {
            return true;
        }
        Identifier resolved = resolve(icon, null);
        return typeDefault != null && typeDefault.equals(resolved);
    }

    public static boolean isDefaultStem(@Nullable String stem, String typeDefaultStem) {
        return isDefaultIcon(stem, texture(typeDefaultStem));
    }

    public static String toStoredValue(Identifier texture) {
        return stemOf(texture);
    }
}

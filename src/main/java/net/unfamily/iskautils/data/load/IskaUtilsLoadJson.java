package net.unfamily.iskautils.data.load;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.fml.ModList;
import net.unfamily.iskautils.IskaUtils;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Collects JSON from the {@code load} resource folder using {@link ResourceManager#listResourceStacks}
 * (last pack in stack wins per {@link ResourceLocation}), and reads built-in files from the mod jar
 * for bootstrap before a server exists.
 */
public final class IskaUtilsLoadJson {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();

    private IskaUtilsLoadJson() {}

    /**
     * Merged JSON per resource location (stack tail), filtered by path under {@code load/}.
     */
    public static Map<ResourceLocation, JsonElement> collectMergedJson(
            ResourceManager resourceManager,
            Predicate<ResourceLocation> locationFilter) {
        Map<ResourceLocation, JsonElement> out = new LinkedHashMap<>();
        Map<ResourceLocation, List<Resource>> stacks =
                resourceManager.listResourceStacks(IskaUtilsLoadPaths.LOAD_FOLDER,
                        id -> id.getPath().endsWith(".json") && locationFilter.test(id));
        for (Map.Entry<ResourceLocation, List<Resource>> entry : stacks.entrySet()) {
            List<Resource> stack = entry.getValue();
            if (stack.isEmpty()) {
                continue;
            }
            Resource top = stack.getLast();
            try (var reader = new BufferedReader(new InputStreamReader(top.open(), StandardCharsets.UTF_8))) {
                JsonElement parsed = GSON.fromJson(reader, JsonElement.class);
                if (parsed != null) {
                    out.put(entry.getKey(), parsed);
                }
            } catch (IOException | JsonParseException ex) {
                LOGGER.error("Failed to read load JSON {}: {}", entry.getKey(), ex.getMessage());
            }
        }
        return out;
    }

    /**
     * Order: built-in {@code iska_utils} namespace first, then other namespaces (string order),
     * so datapacks typically override mod defaults for the same logical ids inside JSON.
     */
    public static List<Map.Entry<ResourceLocation, JsonElement>> orderedEntries(Map<ResourceLocation, JsonElement> merged) {
        List<Map.Entry<ResourceLocation, JsonElement>> list = new ArrayList<>(merged.entrySet());
        list.sort(
                Comparator.comparing((Map.Entry<ResourceLocation, JsonElement> e) -> !IskaUtils.MOD_ID.equals(e.getKey().getNamespace()))
                        .thenComparing(e -> e.getKey().toString()));
        return list;
    }

    /**
     * Bootstrap from the mod jar / resources folder when no {@link ResourceManager} is available.
     */
    public static Map<ResourceLocation, JsonElement> collectFromModJarOnly(String subdirUnderLoad) {
        Map<ResourceLocation, JsonElement> out = new LinkedHashMap<>();
        String dirInRoot = "data/" + IskaUtils.MOD_ID + "/" + IskaUtilsLoadPaths.LOAD_FOLDER + "/" + subdirUnderLoad;
        LOGGER.info("Bootstrap loading from: {}", dirInRoot);
        ModList.get().getModContainerById(IskaUtils.MOD_ID).ifPresentOrElse(
                container -> {
                    var modFileInfo = container.getModInfo().getOwningFile();
                    if (modFileInfo == null) {
                        LOGGER.warn("No mod jar file for {}, cannot bootstrap load/{}", IskaUtils.MOD_ID, subdirUnderLoad);
                        return;
                    }
                    Path root = modFileInfo.getFile().getFilePath();
                    try {
                        if (Files.isDirectory(root)) {
                            Path buildDir = root.getParent().getParent().getParent(); // build/
                            Path resourcesFolder = buildDir.resolve("resources").resolve("main");

                            if (Files.exists(resourcesFolder)) {
                                Path base = resourcesFolder.resolve(dirInRoot);
                                if (Files.exists(base)) {
                                    try (Stream<Path> walk = Files.walk(base)) {
                                        walk.filter(Files::isRegularFile)
                                                .filter(p -> p.toString().endsWith(".json"))
                                                .sorted()
                                                .forEach(file -> readOneJsonFile(out, base, file));
                                    }
                                } else {
                                    LOGGER.warn("DEV resources directory does not exist: {}", base);
                                }
                            } else {
                                // Fallback to standard directory mode (in case resources folder doesn't exist where we expect)
                                Path base = root.resolve(dirInRoot);
                                if (Files.exists(base)) {
                                    try (Stream<Path> walk = Files.walk(base)) {
                                        walk.filter(Files::isRegularFile)
                                                .filter(p -> p.toString().endsWith(".json"))
                                                .sorted()
                                                .forEach(file -> readOneJsonFile(out, base, file));
                                    }
                                } else {
                                    LOGGER.warn("Directory does not exist: {}", base);
                                }
                            }
                        } else {
                            try (var fs = FileSystems.newFileSystem(root, Map.of())) {
                                Path base = fs.getPath(dirInRoot);
                                if (Files.exists(base)) {
                                    try (Stream<Path> walk = Files.walk(base)) {
                                        walk.filter(Files::isRegularFile)
                                                .filter(p -> p.toString().endsWith(".json"))
                                                .sorted()
                                                .forEach(file -> readOneJsonFile(out, base, file));
                                    }
                                } else {
                                    LOGGER.warn("Directory does not exist in JAR: {}", base);
                                }
                            }
                        }
                    } catch (IOException ex) {
                        LOGGER.error("Failed walking mod load path {}: {}", dirInRoot, ex.getMessage());
                    }
                },
                () -> {
                    LOGGER.error("Mod container not found for {} during bootstrap! ModList: {}",
                            IskaUtils.MOD_ID,
                            ModList.get().getMods().stream().map(m -> m.getModId()).toList());
                    LOGGER.warn("Mod file not found for {}, cannot bootstrap load/{}", IskaUtils.MOD_ID, subdirUnderLoad);
                });
        return out;
    }

    private static void readOneJsonFile(Map<ResourceLocation, JsonElement> out, Path base, Path file) {
        try {
            String subdir = base.getFileName().toString();
            String rel = base.relativize(file).toString().replace('\\', '/');
            String pathPart = IskaUtilsLoadPaths.LOAD_FOLDER + "/" + subdir + "/" + rel;
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, pathPart);
            try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                JsonElement parsed = GSON.fromJson(reader, JsonElement.class);
                if (parsed != null) {
                    out.put(id, parsed);
                }
            }
        } catch (Exception ex) {
            LOGGER.error("Failed reading {}: {}", file, ex.getMessage());
        }
    }

    public static String definitionIdFromLocation(ResourceLocation loc) {
        String p = loc.getPath();
        int slash = p.lastIndexOf('/');
        String file = slash >= 0 ? p.substring(slash + 1) : p;
        if (file.endsWith(".json")) {
            file = file.substring(0, file.length() - 5);
        }
        return file;
    }
}


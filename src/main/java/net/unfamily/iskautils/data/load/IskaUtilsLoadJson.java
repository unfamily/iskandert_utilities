package net.unfamily.iskautils.data.load;

import net.unfamily.iskautils.util.ModLogger;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.fml.ModList;
import net.unfamily.iskautils.IskaUtils;

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
    private static final ModLogger LOGGER = ModLogger.of(IskaUtilsLoadJson.class);
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

    public static Map<ResourceLocation, JsonElement> collectMergedJsonUnderDirectory(
            ResourceManager resourceManager, String directoryUnderDataNamespace, Predicate<ResourceLocation> locationFilter) {
        Map<ResourceLocation, JsonElement> out = new LinkedHashMap<>();
        Map<ResourceLocation, List<Resource>> stacks =
                resourceManager.listResourceStacks(directoryUnderDataNamespace, id -> id.getPath().endsWith(".json") && locationFilter.test(id));
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
                LOGGER.error("Failed to read JSON {}: {}", entry.getKey(), ex.getMessage());
            }
        }
        return out;
    }

    /**
     * Merged JSON from {@code load/<subdir>/} and flat {@code load/*.json} whose {@code type} matches the subdir.
     */
    public static Map<ResourceLocation, JsonElement> collectMergedJsonForSubdir(
            ResourceManager resourceManager,
            String subdirUnderLoad) {
        Map<ResourceLocation, JsonElement> out = new LinkedHashMap<>();
        out.putAll(collectMergedJson(resourceManager,
                id -> IskaUtilsLoadPaths.isJsonUnderLoadSubdir(id, subdirUnderLoad)));
        for (var e : collectMergedJson(resourceManager, IskaUtilsLoadPaths::isJsonDirectlyUnderLoad).entrySet()) {
            if (IskaUtilsLoadPaths.jsonMatchesSubdir(e.getValue(), subdirUnderLoad)) {
                out.put(e.getKey(), e.getValue());
            }
        }
        return out;
    }

    /**
     * Merged JSON from every {@code load/} subtree whose root {@code type} field matches {@code jsonType}.
     */
    public static Map<ResourceLocation, JsonElement> collectMergedJsonForType(
            ResourceManager resourceManager,
            String jsonType) {
        Map<ResourceLocation, JsonElement> out = new LinkedHashMap<>();
        for (var e : collectMergedJson(resourceManager, IskaUtilsLoadPaths::isJsonUnderLoadTree).entrySet()) {
            if (IskaUtilsLoadPaths.jsonMatchesType(e.getValue(), jsonType)) {
                out.put(e.getKey(), e.getValue());
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
        int externalFiles = IskaUtilsFilesystemBootstrap.mergeInto(out, subdirUnderLoad);
        if (externalFiles > 0) {
            LOGGER.info("Bootstrap load/{}: merged {} file(s) from kubejs/datapacks on disk", subdirUnderLoad, externalFiles);
        }
        return out;
    }

    /**
     * Bootstrap from mod jar / resources for every {@code load/} JSON whose {@code type} matches {@code jsonType}.
     */
    public static Map<ResourceLocation, JsonElement> collectFromModJarOnlyForType(String jsonType) {
        Map<ResourceLocation, JsonElement> out = new LinkedHashMap<>();
        String loadRoot = "data/" + IskaUtils.MOD_ID + "/" + IskaUtilsLoadPaths.LOAD_FOLDER;
        LOGGER.info("Bootstrap loading type {} from: {}", jsonType, loadRoot);
        ModList.get().getModContainerById(IskaUtils.MOD_ID).ifPresentOrElse(
                container -> {
                    var modFileInfo = container.getModInfo().getOwningFile();
                    if (modFileInfo == null) {
                        LOGGER.warn("No mod jar file for {}, cannot bootstrap load/ for type {}", IskaUtils.MOD_ID, jsonType);
                        return;
                    }
                    Path root = modFileInfo.getFile().getFilePath();
                    try {
                        if (Files.isDirectory(root)) {
                            Path buildDir = root.getParent().getParent().getParent();
                            Path resourcesFolder = buildDir.resolve("resources").resolve("main");
                            Path base = Files.exists(resourcesFolder) ? resourcesFolder.resolve(loadRoot) : root.resolve(loadRoot);
                            if (Files.exists(base)) {
                                try (Stream<Path> walk = Files.walk(base)) {
                                    walk.filter(Files::isRegularFile)
                                            .filter(p -> p.toString().endsWith(".json"))
                                            .sorted()
                                            .forEach(file -> readOneLoadTreeJsonFile(out, base, file, jsonType));
                                }
                            } else {
                                LOGGER.warn("Directory does not exist: {}", base);
                            }
                        } else {
                            try (var fs = FileSystems.newFileSystem(root, Map.of())) {
                                Path base = fs.getPath(loadRoot);
                                if (Files.exists(base)) {
                                    try (Stream<Path> walk = Files.walk(base)) {
                                        walk.filter(Files::isRegularFile)
                                                .filter(p -> p.toString().endsWith(".json"))
                                                .sorted()
                                                .forEach(file -> readOneLoadTreeJsonFile(out, base, file, jsonType));
                                    }
                                } else {
                                    LOGGER.warn("Directory does not exist in JAR: {}", base);
                                }
                            }
                        }
                    } catch (IOException ex) {
                        LOGGER.error("Failed walking mod load path {}: {}", loadRoot, ex.getMessage());
                    }
                },
                () -> LOGGER.warn("Mod container not found for {} during bootstrap type {}", IskaUtils.MOD_ID, jsonType));
        int externalFiles = IskaUtilsFilesystemBootstrap.mergeIntoForType(out, jsonType);
        if (externalFiles > 0) {
            LOGGER.info("Bootstrap load type {}: merged {} file(s) from kubejs/datapacks on disk", jsonType, externalFiles);
        }
        return out;
    }

    /**
     * Bootstrap from mod jar / resources for arbitrary {@code data/<modid>/<directory>/...} trees.
     * Intended for client-only contexts (e.g. JEI) where {@link ResourceManager} for SERVER_DATA is not available.
     */
    public static Map<ResourceLocation, JsonElement> collectFromModJarOnlyUnderDataDir(
            String directoryUnderDataNamespace,
            Predicate<ResourceLocation> locationFilter) {
        Map<ResourceLocation, JsonElement> out = new LinkedHashMap<>();
        String dirInRoot = "data/" + IskaUtils.MOD_ID + "/" + directoryUnderDataNamespace;
        LOGGER.info("Bootstrap loading from: {}", dirInRoot);
        ModList.get().getModContainerById(IskaUtils.MOD_ID).ifPresentOrElse(
                container -> {
                    var modFileInfo = container.getModInfo().getOwningFile();
                    if (modFileInfo == null) {
                        LOGGER.warn("No mod jar file for {}, cannot bootstrap data/{}", IskaUtils.MOD_ID, directoryUnderDataNamespace);
                        return;
                    }
                    Path root = modFileInfo.getFile().getFilePath();
                    try {
                        if (Files.isDirectory(root)) {
                            Path buildDir = root.getParent().getParent().getParent(); // build/
                            Path resourcesFolder = buildDir.resolve("resources").resolve("main");
                            Path base = Files.exists(resourcesFolder) ? resourcesFolder.resolve(dirInRoot) : root.resolve(dirInRoot);
                            if (Files.exists(base)) {
                                try (Stream<Path> walk = Files.walk(base)) {
                                    walk.filter(Files::isRegularFile)
                                            .filter(p -> p.toString().endsWith(".json"))
                                            .sorted()
                                            .forEach(file -> readOneDataJsonFile(out, base, file, locationFilter));
                                }
                            } else {
                                LOGGER.warn("Directory does not exist: {}", base);
                            }
                        } else {
                            try (var fs = FileSystems.newFileSystem(root, Map.of())) {
                                Path base = fs.getPath(dirInRoot);
                                if (Files.exists(base)) {
                                    try (Stream<Path> walk = Files.walk(base)) {
                                        walk.filter(Files::isRegularFile)
                                                .filter(p -> p.toString().endsWith(".json"))
                                                .sorted()
                                                .forEach(file -> readOneDataJsonFile(out, base, file, locationFilter));
                                    }
                                } else {
                                    LOGGER.warn("Directory does not exist in JAR: {}", base);
                                }
                            }
                        }
                    } catch (IOException ex) {
                        LOGGER.error("Failed walking mod data path {}: {}", dirInRoot, ex.getMessage());
                    }
                },
                () -> LOGGER.warn("Mod container not found for {} during bootstrap data/{}", IskaUtils.MOD_ID, directoryUnderDataNamespace));
        return out;
    }

    private static void readOneDataJsonFile(
            Map<ResourceLocation, JsonElement> out,
            Path base,
            Path file,
            Predicate<ResourceLocation> locationFilter) {
        try {
            String rel = base.relativize(file).toString().replace('\\', '/');
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, base.getFileName() + "/" + rel);
            // base.getFileName() is the last path segment of directoryUnderDataNamespace, e.g. "recipe"
            // so id becomes "iska_utils:recipe/<...>.json" which matches ResourceManager ids.
            if (!locationFilter.test(id)) {
                return;
            }
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

    private static void readOneLoadTreeJsonFile(
            Map<ResourceLocation, JsonElement> out,
            Path loadBase,
            Path file,
            String jsonTypeFilter) {
        try {
            String rel = loadBase.relativize(file).toString().replace('\\', '/');
            String pathPart = IskaUtilsLoadPaths.LOAD_FOLDER + "/" + rel;
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, pathPart);
            try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                JsonElement parsed = GSON.fromJson(reader, JsonElement.class);
                if (parsed == null) {
                    return;
                }
                if (jsonTypeFilter != null && !IskaUtilsLoadPaths.jsonMatchesType(parsed, jsonTypeFilter)) {
                    return;
                }
                out.put(id, parsed);
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


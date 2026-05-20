package net.unfamily.iskautils.data.load;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Reads {@code load/<subdir>/} and flat {@code load/*.json} (by JSON {@code type}) from disk before a
 * {@link net.minecraft.server.packs.resources.ResourceManager} exists (KubeJS {@code kubejs/data},
 * world {@code datapacks/<pack>/data}).
 */
public final class IskaUtilsFilesystemBootstrap {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();

    private IskaUtilsFilesystemBootstrap() {}

    /**
     * Merges matching files into {@code target} (later files override earlier). Returns file count merged.
     */
    public static int mergeInto(Map<Identifier, JsonElement> target, String subdirUnderLoad) {
        Path gameDir = FMLPaths.GAMEDIR.get();
        int count = 0;
        count += scanDataRoot(gameDir.resolve("kubejs/data"), subdirUnderLoad, target);
        count += scanDatapackRoots(gameDir.resolve("datapacks"), subdirUnderLoad, target);
        return count;
    }

    private static int scanDatapackRoots(Path datapacksDir, String subdirUnderLoad, Map<Identifier, JsonElement> target) {
        if (!Files.isDirectory(datapacksDir)) {
            return 0;
        }
        int count = 0;
        try (Stream<Path> packs = Files.list(datapacksDir)) {
            for (Path pack : packs.toList()) {
                if (Files.isDirectory(pack)) {
                    count += scanDataRoot(pack.resolve("data"), subdirUnderLoad, target);
                }
            }
        } catch (IOException ex) {
            LOGGER.debug("Could not list datapacks: {}", ex.getMessage());
        }
        return count;
    }

    private static int scanDataRoot(Path dataRoot, String subdirUnderLoad, Map<Identifier, JsonElement> target) {
        if (!Files.isDirectory(dataRoot)) {
            return 0;
        }
        String subdirSegment = "/" + IskaUtilsLoadPaths.LOAD_FOLDER + "/" + subdirUnderLoad + "/";
        int count = 0;
        try (Stream<Path> walk = Files.walk(dataRoot)) {
            for (Path file : walk.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".json")).toList()) {
                String normalized = file.toString().replace('\\', '/');
                Path rel = dataRoot.relativize(file);
                if (rel.getNameCount() < 3) {
                    continue;
                }
                boolean inSubdir = normalized.contains(subdirSegment);
                boolean flatUnderLoad = isFlatUnderLoad(rel);
                if (!inSubdir && !flatUnderLoad) {
                    continue;
                }
                String namespace = rel.getName(0).toString();
                String pathPart = rel.toString().replace('\\', '/');
                Identifier id = Identifier.fromNamespaceAndPath(namespace, pathPart);
                try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    JsonElement parsed = GSON.fromJson(reader, JsonElement.class);
                    if (parsed == null) {
                        continue;
                    }
                    if (flatUnderLoad && !IskaUtilsLoadPaths.jsonMatchesSubdir(parsed, subdirUnderLoad)) {
                        continue;
                    }
                    target.put(id, parsed);
                    count++;
                } catch (IOException | JsonParseException ex) {
                    LOGGER.warn("Failed to read external load JSON {}: {}", file, ex.getMessage());
                }
            }
        } catch (IOException ex) {
            LOGGER.debug("Could not walk {}: {}", dataRoot, ex.getMessage());
        }
        return count;
    }

    /** {@code <namespace>/load/<file>.json} with no extra folder under {@code load}. */
    private static boolean isFlatUnderLoad(Path relFromDataRoot) {
        return relFromDataRoot.getNameCount() == 3
                && IskaUtilsLoadPaths.LOAD_FOLDER.equals(relFromDataRoot.getName(1).toString());
    }
}

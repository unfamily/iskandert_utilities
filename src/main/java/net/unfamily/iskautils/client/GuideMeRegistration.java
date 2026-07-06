package net.unfamily.iskautils.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.unfamily.iskautils.util.ModLogger;

/**
 * Loads GuideME integration via reflection so {@link net.unfamily.iskautils.IskaUtils} never references guideme on dedicated servers.
 * Must run from the mod constructor on the physical client, before the first resource reload.
 */
public final class GuideMeRegistration {
    private static final ModLogger LOGGER = ModLogger.of(GuideMeRegistration.class);
    private static final String IMPL_CLASS = "net.unfamily.iskautils.guide.IskaUtilsGuide";

    private GuideMeRegistration() {}

    public static void register() {
        if (FMLEnvironment.dist != Dist.CLIENT || !ModList.get().isLoaded("guideme")) {
            return;
        }
        try {
            Class.forName(IMPL_CLASS).getMethod("registerClient").invoke(null);
        } catch (ReflectiveOperationException e) {
            LOGGER.error("Failed to register GuideME guide", e);
        }
    }
}

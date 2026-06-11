package net.unfamily.iskautils.guide;

import guideme.Guide;
import guideme.GuideItemSettings;
import guideme.compiler.TagCompiler;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.ModList;
import net.unfamily.iskautils.IskaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registers the Iska Utils GuideME guidebook on the client.
 */
public final class IskaUtilsGuide {
    private static final Logger LOGGER = LoggerFactory.getLogger(IskaUtilsGuide.class);

    public static final Identifier GUIDE_ID = Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "guide");

    private IskaUtilsGuide() {
    }

    public static void registerClient() {
        if (!ModList.get().isLoaded("guideme")) {
            return;
        }
        try {
            var guideItemSettings = new GuideItemSettings(
                    Optional.of(Component.translatable("item.iska_utils.guide")),
                    List.of(Component.translatable("tooltip.iska_utils.guide.line0")
                            .withStyle(ChatFormatting.DARK_GRAY)),
                    Optional.empty());
            Guide.builder(GUIDE_ID)
                    .itemSettings(guideItemSettings)
                    .extension(TagCompiler.EXTENSION_POINT, new TheRootsTitleTagCompiler())
                    .index(new TheRootsNavigationIndex())
                    .build();
            LOGGER.info("GuideME guide registered");
        } catch (Exception e) {
            LOGGER.warn("Failed to register GuideME guide: {}", e.getMessage());
        }
    }
}
